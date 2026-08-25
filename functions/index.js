/**
 * functions/index.js
 * ─────────────────────────────────────────────────────────────────
 * Cloud Function triggers for the Firestore-backed app.
 *
 * `moderateComment` fires whenever a new comment lands in
 * `programs/{programId}/comments/{commentId}`. It runs the same
 * `lib/profanity` algorithm (ported to Node in `./profanity.js`)
 * and sets a `moderation` map on the comment doc:
 *
 *   { ok: true }                            → comment stays public
 *   { ok: false, reason: "profane" }        → comment is flagged
 *   { ok: false, reason: "rate_limit" }     → comment is flagged
 *
 * It also applies a server-side per-device rate limit (comments per
 * program within a rolling window). Client-side checks are trivially
 * bypassable; this trigger is the authoritative gate.
 *
 * The mobile app reads `moderation` and either renders the comment
 * normally (ok: true) or hides it behind a "تم إخفاء هذا التعليق"
 * notice (ok: false). Hidden comments still count toward reaction
 * counts and stay in the collection for admin review.
 *
 * Trigger: `onDocumentCreated` (v2 API). Firestore event triggers
 * MUST run in the same region as the Firestore database. The project
 * documents its database location as `eur3` (Europe multi-region),
 * so triggers deploy to `europe-west1`. If the database is actually
 * in another region (e.g. `us-central1`), deployment fails loudly —
 * change REGION to match the Console value before deploying.
 *
 * Deploy:
 *   firebase deploy --only functions            (from repo root)
 *   # or: cd admin && firebase deploy --only functions
 * ─────────────────────────────────────────────────────────────────
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp }      = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { Expo }               = require("expo-server-sdk");
const { moderate }           = require("./profanity");
const {
  chunkItems,
  isStaleExpoTicket,
  isStaleFcmResponse,
  filterPushTokensByCategory,
  partitionPushTokens,
} = require("./lib/push-helpers");
const {
  COMMENT_RATE_WINDOW_MS,
  COMMENT_RATE_LIMIT,
  RATE_LIMIT_REASON,
  isRateLimited,
} = require("./lib/comment-rate-limit");

initializeApp();

/**
 * Single region for every function in this project. Firestore event
 * triggers must match the database location (documented as eur3 →
 * Europe multi-region). See the header comment for the deploy-time
 * failure mode if the Console value differs.
 */
const REGION = "europe-west1";

exports.moderateComment = onDocumentCreated(
  {
    document: "programs/{programId}/comments/{commentId}",
    region:   REGION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data       = snap.data();
    const programId  = event.params.programId;
    const commentId  = event.params.commentId;
    const text       = (data?.text ?? "").toString();
    // userUid is validated by Firestore rules against request.auth.uid.
    // Never use the client-controlled installation/device identifier as the
    // authoritative server-side rate-limit key.
    const userUid    = (data?.userUid ?? "").toString();

    // Profanity / spam / shape checks are authoritative.
    const result = moderate(text);

    // Server-side rate limit: at most COMMENT_RATE_LIMIT comments per
    // authenticated user per program inside the rolling window. Best-effort: if
    // the query fails (e.g. the composite index has not been deployed
    // yet), moderation still runs — the rate limit just stays dormant
    // until the index exists (firestore.indexes.json / firestore deploy).
    let rateLimited = false;
    if (userUid) {
      try {
        const cutoff = Timestamp.fromMillis(Date.now() - COMMENT_RATE_WINDOW_MS);
        const recent = await getFirestore()
          .collection("programs")
          .doc(programId)
          .collection("comments")
          .where("userUid", "==", userUid)
          .where("createdAt", ">", cutoff)
          .orderBy("createdAt", "desc")
          .limit(COMMENT_RATE_LIMIT + 1)
          .get();
        rateLimited = isRateLimited(recent.size);
      } catch (err) {
        console.warn(
          `moderateComment: rate-limit query failed for ${programId}/${commentId}:`,
          err?.message ?? err,
        );
      }
    }

    const finalResult = rateLimited
      ? { ok: false, reason: RATE_LIMIT_REASON }
      : result;

    const db  = getFirestore();
    const ref = db.doc(`programs/${programId}/comments/${commentId}`);

    try {
      await ref.set(
        {
          moderation: {
            ok:          finalResult.ok,
            reason:      finalResult.reason ?? null,
            checkedAt:   new Date().toISOString(),
            checkedBy:   "moderation-function-v1",
          },
        },
        { merge: true },
      );
    } catch (err) {
      // If the write fails (network blip, rule denial), don't fail the
      // whole function -- the comment stays in the doc unchanged and
      // the UI just falls back to client-side moderation.
      console.warn(
        `moderateComment: failed to write moderation flag for ${programId}/${commentId}:`,
        err?.message ?? err,
      );
    }
  },
);

const expo = new Expo();

// ─── Send Push Notification ───────────────────────────────────────
// Triggers when the admin panel writes a new doc to the `notifications`
// collection. Targets token records by the requested category and delivers to
// devices. Legacy records without categories remain eligible. Two transports are supported:
//   1. FCM (Firebase Cloud Messaging) — used by the native Android app
//   2. Expo Push API — legacy support for older Expo-based builds
// Tokens that are no longer registered are removed automatically.
// ───────────────────────────────────────────────────────────────────
exports.sendPushNotification = onDocumentCreated(
  {
    document: "notifications/{notificationId}",
    region:   REGION,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data    = snap.data();
    const notifId = event.params.notificationId;

    // ── Safety guard: prevent re-processing ────────────────────
    if (data.sent) {
      console.log(`sendPushNotification[${notifId}]: already sent, skipping`);
      return;
    }

    const { title, body, category } = data;
    if (!title || !body) {
      console.warn(`sendPushNotification[${notifId}]: missing title or body`);
      return;
    }

    const db = getFirestore();

    // ── Fetch tokens ───────────────────────────────────────────
    let tokensSnapshot;
    try {
      tokensSnapshot = await db.collection("push_tokens").get();
    } catch (err) {
      console.error(`sendPushNotification[${notifId}]: failed to read push_tokens:`, err?.message ?? err);
      return;
    }

    if (tokensSnapshot.empty) {
      console.log(`sendPushNotification[${notifId}]: no push tokens found`);
      await markNotificationSent(db, notifId, 0);
      return;
    }

    // ── Filter by category, then split by transport type ────────
    // Legacy token records without `categories` remain eligible so an app
    // upgrade does not silently drop existing recipients. New Android builds
    // write ["all"] until topic preferences are exposed in the UI.
    const tokenRecords = [];
    tokensSnapshot.forEach((doc) => {
      const tokenData = doc.data();
      tokenRecords.push({
        token: tokenData.token,
        categories: tokenData.categories,
        docId: doc.id,
      });
    });
    const targetedTokenRecords = filterPushTokensByCategory(tokenRecords, category);
    const { fcmTokens, expoTokens, validTokens } = partitionPushTokens(targetedTokenRecords);

    let deliveredCount = 0;
    const tokensToRemove = [];

    // ── Send via FCM (native Android) ──────────────────────────
    if (fcmTokens.length > 0) {
      const { getMessaging } = require("firebase-admin/messaging");
      const messaging = getMessaging();

      // FCM batch limit is 500 per call.
      for (const tokenChunk of chunkItems(fcmTokens)) {
        const batchTokens = tokenChunk.map((t) => t.token);
        try {
          const result = await messaging.sendEachForMulticast({
            tokens: batchTokens,
            notification: {
              title,
              body,
            },
            android: {
              priority: "high",
              notification: {
                sound: "default",
                channelId: category === "breaking" ? "breaking-news" : "general",
              },
            },
            data: {
              type: "push_notification",
              category: category ?? "",
            },
          });
          deliveredCount += result.successCount;

          // Collect invalid/unregistered tokens for cleanup.
          result.responses.forEach((r, idx) => {
            if (isStaleFcmResponse(r)) {
              tokensToRemove.push(batchTokens[idx]);
            }
          });
        } catch (err) {
          console.error(`sendPushNotification[${notifId}]: FCM sendEachForMulticast threw:`, err?.message ?? err);
        }
      }
    }

    // ── Send via Expo Push (legacy builds) ─────────────────────
    if (expoTokens.length > 0) {
      const messages = expoTokens.map((t) => ({
        to:    t.token,
        sound: "default",
        title,
        body,
        data:  { type: "push_notification", category: category ?? "" },
      }));

      const chunks = expo.chunkPushNotifications(messages);
      for (const chunk of chunks) {
        let tickets;
        try {
          tickets = await expo.sendPushNotificationsAsync(chunk);
        } catch (err) {
          console.error(`sendPushNotification[${notifId}]: Expo sendPushNotificationsAsync threw:`, err?.message ?? err);
          continue;
        }

        for (let i = 0; i < tickets.length; i++) {
          const ticket = tickets[i];
          if (ticket.status === "ok") {
            deliveredCount++;
          } else {
            const errType = ticket.details?.error;
            if (isStaleExpoTicket(ticket)) {
              tokensToRemove.push(chunk[i].to);
            }
            console.warn(
              `sendPushNotification[${notifId}]: Expo ticket error [${errType ?? "unknown"}]`,
              ticket.message ?? "",
            );
          }
        }
      }
    }

    // ── Remove stale tokens ────────────────────────────────────
    if (tokensToRemove.length > 0) {
      const removeSet = new Set(tokensToRemove);
      const batch     = db.batch();

      for (const vt of validTokens) {
        if (removeSet.has(vt.token)) {
          batch.delete(db.collection("push_tokens").doc(vt.docId));
        }
      }

      try {
        await batch.commit();
        console.log(
          `sendPushNotification[${notifId}]: removed ${removeSet.size} unregistered token(s)`,
        );
      } catch (err) {
        console.error(
          `sendPushNotification[${notifId}]: failed to remove stale tokens:`,
          err?.message ?? err,
        );
      }
    }

    // ── Mark delivered ─────────────────────────────────────────
    await markNotificationSent(db, notifId, deliveredCount);
  },
);

/**
 * Writes delivery metadata back to the notification document so the
 * admin panel can display how many devices received the push.
 *
 * Uses `merge: true` so it doesn't overwrite original fields
 * (title, body, category, etc.).
 */
async function markNotificationSent(db, notifId, deliveredCount) {
  try {
    await db
      .collection("notifications")
      .doc(notifId)
      .set(
        {
          sent:           true,
          deliveredCount: deliveredCount,
          deliveredAt:    new Date().toISOString(),
        },
        { merge: true },
      );
  } catch (err) {
    // Non-fatal: the notification was already delivered to devices,
    // we just couldn't persist the delivery metadata.
    console.error(
      `sendPushNotification[${notifId}]: failed to mark delivered:`,
      err?.message ?? err,
    );
  }
}
