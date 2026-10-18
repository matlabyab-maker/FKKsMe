# FKKsMe — Persian Android Keyboard

Android Studio / GitHub Actions project for the FKKsMe Persian keyboard.

## Features
- Persian key layout inspired by the supplied reference image.
- Long-press variants for ا، ی، ک، و (including common و forms).
- Built-in Persian/English layout toggle using the 🌐 EN/فا key.
- Visible Enter key; backspace key is wider.
- Paste, Copy, Copy Screen, Select, Undo, Redo, History, Tool, Muse, Resize controls.
- Tool opens a simple working calculator.
- Muse opens an interactive touchpad with pointer movement, click, zoom, and close controls.
- Up/down buttons highlight softly yellow when tapped and move the editor cursor/selection where supported.
- GitHub Actions builds a debug APK on push and pull requests.

## Build with GitHub / MGit
1. Extract the ZIP. Keep `settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, `README.md`, `.github/`, and `app/` directly in the repository root.
2. Create a repository named `FKKsMe` and upload/push the extracted project files (not the ZIP itself).
3. In MGit, initialize/open the repository folder, stage all project files, commit, then push. GitHub Actions runs automatically after the push.
4. Download the APK from **Actions → Android Build → Artifacts → FKKsMe-debug-apk**.

The workflow uses the Gradle setup action and does not depend on a missing `gradlew` wrapper. If MGit reports no changes, check that files were extracted into the repository root and that `.gitignore` has not been altered.

## Install / enable keyboard
Install the built APK, then open Android Settings → System/General management → Languages & input → On-screen keyboard → Manage keyboards, enable **FKKsMe**, and select it as the current keyboard.


Note: the keyboard itself has no suggestion/correction strip. Android spell-check behavior inside a target app can still be controlled by that app or the system keyboard settings.
