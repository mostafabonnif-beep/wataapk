# Elwatania TV — handoff

## Current state

- Repository: `mostafabonnif-beep/wataapk`
- Android package: `com.elwataniatv.app`
- Firebase project: `elwataniatvapp`
- Public download page: https://elwataniatvapp.web.app/download
- Admin panel: https://elwataniatvapp.web.app/admin/
- `admin/firebase-config.js` is generated during hosting preparation by `scripts/build-hosting.js`; it is ignored by Git. Never hard-code service-account credentials or commit `google-services.json`.
- Firestore content, streams, archive, websites, social links, banners, branding, maintenance, and version settings are controlled by the admin panel.

## Safe workflow

1. Read `README.md`, `FIREBASE_SETUP.md`, and `admin/DEPLOY.md` before changes.
2. Never commit service-account JSON, passwords, signing keys, or `.env` files.
3. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` after Android changes.
4. For admin changes, run `node scripts/build-hosting.js`, then deploy only the hosting target after review.
5. Verify the public download page and admin login after deployment.
6. Do not change Firestore rules or delete production data without an explicit backup and approval.

## Known product boundary

Cloud Functions are intentionally not deployed on the free Firebase plan. Aggregated reactions remain read-only until a trusted server-side writer is deployed. Comments use anonymous Firebase Auth plus Firestore rules and local profanity filtering.

## Handoff note

The current user has provided Firebase service-account access through the chat upload and authenticated the admin panel. Credentials must remain private and must never be copied into this file, source code, logs, or chat replies.
