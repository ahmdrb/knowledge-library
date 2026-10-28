# Knowledge Library — Native Android Preview

This project is an in-progress native Kotlin/Jetpack Compose migration of the original WebView-based Knowledge Library app. It is not yet a feature-complete replacement.

## Current native preview

- Native Compose launcher and Material 3 controls
- Import UTF-8 `.txt` outlines using Android's document picker
- Four spaces per nesting level; first line is package title; leading local numbering is stripped
- Multiple local packages and nested concept display
- Basic package/concept title filtering
- Star toggles, expand/collapse, package deletion, and outline editing
- Native JSON backup import/export entry points
- No internet permission or backend requirement

See [FEATURE_INVENTORY.md](FEATURE_INVENTORY.md) for confirmed source features and migration status. See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) before changing installations or app data.

## Build requirements

- JDK 17 or compatible newer JDK
- Android SDK with API 35 platform/build tools
- Gradle compatible with Android Gradle Plugin 8.7.3 (Gradle 8.9 is the usual compatible baseline)
- Android Studio recommended

The supplied repository did not include Gradle wrapper files. From the project root, with a compatible Gradle installation available:

```sh
gradle assembleDebug
```

Expected APK output if the build succeeds:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install using Android Debug Bridge:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Build status for this source snapshot: **not verified**. Gradle was not available in the execution environment, so compilation and device installation have not been tested. The project should be built and tested before use with valuable data.

## Data preservation warning

The original version stores data in WebView `localStorage`; this preview stores data in native SharedPreferences JSON. These stores are separate. Do not uninstall the existing app or clear its data until you have exported a backup and verified that it can be imported correctly. The original-backup compatibility path is still pending verification.

## Termux

A compatible JDK/Gradle/Android SDK toolchain may be difficult to configure directly in Termux. A reliable alternative is Android Studio on a desktop or the repository's GitHub Actions build workflow. Do not assume that this preview compiles until the build has completed successfully.
