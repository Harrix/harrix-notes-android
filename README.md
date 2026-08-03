# Harrix Notes (Android)

Android app for browsing and editing Markdown notes from a folder you choose (Storage Access Framework). Logical companion to [Harrix Notes Explorer](https://github.com/Harrix/harrix-notes-explorer) / the HSK VS Code notes panel — same folder and title conventions (`Name/Name.md`, hide `_<Folder>.g.md`, Diary / Dreams / Cases).

<details>
<summary>📖 Contents ⬇️</summary>

## Contents

- [Build](#build)
- [Editor bundle](#editor-bundle)
- [Quality](#quality)
- [License](#license)

</details>

|                           |                    |
| ------------------------- | ------------------ |
| Display name              | Harrix Notes       |
| Package / `applicationId` | `dev.harrix.notes` |
| Min SDK                   | 26                 |

## Build

Requires Windows, JDK 17, and Android SDK. From this repository root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

APK output:

- Debug: `app\build\outputs\apk\debug\HarrixNotes-debug.apk`
- Release: `app\build\outputs\apk\release\HarrixNotes-release.apk` (debug-signed for sideload)

Via [Harrix Swiss Knife](https://github.com/Harrix/harrix-swiss-knife) (add this folder to `paths_android_projects` in HSK `config.json`):

```text
hsk android format D:\GitHub\harrix-notes-android
hsk android check D:\GitHub\harrix-notes-android
hsk android build D:\GitHub\harrix-notes-android
```

## Editor bundle

The note editor is CodeMirror 6 running in a WebView, so multi-megabyte notes do not block the main thread. Assets are served through `WebViewAssetLoader` (`https://appassets.androidplatform.net/...`), not `file://`. The bundle in `app\src\main\assets\editor\editor.js` is committed, so a normal Gradle build needs no Node. Rebuild it only after changing `web\src\editor.js` or upgrading CodeMirror:

```powershell
cd web
npm install
npm run build
```

## Quality

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat qualityCheck
```

`qualityCheck` runs Spotless check, Detekt (Compose rules), and `lintDebug`.

## License

MIT — see [`LICENSE.md`](LICENSE.md).
