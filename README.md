# Phone Mic

Built by **[Ridhwan S.](https://github.com/ridhwansalim)** · [Instagram](https://www.instagram.com/ridhwan_salim/) · [LinkedIn](https://www.linkedin.com/in/ridhwan-s/)

**Current version: 1.3.1.** Home contains the live microphone controls, speaker picker, output volume and creator card. Open **Settings** for EQ, bass, echo, feedback protection and calibration.

[Download current APK](dist/PhoneMic-v1.3.1-debug.apk) · [All versions and history](docs/VERSIONS.md)

The repository contains current source plus six archived debug APKs. Earlier source history was not saved. Speaker echo cancellation is experimental; the reported physical-speaker calibration failure remains unconfirmed after the v1.2.2 fix. The creator/UI updates do not change the audio engine.

Native Android app that sends the phone's built-in microphone to a connected Bluetooth media speaker. Android 8.0 or later.

## Features

- Connected Bluetooth audio output picker (A2DP and supported BLE audio devices).
- Live microphone with input meter, foreground notification and Stop action.
- App output volume, five-band equalizer, bass shelf, echo amount and delay.
- Sound settings persist between launches; hardware volume keys control media volume.
- Vocal preset (100 Hz -6 dB, 400 Hz -3 dB, 1 kHz 0 dB, 4 kHz +1 dB, 10 kHz -2 dB), 25% app volume, bass boost/echo off.
- Feedback protection: 100 Hz high-pass, noise gate, persistent-tone detection with latched output reduction, and platform acoustic echo cancellation when available.
- Version 1.2 speaker-reference cancellation: explicit quiet-probe calibration measures Bluetooth delay, trains native SpeexDSP, then subtracts the learned speaker return from mic capture using the exact post-effects audio submitted to AudioTrack.
- Hold-to-talk option for nearby speakers, with immediate software mute and echo-buffer clearing on release or leaving the screen.
- Media-session notification with Stop control, restoration from the app, and a separate disconnect/error notification when notifications are allowed.
- Pauses on temporary focus loss and optionally resumes when Android returns focus. Stop cancels pending resume. Permanent focus loss, output disconnection or route mismatch stop the session.
- Local processing only. No network permission, recording files, account or uploads.

## Use

1. Install `dist/PhoneMic-v1.3.1-debug.apk` on your Android phone (update the existing app). The earlier v1.1 APK remains in `dist` as a known-working fallback; Android normally requires uninstalling before downgrading.
2. Pair and connect the speaker in Android Bluetooth settings. Enable media audio for that device.
3. Open Phone Mic, select the speaker and tap **Go live**. With **Cancel speaker echo** enabled, confirm calibration, stay quiet with the phone/speaker in their intended positions, and wait about 8–12 seconds. A soft test sound plays while microphone monitoring is muted. Successful calibration starts mic playback automatically; a weak, noisy, clipped or ineffective calibration stops with an explanation.
4. Allow microphone / nearby-device access. Notifications provide a Stop button while the app is in the background.
5. Start with low media volume and keep the phone away from the speaker. Increase volume gradually.
6. Adjust equalizer, bass and echo while live. Stop before switching output devices.

Bluetooth playback adds hardware/codec buffering: this app does not promise zero latency or replace a dedicated wireless microphone for timing-sensitive performances. Android ultimately controls routing; if the preferred device cannot be confirmed, the app stops and asks you to select it in system settings. Only audio outputs exposed by Android appear, not every paired Bluetooth device. Some manufacturers expose only the currently active speaker.

This APK is a debug-signed test build, not a Play Store release. Version 1.0 was tested by the user on a Redmi Note 13 5G running Android 15 with a boAt Aavante Bar 480. Core playback, effects, permissions and screen-lock playback worked, but nearby-speaker feedback persisted even with echo/bass boost off; notification recovery and call resume needed improvement. Version 1.1 addresses these areas and requires fresh hardware validation. The user has not yet tested two-speaker switching or a long-duration session.

The user subsequently reported v1.1 works very well and requested stronger cancellation with the speaker in front of the microphone. Detailed v1.1 hardware retest results were not supplied. Versions 1.2 and 1.2.1 failed calibration on that hardware. Version 1.2.2 corrects the evaluation window for long Bluetooth delays; a successful hardware retest is still pending.

## Speaker cancellation (v1.2)

Keep the phone/speaker still during calibration and do not speak. Set the physical speaker/media volume before calibrating. Stop and restart to recalibrate after changing that volume or moving significantly. This mode cancels only this app's playback; it does not have a reference for music from another app or other nearby speakers. Echo cancellation does not remove the Bluetooth speaking-to-playback delay.

The probe is approximately six seconds of band-limited random noise at a bounded low PCM level, followed by silence for the speaker tail to drain. Calibration analysis/training runs off the audio worker while outgoing playback remains silent. Cross-correlation estimates 20–1200 ms delay, checks an independent probe segment, rejects clipped/uncorrelated data, and requires reduced residual energy before enabling live playback. Native SpeexDSP processes 10 ms / 480-sample frames at 48 kHz, with a 200 ms echo tail after external delay alignment. The post-EQ/bass/echo/limiter PCM accepted by AudioTrack is the reference, including partial writes. The built-in platform canceller is disabled in this mode to avoid two competing adaptive filters.

Temporary interruptions retain the trained acoustic filter but clear stale reference history; no calibration sound plays automatically after a call. A call during calibration cancels it. Stop/disconnect destroys the model; the next start calibrates again. Cancelling calibration releases a background-created native model when its worker finishes.

This implementation is adaptive within its finite acoustic tail but does not continuously re-estimate the external Bluetooth delay. Codec distortion, clipping, long reverberation, changing buffering, clock drift, other audio and movement can reduce cancellation. If sound deteriorates, lower the speaker volume and recalibrate. Keep feedback protection enabled. Untick Cancel speaker echo to use the v1.1-style path. Quiet speech may still be affected by the gate/residual suppressor; real voice quality requires listening tests.

Host-native synthetic tests using the same SpeexDSP C sources measured 25.7 dB echo-only reduction and 27.7 dB in the calibrated reference pipeline. A synthetic feedback loop with uncancelled loop gain above one retained 98% of near-input power and decayed to zero after the source stopped. These controlled linear simulations are **not** measurements or guarantees for the Redmi/boAt setup. Tests also cover double talk, silent/failed calibration, partial writes and multiple delays. See `tools/test-echo.ps1`; Microsoft Visual Studio C build tools are needed for the Windows-host JNI tests.

On first launch of v1.1, sound settings migrate once to the new vocal preset. Later customizations persist. The preset is a conservative speech starting point, not a universal best setting for every speaker or voice. Leave echo off during feedback testing. A strong sustained musical note may trigger the tone guard; its reduction stays latched until a new audio stream starts. Reduce speaker volume before restarting. The noise gate can suppress very quiet speech, and protection can be disabled for comparison at low volume. Platform echo-cancellation availability does not prove that it cancels this Bluetooth route's echo. A nearby speaker can still feed back; the app cannot promise to eliminate acoustic feedback or Bluetooth delay.

With automatic resume enabled, microphone playback returns after temporary interruptions only when Android grants audio focus again and the same output route is available. Some devices/calling apps can issue permanent focus loss, requiring manual restart. No phone-state permission is requested. Notification permission and channel settings must allow the separate disconnect alert. Notification layout and dismissal behavior can vary with the manufacturer's Android interface.

## Build

Open the project in Android Studio with JDK 17, Android SDK 35 and Gradle 8.11.1. The Android Gradle plugin is pinned to 8.9.2. Build the `app` debug APK.

Version 1.2 additionally uses Android NDK 27.0.12077973 and CMake 3.22.1; `tools/install-native-sdk.ps1` installs them into the local SDK. Native libraries ship for arm64-v8a, armeabi-v7a and x86_64, with 16 KB ELF page alignment. SpeexDSP 1.2.1 sources are vendored under `app/src/main/cpp/speexdsp`; archive provenance, checksum and license notices are recorded there. In-app notices include SpeexDSP and KISS FFT licenses.

The included Gradle wrapper also supports `./gradlew assembleDebug lintDebug` (or `gradlew.bat` on Windows) with `JAVA_HOME` and `ANDROID_HOME` configured.

For the workspace-local toolchain, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1 -Lint
```

The script runs the standalone DSP tests, builds the APK, optionally runs Android lint and copies the result into `dist/PhoneMic-debug.apk`. Tool downloads and caches live in ignored `.tooling/`. `tools/install-sdk.ps1` installs SDK packages and accepts the SDK licenses; review the Android SDK terms before using it in another environment.

## Architecture

`MainActivity` provides native Android controls and immutable audio-setting snapshots. `MicService` owns audio focus, the media session/foreground notification, wake lock, route monitoring and the audio worker. `SessionState` preserves user intent across temporary interruptions and asynchronous worker shutdown; capture/playback resources are released while paused and recreated on focus gain. `AudioRecord` captures mono 48 kHz PCM from the built-in mic; `AudioProcessor` applies optional high-pass/gate/feedback protection, five peaking filters, a bass shelf, feedback delay, smoothed gain and a soft limiter; `AudioTrack` writes speech as media audio to the selected Bluetooth output. `FeedbackGuard` analyzes 2048-sample Hann-windowed FFT frames, detecting persistent dominant narrow-band energy and reducing gain without buffering the outgoing stream. Microphone capture starts only after the selected playback route is confirmed.

`tests/AudioProcessorTest.java` verifies silence, mute, frequency response, echo timing/decay, overflow protection, sustained-tone detection, noise/brief-tone rejection, gate behavior, hold-to-talk echo clearing and the vocal preset. `tests/SessionStateTest.java` checks interruption/resume and Stop/disabled-resume cancellation. See `docs/DEVICE_TESTS.md` for checks requiring hardware.

## Android references

- [AudioTrack and preferred output routing](https://developer.android.com/reference/android/media/AudioTrack)
- [AudioRecord and input routing](https://developer.android.com/reference/android/media/AudioRecord)
- [Microphone foreground-service requirements](https://developer.android.com/develop/background-work/services/fgs/service-types#microphone)
- [Platform acoustic echo cancellation](https://developer.android.com/reference/android/media/audiofx/AcousticEchoCanceler)
- [Audio-focus interruption and recovery](https://developer.android.com/media/optimize/audio-focus)
- [Android 14+ notification dismissal changes and media exception](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)
- [SpeexDSP echo cancellation and timing requirements](https://www.speex.org/docs/manual/speex-manual/node7.html#SECTION00740000000000000000)
