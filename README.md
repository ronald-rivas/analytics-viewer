# Analytics Viewer For Firebase

View Firebase Analytics events and user properties from a connected Android device or emulator in real time, directly inside your IDE.

> This plugin is not affiliated with, endorsed by, or sponsored by Google or Firebase.

## Features

- Live list of Firebase Analytics events, newest first (up to 1000 events).
- Event parameters shown in a **Parameters** tab.
- User properties shown in a **Properties** tab.
- **Search** button (magnifier) to filter events by name.
- **Clear** button to empty the event list (user properties are kept).
- **Sync** button to reconnect to adb and re-enable debug logging, for example after reconnecting a device.

## Requirements

- Android Studio or an IntelliJ-based IDE.
- `adb` (Android SDK Platform-Tools). The plugin looks for it in `ANDROID_HOME`, `ANDROID_SDK_ROOT` and the default SDK locations, and falls back to `adb` in the `PATH`.
- A connected device or emulator with USB debugging enabled.

## How it works

When the tool window opens, the plugin runs:

```
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb logcat -v time -s FA FA-SVC
```

The `setprop` values are temporary and are lost when the device restarts. Use **Sync** to apply them again.

## Privacy and security

- Everything runs locally. The plugin makes no network connections and does not collect or send data.
- Events are only displayed in memory and are not stored.
- Events may contain sensitive data sent by your app. Use debug builds.

## Usage

1. Connect a device or start an emulator (`adb devices` must list it as `device`).
2. Open the **Analytics Viewer For Firebase** tool window.
3. Use your app. Events appear in the list. Select one to see its parameters.

## Development

```
./gradlew runIde        # run a sandbox IDE with the plugin
./gradlew verifyPlugin  # check compatibility
./gradlew buildPlugin   # build the zip in build/distributions
```

## License

Copyright 2026 Ronald Antonio Rivas Ramírez.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

Version: `0.4.0`
