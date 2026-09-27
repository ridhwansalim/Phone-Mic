# Phone Mic

Turn an Android phone into a live microphone for a connected Bluetooth speaker.

Built by **[Ridhwan S.](https://github.com/ridhwansalim)** · [Instagram](https://www.instagram.com/ridhwan_salim/) · [LinkedIn](https://www.linkedin.com/in/ridhwan-s/)

**Current version: 1.4 (build 9).** This revision removes the unsuccessful speaker-calibration feature. Start microphone now connects directly to the selected speaker after permissions and routing checks. The approved Home/Settings design remains.

[Download v1.4 APK](dist/PhoneMic-v1.4-debug.apk) · [Version history](docs/VERSIONS.md) · [v1.4 notes](docs/UI_AND_CALIBRATION_1.4.md)

## Features

- Built-in phone microphone to Bluetooth media audio (A2DP and supported BLE outputs).
- Start/Stop, input meter, output selection and volume on Home.
- Settings gear for vocal EQ, bass, creative echo/repeat delay, feedback protection and hold-to-talk.
- Conservative vocal preset: 100 Hz -6 dB, 400 Hz -3 dB, 1 kHz 0 dB, 4 kHz +1 dB, 10 kHz -2 dB; output 25%, bass/echo off.
- High-pass filter, noise gate and sustained-tone detection with automatic output reduction. Optional Android platform AEC when supported, with no calibration requirement.
- Foreground media notification, Stop action, notification restoration and disconnect alerts.
- Temporary audio-focus interruptions pause playback; optional resume when focus returns. Stop cancels pending resume.
- Creator modal from the Home footer, full creator card at the bottom of Settings.
- Local audio processing. No network permission, account, audio recording files or uploads.

## Use

1. Install the v1.4 APK over the existing app. The version label remains 1.4; build 9 replaces the earlier build 8 without requiring an uninstall.
2. Pair/connect a Bluetooth speaker for media audio in Android settings. The Home **Pair** shortcut opens those settings.
3. Select the output, tap **Start microphone** and allow microphone/nearby-device permissions. Allow notifications for background controls.
4. Start with low speaker volume. Speak near the phone and point the speaker away from its microphone. Phone volume keys control media volume; the app slider controls its output level.
5. Use the gear to adjust sound or enable hold-to-talk. Stop before switching outputs.

Bluetooth adds playback latency. Nearby speakers can still feed back; protection does not guarantee feedback-free use. Hold-to-talk mutes on release or leaving the app, but already-buffered Bluetooth audio may continue briefly. Only outputs exposed by Android appear in the picker, and actual routing is verified before capture and during playback.

## Calibration removed

The user repeatedly reported failed speaker calibration on a Redmi Note 13 5G running Android 15 with a boAt Aavante Bar 480. Longer probes and manual adjustments did not establish useful cancellation. Current v1.4 removes the entire calibration path, controls and native library from the APK. Retired code, tests and licenses remain under [experiments/speaker-echo](experiments/speaker-echo/README.md) for reference. The ordinary creative echo effect remains available and is separate from speaker cancellation.

This is a debug-signed testing APK. Earlier APKs remain in `dist` with [SHA-256 checksums](dist/SHA256SUMS.txt). Downgrading can require uninstalling, which clears settings. Historical releases before the Git import preserve binaries, not their original source snapshots.

## Build and tests

Use Android Studio or JDK 17, Android SDK 35 and the included Gradle 8.11.1 wrapper; Android Gradle Plugin 8.9.2. The current app no longer needs NDK/CMake.

```sh
./gradlew assembleDebug lintDebug
```

For this workspace's local toolchain:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Lint
```

The script runs DSP and interruption-state tests, builds the app, runs optional lint and copies the APK to `dist`. Toolchain/cache files live in ignored `.tooling/`. SDK installation scripts accept SDK license terms; review the terms before using them in another environment.

## Architecture

MainActivity provides native views, permissions and persisted immutable AudioSettings snapshots. MicService owns foreground execution, wake lock, audio focus, MediaSession, routing and audio resources. SessionState tracks interruption/resume intent independently of worker shutdown. AudioRecord captures built-in-microphone mono 48 kHz PCM16; AudioProcessor applies protection, five EQ bands, bass, optional creative echo, smoothed gain and limiter; AudioTrack plays media audio to the verified Bluetooth output.

FeedbackGuard detects sustained narrow-band tones using a 2048-sample FFT. Output reduction stays latched until restart. Quiet speech can be affected by the noise gate. Android platform echo-cancellation availability does not establish its effectiveness on a Bluetooth route.

DSP and session-state tests cover mute, EQ/bass, echo timing, limits, feedback protection, hold-to-talk and interruption/Stop behavior. Physical audio quality, manufacturer-specific calls/notifications, long sessions and two-speaker switching still require device testing. See [device checks](docs/DEVICE_TESTS.md).
