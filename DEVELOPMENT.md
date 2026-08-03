# Development

<details>
<summary>📖 Contents ⬇️</summary>

## Contents

- [Toolchain](#toolchain)
- [Project layout](#project-layout)
- [Format and check](#format-and-check)
- [Build and install](#build-and-install)
- [Related](#related)

</details>

## Toolchain

- JDK 17 (`JAVA_HOME`)
- Android SDK (`ANDROID_HOME` / `local.properties` `sdk.dir`)
- Gradle wrapper 8.11.1, AGP 8.9.1, Kotlin 2.1.10, Compose BOM 2025.02.00

If you already use Harrix Swiss Knife Android tooling, run `install\setup-android-sdk.bat` from that repo once, then copy or recreate `local.properties` here (same `sdk.dir`).

## Project layout

Single module `:app`, package `dev.harrix.notes`:

- Domain / prefs: root package (`NotesTreeRepository`, `NotesViewerPreferences`, …)
- UI: `ui.notes` (viewer, tree drawer), `ui.settings`, `ui.theme`
- Shell: `MainActivity` → `MainScreen` hosts `NotesViewerScreen` + settings overlay

Notes access is SAF-only (no media storage permissions).

## Format and check

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat qualityCheck
```

Or from Harrix Swiss Knife (folder must contain `gradlew.bat`):

```text
hsk android format D:\GitHub\harrix-notes-android
hsk android check D:\GitHub\harrix-notes-android
```

## Build and install

```powershell
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\HarrixNotes-release.apk
```

Release uses the debug keystore for sideload (same approach as HSK Android). Use a dedicated keystore before Play Store publishing.

## Related

- VS Code: Harrix Notes Explorer (folder collapse / title rules mirrored in `NotesTreeRepository`)
- Former host app: Harrix Swiss Knife Android (Gallery / Video Cleaner only after Notes extraction)
