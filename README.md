# EazyTagg Agent App

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
