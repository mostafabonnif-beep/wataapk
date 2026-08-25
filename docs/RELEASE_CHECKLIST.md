# Release Checklist

Only the following owner-gated items remain before public release:

- [ ] **Signed AAB**: produce and verify a release-signed Android App Bundle; CI currently provides no signed release artifact.
- [ ] **`google-services.json`**: provide the production Firebase configuration through the secure release/build process; do not commit secrets to the repository.
- [ ] **Real-device test**: install the release build on a physical Android device and verify launch, stream playback, fallback behavior, notifications, comments, reminders, and offline/recovery flows.
- [ ] **Screenshots**: capture current, genuine screenshots from the release candidate for the supported phone layouts and store listing.
- [ ] **Play Console**: complete the Play Console listing, upload the signed AAB, configure required declarations, testing tracks, content details, and release metadata.
- [ ] **Firebase Functions**: deploy and verify the production Functions, including moderation and category-targeted push delivery, with logs confirming successful execution.
- [ ] **App Check Enforcement**: enable and verify Play Integrity enforcement for the Android app, then validate that legitimate release builds can access Firebase services.
- [ ] **Rules parity**: diff the deployed Firestore Rules against `admin/firestore.rules`, resolve every difference, and record the verified production revision.
- [ ] **Privacy domain**: make the official privacy-policy domain serve the current policy at its public `/privacy` endpoint and verify the final URL over HTTPS.
- [ ] **Translations**: replace the cosmetic language selector with complete, tested translations for every language presented to users, or limit the selector to the languages actually supported.
- [ ] **Official backup stream**: obtain a genuine independent backup stream from the broadcaster/operator, configure it as a separate Firestore stream, and verify automatic failover; do not invent or substitute a URL.
