/**
 * send-push.js — Vercel serverless function
 * ==========================================
 * Sends real FCM push notifications to every registered device without
 * needing Cloud Functions (works on the free Spark plan).
 *
 * The admin panel calls this endpoint after saving a notification to
 * Firestore; the function reads the push_tokens collection (which the app
 * keeps up to date) and delivers the message via FCM HTTP v1.
 *
 * Setup (one time, by the project owner):
 *   1. Create a service account in Firebase Console → Project settings →
 *      Service accounts → Generate new private key (JSON).
 *   2. Add it as a Vercel Environment Variable named
 *      FIREBASE_SERVICE_ACCOUNT (raw JSON string) for the admin project.
 *   3. Deploy this folder to Vercel (or use the existing admin deployment).
 *   4. Set VERCEL_FN_URL in the admin panel to the deployed URL, e.g.
 *      https://elwataniatv-channel.vercel.app/api/send-push
 *
 * No secrets are embedded in the repo: the service account lives only in
 * Vercel's environment.
 */

const admin = require("firebase-admin");

let app;
function getApp() {
  if (app) return app;
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) {
    throw new Error(
      "FIREBASE_SERVICE_ACCOUNT environment variable is not configured."
    );
  }
  let serviceAccount;
  try {
    serviceAccount = JSON.parse(raw);
  } catch (e) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT is not valid JSON.");
  }
  app = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  return app;
}

module.exports = async function handler(req, res) {
  // CORS for the admin panel origin(s).
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(204).end();
  if (req.method !== "POST") {
    return res.status(405).json({ ok: false, error: "Method not allowed" });
  }

  const { title, body, url, category = "general" } = req.body || {};

  if (!title || !body) {
    return res.status(400).json({ ok: false, error: "title and body are required" });
  }

  try {
    getApp();
    const db = admin.firestore();

    // Gather every registered FCM token (push_tokens/{userUid}).
    const snapshot = await db.collection("push_tokens").get();
    const tokens = [];
    snapshot.forEach((doc) => {
      const data = doc.data();
      const token = data && typeof data.token === "string" ? data.token.trim() : "";
      if (token.length >= 20 && token.length <= 4096) {
        tokens.push(token);
      }
    });

    if (tokens.length === 0) {
      return res.status(200).json({ ok: true, sent: 0, failed: 0, total: 0 });
    }

    // FCM HTTP v1 message.
    const message = {
      notification: { title, body },
      data: {
        category: String(category || "general"),
        ...(url ? { url } : {}),
      },
      android: {
        priority: "high",
        // Must match the app's notification channel (notifications/FcmMessageService.kt).
        notification: { channelId: "general", clickAction: "MAIN" },
      },
      // The app uses the default topic-less delivery; tokens are the target.
    };

    // Send in batches of 500 (FCM multi-cast limit).
    let sent = 0;
    let failed = 0;
    const batchSize = 500;
    for (let i = 0; i < tokens.length; i += batchSize) {
      const batch = tokens.slice(i, i + batchSize);
      try {
        const resp = await admin.messaging().sendEachForMulticast(
          batch.map((token) => ({ ...message, token }))
        );
        sent += resp.successCount || 0;
        failed += resp.failureCount || 0;
      } catch (e) {
        failed += batch.length;
      }
    }

    return res.status(200).json({ ok: true, sent, failed, total: tokens.length });
  } catch (err) {
    return res.status(500).json({ ok: false, error: String(err && err.message || err) });
  }
};
