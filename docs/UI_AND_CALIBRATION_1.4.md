# Phone Mic 1.4 — revised build 9

The public version stays **1.4**. Internal versionCode increases from 8 to 9 so the revised APK installs as an update. The previous local build 8 APK was preserved in ignored `.tooling/archives/PhoneMic-v1.4-calibration-build8.apk` before replacement.

## Final behavior

- Start microphone opens normal live playback after permission and Bluetooth-route checks. No probe sound, calibration dialog or calibration failure can block startup.
- Calibration gain, delay correction, retry and recovery controls are removed. Saved calibration preferences are cleared on upgrade; EQ, volume, hold-to-talk and other preferences remain.
- The native SpeexDSP cancellation engine is not compiled or packaged. Experimental Java/C sources and tests are moved to `experiments/speaker-echo` outside the Android source set.
- Feedback protection, platform AEC when available, hold-to-talk, vocal EQ, bass and creative echo effects remain.
- The approved compact Home, settings gear, creator footer/modal and creator card at the bottom of Settings remain.

## Why it was removed

Physical calibration continued failing after the initial v1.4 changes. Earlier diagnostics showed only 0.3 dB reduction, match 0.18, delay 737 ms and mic RMS 700. Code review identified fixed reference timing without continuous drift tracking. A simulation with a 737 ms linear echo passed with equal clocks, but 300 ppm timing drift produced only 0.3 dB cancellation (match 0.37); 1000 ppm failed correlation (match 0.13). This exposes a weakness but is not proof of the hardware's exact cause. The user explicitly authorized removal if reliable success could not be established. No thresholds were weakened to force a pass.

## Validation and device checks

Run build/lint and the shipped DSP/session tests. Inspect the APK for absence of native libraries and experimental cancellation classes. Verify versionName 1.4 and versionCode 9. Physical checks: update over build 8 without uninstalling; confirm no calibration controls or prompt; start/stop with connected speaker; test EQ, hold-to-talk, call resume and notifications; verify Settings/Back and creator modal. Real phone/speaker testing remains necessary; removing calibration does not remove acoustic feedback or Bluetooth latency.
