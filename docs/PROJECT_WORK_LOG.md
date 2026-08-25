# Project Work Log

**Last updated:** August 10, 2026
**Repository:** `merci1994dz/Elwataniatv-Channel`
**Current verified main:** `56ecec3a28c3a23c7eb6be223796b8621454f49e`

This file is the durable handoff for the next maintainer or assistant. It records what was actually changed and what remains blocked. It is not a substitute for live verification of GitHub, Firebase, Play Console, or hosting state.

## Completed and merged

- PR 83 merged into `main` at `973c972b969f4eff5dc0f9fb596a07db8e53953a5`: fixed the Room main-thread path, tightened Firestore comment reactions, removed the unsafe global uncaught-exception handler from Compose, and added regression tests.
- PR 84 merged at `d16a9879a44a6d431d9b14bd4785bc740c27f4a4`: added Firestore-driven stream fallback without invented URLs or loops, and added push-notification category targeting with legacy-token compatibility.
- PR 85 merged at `ffbb182b328693a9038098ae28ff142b52762a31`: shipped the official primary stream default into the Android repository.
- PR 86 merged at `6031e05ebec034e39fb346a541a2c446b457bd96`: added this project work log and archived the superseded pre-remediation audit.
- PR 87 merged at `56ecec3a28c3a23c7eb6be223796b8621454f49e`: added `docs/RELEASE_CHECKLIST.md` with only the remaining owner-gated release requirements.
- CI #435 on merged main passed after PRs 83-85. PR 86 CI #436 passed. PR 87 CI #438 passed. Post-PR-87 main CI #439 was running at the time of this update and must be checked live before being called successful.
- No stream URL is embedded as an Android fallback. The approved primary stream must be supplied by the channel operator through Firestore; no YouTube or invented URL is used as an automatic backup.
- The working privacy page is `https://elwataniatv-channel.vercel.app/privacy`. The old `elwataniatv.dz/privacy` route returned 404 and was removed from runtime documentation.
- The admin panel supports YouTube links in two separate places: `social` for public channel/account links, and `streams` with type `youtube` for a YouTube live source.

## Still incomplete or requiring owner-controlled access

- No signed release AAB has been produced or verified on a real Android device.
- No final Play Store screenshots or release listing are present.
- Firebase Cloud Functions production deployment is not fully verified: code is deploy-ready (region aligned to `europe-west1` for the documented `eur3` Firestore location, server-side comment rate limiting added, root `firebase.json`/`.firebaserc` added). Deployment is blocked on owner actions: Blaze plan upgrade + `firebase login` + `firebase deploy --only functions` and `firebase deploy --only firestore:indexes`.
- App Check Enforcement is not proven active in Firebase Console.
- The official privacy URL/domain still needs a real production decision; the Vercel URL is the verified working page.
- English/French resource support and a real language switch are not complete.
- A second official broadcaster-provided stream is unavailable, so there is no independent backup source.

## Documentation decisions

- Operational documents remain in place: README, BUILD, Firebase setup/action guides, Firestore security/rules documents, Play Store guide, privacy policy, user guide, admin deploy guide, and store-assets guidance.
- `todo.md` remains because it mixes live tasks with historical release notes and needs a deliberate split, not deletion.
- `FIREBASE_SETUP.md` and `design.md` need reconciliation against the current project, but are not deleted blindly.
- The dated audit for the pre-remediation commit `b67ca14` is archived under `docs/audits/` and marked superseded. It is historical evidence, not the current release verdict.

## 2026-08-09 — Functions deploy readiness (region + rate limiting)

- **Region decision applied in code**: `functions/index.js` (both Firestore triggers) and
  `functions/gemini-proxy.js` now use `europe-west1` (the documented Firestore location is `eur3`,
  Europe multi-region; Firestore event triggers must match the database region). If the Console shows a
  different location, change `REGION` — deployment fails loudly on mismatch.
- **Server-side rate limiting added** to `moderateComment`: per `deviceId`, per program, max 5 comments
  per 60 s → `moderation: { ok: false, reason: "rate_limit" }`. Pure helpers in
  `functions/lib/comment-rate-limit.js` with unit tests; the query requires the new composite index
  `comments(deviceId ASC, createdAt DESC)` in `firestore.indexes.json` and fails open if the index is
  not deployed yet (profanity moderation still runs).
- **Root-level Firebase config added**: `firebase.json` + `.firebaserc` (project `elwataniatvapp`) so
  `firebase deploy --only functions` and `firebase deploy --only firestore:indexes` work from the repo
  root, matching the documented deploy flow. `admin/firebase.json` remains the hosting deploy point.
- **Not done (owner-gated)**: Blaze plan upgrade (billing decision) and the actual `firebase deploy`.
  `firebase login` must be run by an account owning `elwataniatvapp`.
- Docs updated: `admin/DEPLOY.md`, `FIREBASE_ACTION_GUIDE.md`, `FIREBASE_SETUP.md`, `todo.md`.

## 2026-08-09 — Free-tier moderation: client filter + hardened rules (no Cloud Functions)

- **Decision**: stay on the free Spark plan; do NOT deploy Cloud Functions. Moderation is now two free layers.
- **Client filter**: `app/src/main/java/com/elwataniatv/app/util/ProfanityFilter.kt` — a 1:1 Kotlin port of
  `functions/profanity.js` (same banned list, normalise, isProfane, thresholds). Called in
  `WataniaRepository.addComment` (before local insert/network, with a Toast + onResult) and in
  `FirebaseSync.postComment` (before anonymous sign-in). Unit tests mirror the JS tests.
- **Rules hardening** (`admin/firestore.rules`), verified by the emulator (21/21):
  - `createdAt == request.time` — comments must use `FieldValue.serverTimestamp()`; client timestamps rejected.
  - Per-user interval: max 1 comment / 30 s via `users/{uid}.lastCommentAt` + `duration.value(30, 's')`;
    the user-doc rule forces `lastCommentAt` to be a server timestamp too (no backdating).
  - Max one link per comment (`.*http.*http.*` pattern) + existing 500-char cap and hasOnly payload check.
  - App now writes the comment + `users/{uid}.lastCommentAt` in one atomic batch (FirebaseSync.sendCommentDoc).
- **Emulator findings** (worth recording): the Firestore emulator v1.19.8 rules runtime lacks
  `duration()`/`.value()` (use `duration.value(n, 's')`) and `get()` errors on non-existent docs
  (use `exists()` + short-circuit). Emulator port in `rules-tests/firebase.json` is 8081 because the
  sandbox occupies 8080. `withSecurityRulesDisabled(cb)` passes a context, not a Firestore instance.
- **Signed release build**: `keystore/release.keystore` generated (alias `watania_release`, gitignored;
  credentials stored outside the repo for the owner). `bundleRelease` pipeline validated up to the SDK
  step; a signed AAB is produced once an Android SDK is available or built in CI with signing env vars.
- **Still owner-gated**: `firebase login` + `firebase deploy --only firestore:rules`; real
  `google-services.json` (needed before publishing the release build); Cloud Functions remain optional.

## 2026-08-09 — App Check debug provider + API-key restriction guidance

- The app already initialised App Check with Play Integrity. Added the standard debug path:
  `firebase-appcheck-debug` dependency + `MainActivity` now picks the **Debug provider** in
  `BuildConfig.DEBUG` builds and **Play Integrity** in release builds (emulator/unsigned builds
  keep working; register the debug token in the console once enforcement is enabled).
- Documented the owner-side App Check steps (Play Integrity API is free; enable enforcement only
  after a signed build with the SDK ships; start in Monitor mode) in `admin/DEPLOY.md`,
  `FIRESTORE_SECURITY_TASKS.md` (TASK-003 updated + new TASK-003A), `FIREBASE_ACTION_GUIDE.md`, `todo.md`.
- API-key restriction guidance: the built-in config in `admin/index.html` carries an **Android-style
  appId** (`1:954896144400:android:...`) — likely the Android key used from a browser. Restricting
  that key by HTTP referrers would break the native app, so the docs instruct creating a dedicated
  **Web app config**, restricting the web key by referrers, and restricting the Android key by
  package name + SHA-1. (Console actions are owner-gated.)
- Region conflict (functions vs Firestore): already resolved in code since the earlier PR
  (`REGION = europe-west1` documented for `eur3`) — no further change needed.

## Next recommended order

1. Build and test a signed release AAB using owner-provided signing configuration and `google-services.json`.
2. Test the release build on a real Android device, including HLS playback, YouTube link behavior, Auth, App Check, notifications, and offline states.
3. Verify or deploy production Functions and decide whether to enable App Check Enforcement, then retest.
4. Prepare genuine Play Store screenshots and complete the privacy-domain decision.
5. Split `todo.md` into a short live checklist and a historical changelog after reviewing its contents.

## 2026-08-10 — Dead Gemini code removal + large-file splits (no behaviour change)

- **Dead Gemini code removed (Cloud Functions)**: deleted `functions/gemini-proxy.js` and
  `functions/lib/gemini-helpers.js`; removed the `exports.geminiGenerate` line from
  `functions/index.js` (no client calls it — the AI-search feature was removed earlier). The Gemini
  test blocks in `functions/test/helpers.test.js` were dropped and the file renamed to
  `functions/test/push-helpers.test.js` since it now only covers `lib/push-helpers`.
  `npm test` in `functions/`: 10/10 pass. Docs that referenced `gemini-proxy.js`
  (`FIREBASE_ACTION_GUIDE.md`, `FIREBASE_SETUP.md`, `admin/DEPLOY.md`) updated.
- **`SettingsScreen.kt` (1892 → 24 lines of orchestration)** split into `ui/screens/settings/`:
  `SettingsScreen.kt` (state + composition), `SettingsHeaderAndBanner.kt`,
  `AppearancePreferencesCard.kt`, `SatelliteFrequenciesCard.kt`, `ContactChannelCard.kt`,
  `SettingsAboutSections.kt`, `FeedbackDialog.kt`, `AdminAuthDialog.kt`, `AdminConsoleState.kt`
  (snapshot-backed form state holder), `AdminConsoleDialog.kt` (tab host) and one `Admin*Tab.kt`
  file per console tab (Telemetry/Popup/Streams/Breaking/Social/Websites/Archive/EPG/Media/
  Security/Banners/WebCMS). `MainActivity.kt` imports the new package explicitly.
- **`FirebaseSync.kt` (997 → facade)** split by concern into `FirebaseAuthSync.kt`,
  `FirestoreContentSync.kt` and `FcmTokenSync.kt`; the `FirebaseSync` facade keeps the exact same
  public API (flows, start/stop/reconnect, admin ops, `registerFcmTokenNow`), so
  `WataniaRepository` and `FcmMessageService` needed no changes. `InAppNotification` stays in
  `data/remote` (imported by the repository).
- **`ArchiveScreen.kt` (1139)** split into `ui/screens/archive/`: `ArchiveScreen.kt` (display only),
  `ArchiveLogic.kt` (pure helpers: URL validation, YouTube id/thumbnail derivation, category icon,
  category extraction, date sorting) and `ArchiveProgramCard.kt` (card + skeleton). Query/category
  filtering remains in `MainViewModel.filteredArchive` as before.
- **Verification**: `./gradlew test` BUILD SUCCESSFUL; `./gradlew lint` 0 errors / 75 warnings
  (all pre-existing; the only 2 warnings touching new files are the pre-existing
  `ModifierParameter` style notes carried over unchanged); `npm test --prefix functions` 10/10.
  CI re-verifies on the PR.
