# PDynamics Agent App

Android app for door staff. Works with the existing EazyTagg PHP system (`/api/*.php`).

- Sign in (the server address is set on the sign-in screen)
- Enter an event code, choose check-in / check-out / a session
- Scan QR codes (camera) or enter a PIN
- Search by name, fix wrongly entered details
- Offline queue with automatic sync, live counts, per-agent activity log
- Dark theme by default, light theme option, tablet layout

## Build

Push to `main`: GitHub Actions builds a debug APK (Actions tab, artifact **eazytagg-agent-debug-apk**).
Or open the folder in Android Studio and run the `app` configuration.

## Testing against a local server

- Android emulator: server address `10.0.2.2/eazytagg_test`
- Real phone on the same Wi-Fi: `http://<PC-IP>/eazytagg_test` (Apache must accept connections from the network)


## Latest PDynamics test download

The `test-latest` GitHub prerelease contains the current test APK and `build-info.json` with the source commit and SHA-256 checksum. The office Git Pull page retrieves the selected app's source and matching APK into `APKS/builds`. Wait for the GitHub build to finish before downloading.

GitHub Actions runs the app tests and validates compilation. The update-compatible APK is signed and published from the office computer, keeping its signing key local. Git Pull retrieves that published release. CI does not overwrite the release with a differently signed validation build. Do not install the CI validation artifact over an existing preview. An explicitly approved, separately configured `PDYNAMICS_TEST_KEYSTORE_B64` secret can enable automated publishing later. Never commit a keystore or its base64 value. These are internal test builds, not Play Store releases.

Current preview: **0.1.1**. Includes the PDynamics icon and full wordmark, light/dark/device themes, portrait/landscape account layout, coloured guest action icons, scanner camera/torch controls, local-server attendance and event-day figures, cached counts and persistent offline action syncing. Send SMS is intentionally omitted. Device camera, torch and physical printing still require hardware validation.
