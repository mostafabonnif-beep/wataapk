# ElWatania TV V10 — Production Scope

## Product direction
Official TV-channel companion application:
- Main television live stream
- Multiple official YouTube channels
- Programs/archive
- EPG
- Social/official links
- Notifications
- Admin/CMS controls

## V10 changes in this baseline
- Android TV manifest declarations and LEANBACK launcher.
- TV banner asset.
- Compose for TV dependencies (`androidx.tv`).
- Media3 upgraded to stable 1.10.1.
- Media3 Session dependency added for the player/service architecture.
- App version advanced to 10.0.0 / versionCode 20.

## Release gates still required
- Online Gradle build and tests.
- Android TV hardware/emulator navigation QA.
- 16 KB page-size verification.
- 32/64-bit verification.
- WebView security regression tests.
- Player lifecycle/configuration-change tests.
- Google Play Internal Testing / pre-launch report.

## Control Center policy
The Android app no longer embeds the administrator console or admin login. Administration is web-only under the `admin/` Firebase web console. The consumer app exposes no secret admin gesture or admin dialog.
