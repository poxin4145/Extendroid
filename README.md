# Extendroid

Android multi-tasking manager with desktop-like multi window support.

**English** | [简体中文](README.zh-CN.md)

> This is a community fork of [legendsayantan/Extendroid](https://github.com/legendsayantan/Extendroid).
> The `master` branch is a rewrite of the original app; the `legacy` branch holds the old version.

## What this fork adds

- **Optional Simplified Chinese UI** — the app follows the system language and can be switched
  at runtime in *Options → Language* (Follow system / 简体中文 / English).
- **Builds without Firebase** — Echo (remote access) is optional. When a build has no
  `google-services.json`, the Echo entry points are hidden instead of crashing the app.
- **Out-of-the-box build** — no git-ignored secret files are required to compile.
- **Automated releases** — every `v*` tag is built and published as a signed APK by GitHub Actions.
- **In-app help** — *Options → Help* shows the quick start guide, localized.

## Requirements

- Android 10 (API 29) or newer
- [Shizuku](https://shizuku.rikka.app/) running (started over ADB or root)
- “Display over other apps” permission
- On Xiaomi devices: the additional MIUI popup-window permission

## Download

Grab the latest `Extendroid-<version>.apk` from the [Releases](../../releases) page.

Upgrading from an older build? If Android reports a signature mismatch, uninstall the previous
version first (app settings are lost).

## Usage

1. Start Shizuku (over ADB or root) and open Extendroid.
2. On the home screen, complete **Setup Shizuku** and grant the permission request. Then allow
   “Display over other apps”. Both rows show a check mark when done.
3. Open the control menu from the notification or from the floating ball.
4. Pick an app from **☆** (recent/frequent) or **＋** (all installed apps) to load its live preview.
5. In the preview tab:
   - **Tap** an app to open it in a popup window.
   - **Long press** the window to go fullscreen.
   - **Tap ◇** to interact with the app inside the preview, **long press ◇** to close it.
6. On a popup window:
   - Drag the top handle to move it.
   - Tap the top handle for minimize / maximize / back / close.
   - Drag from the bottom corners to resize it freely.
7. Use the **⋮** menu to turn the screen off while every app keeps running.
8. Change the language in *Options → Language*; the overlay menu picks it up immediately.

More detail lives in the in-app help (*Options → Help*) and in [README.zh-CN.md](README.zh-CN.md).

## Build from source

```bash
git clone https://github.com/poxin4145/Extendroid.git
cd Extendroid
./gradlew :app:assembleDebug      # or :app:assembleRelease
```

Requirements: JDK 17, Android SDK 35, Gradle wrapper 8.14 (downloaded automatically).

Release builds are signed with `EXTENDROID_KEYSTORE`, `EXTENDROID_KEYSTORE_PASSWORD`,
`EXTENDROID_KEY_ALIAS` and `EXTENDROID_KEY_PASSWORD` when those environment variables point to a
keystore; without them `assembleRelease` produces an unsigned APK. The CI workflow handles this
automatically — see [.github/signing/README.md](.github/signing/README.md).

### Echo (optional)

Echo needs a Firebase project and the upstream backend:

1. Add your `app/google-services.json` (the Google Services Gradle plugin is applied automatically
   once the file exists).
2. Replace `app/src/main/res/values/strings_echo_backend.xml` with your own
   `app/src/main/res/values/secrets.xml` containing `url_backend_prod`, `url_backend_dev`,
   `mappingJsonUrl` and `disclaimerUrl`.

Without those two pieces the app builds and runs fine, with Echo hidden.

## Project layout

| Path | Content |
| --- | --- |
| `app/src/main/java/.../ui` | overlay menu, floating ball, popup windows |
| `app/src/main/java/.../lib` | virtual display / input injection helpers |
| `app/src/main/java/.../echo` | WebRTC based remote access (optional) |
| `app/src/main/res/values` | English strings |
| `app/src/main/res/values-zh-rCN` | Simplified Chinese strings |

## License

GNU General Public License v3.0 — see [LICENSE.txt](LICENSE.txt).

Upstream note: you must not distribute this software, original or modified, to any platform
without referencing its original source code, and publishing to app stores without permission
is forbidden.
