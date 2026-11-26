# TChat

TChat Android application with GitHub Actions APK compilation and a web preview.

## Build & Install Android APK via GitHub Actions

1. Push this repository to GitHub (to `main` or `master`, or trigger **Build Android APK** manually under the **Actions** tab).
2. Open the completed workflow run in **Actions** and download the **`TChat-debug-apk`** artifact.
3. Unzip it to get `app-debug.apk`, transfer it to your Android device (Android 7.0+ / API 24+), allow **Install unknown apps** when prompted, and install & launch **TChat**.

## Android Features

- **Spaces & Direct Messages**: Browse active conversations, create new Spaces/DMs, tap any message to reply, and attach files.
- **Local Network & TCP Chat**: Shows your device's local IPv4 address, scans the `/24` local subnet for TChat peers on port `8888`, and supports hosting/connecting over TCP sockets.
- **Bluetooth Chat**: Supports starting a Bluetooth RFCOMM host (`TChatBT`) and connecting to paired Bluetooth devices.
- **Settings & Drawer**: Navigation drawer, floating bottom dock, saved username, and System / Dark / Light theme selection.