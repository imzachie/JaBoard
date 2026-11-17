# JaBoard

A native Kotlin + Jetpack Compose dashboard and utility app (`com.jmrc.jaboard`) with a Pixel Cobalt fallback palette, optional Material You color, and an expressive editable home grid. JaBoard is a standalone app, not a launcher replacement.

## Project status

The Android source project, Gradle configuration, manifest, application icon, local persistence, screens, and tests are implemented in this repository. **A debug APK could not be built in the provided sandbox**: it has no Java/JDK, Android SDK, Android build tools, Gradle installation, Android Studio, emulator, or connected device. The sandbox also cannot reach Android's SDK repository or Gradle's distribution host. The build was attempted; `./gradlew --version` stopped immediately with `JAVA_HOME is not set and no 'java' command could be found`. No APK is included or claimed to exist. Build it in an Android development environment using the steps below.

## Features

- **Home:** adaptive mixed-size Bento cards for clock/date, live battery status, system memory, internal storage, Open-Meteo weather, active media sessions, upcoming calendar events (when allowed), and working Notes / To-dos / Calculator shortcuts. Long-press and drag cards to reorder.
- **Explore:** utility search and category filters for Device, Lifestyle, Productivity, and Media.
- **Studio:** dashboard presets, hide/show and size controls, reorder controls, reset-to-default, system/light/dark theme, and Dynamic Color toggle.
- **Settings:** contextual optional-permission controls, privacy information, local weather-cache clearing, app version, and license/attribution information.
- **Utilities:** battery details from `ACTION_BATTERY_CHANGED`; RAM and storage from Android APIs; device and connectivity info; Open-Meteo city search and current/hourly/seven-day forecasts with a Room cache; real playback controls using Android MediaSession through Notification Listener access; Calendar Provider events; local Room notes and to-dos; calculator; clock; stopwatch with laps; and countdown timer.
- **Persistence:** Room stores notes, tasks, and the latest weather result. Preferences DataStore stores theme, dynamic color, dashboard order/visibility/sizes/preset, and selected weather location.
- **Privacy:** no account, analytics, ads, broad storage access, or background location. Notes and tasks remain local.

## Build requirements

- JDK 17
- Android SDK Platform 35 and Android Build Tools 35.x
- Gradle 8.9 (the checked-in Gradle Wrapper downloads it on a normal network)
- Android Studio is optional; command-line builds are supported

Set `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) and either let Gradle locate the installed JDK or set `JAVA_HOME`. Then run:

```bash
./gradlew assembleDebug
```

The expected output is `app/build/outputs/apk/debug/app-debug.apk`. To run local tests:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Install to a connected Android device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. No device test has been run in the sandbox.

## Permissions

| Permission / access | Purpose | Required? |
| --- | --- | --- |
| `INTERNET` | Open-Meteo geocoding and forecasts | Only for live weather; cached weather remains local |
| `ACCESS_NETWORK_STATE` | Show network connection type/status | Optional utility information |
| `ACCESS_COARSE_LOCATION` | Use Android's last-known location for weather | Optional; city search works without it |
| `READ_CALENDAR` | Read upcoming event titles and times from Calendar Provider | Optional |
| Notification Listener access (user enabled in Android Settings) | Read active media session metadata and invoke play/pause/skip/seek | Optional; no fake player is shown |

JaBoard does not request fine/background location, broad storage access, or notification-posting access. Timer completion is shown in-app.

## Data sources and implementation notes

- Weather: [Open-Meteo Forecast API](https://open-meteo.com/) and its geocoding API; no API key is required. The most recent successful forecast is cached in Room and retained when a refresh fails.
- Battery: Android's sticky battery broadcast. Battery health and temperature are shown only when reported by the system.
- Memory and storage: `ActivityManager.MemoryInfo` and `StatFs`; memory is system-wide, not a per-process claim.
- Processor: Android build / SoC metadata. Accurate continuous CPU utilization is intentionally not shown because ordinary Android apps do not receive a reliable supported metric.
- Location: coarse permission is requested only after the user chooses “Use current location”. The platform's last-known fix may be absent or old; JaBoard then asks the user to choose a city instead.
- Media: requires the user to enable JaBoard under Android's Notification access settings. Playback metadata and controls come from active `MediaController` sessions.

## Tests and verification

- Calculator unit tests cover operator precedence, parentheses, percentages, trigonometry, constants, and invalid expressions.
- Robolectric Room tests cover note create/pin/delete and task completion persistence.
- Kotlin source syntax was checked with a Tree-sitter Kotlin parser.
- **Not run:** Gradle dependency resolution, Android compilation, unit/Robolectric tests, Android Lint, APK install, emulator or physical-device testing. The sandbox lacks Java and Android SDK tooling, and the relevant download hosts are blocked. These checks must be run on a machine with the build requirements above.

## Known limitations

- First build requires normal access to Google Maven, Maven Central, and the Gradle distribution service so Gradle can resolve Android/Compose dependencies and download Gradle 8.9.
- No APK is present until a successful Android build is performed.
- Location lookup uses an available last-known location rather than requesting continuous or background updates; manual city selection is the reliable alternative.
- Timer completion is in-app and does not post a notification.
- Weather temperatures are currently shown in Celsius.
- Album artwork is displayed only when the active media session supplies artwork; playback progress and seek are available only when session metadata provides a duration.
