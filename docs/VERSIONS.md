# Version archive

This repository begins with the current v1.3.1 source. Development before this import was not recorded in Git. Older APKs are preserved exactly as found; their original source snapshots are unavailable. Historical archive tags/releases identify APKs, not reproducible source checkouts for those versions.

| Version | Saved build | Changes |
| --- | --- | --- |
| 1.0 | [APK](../dist/PhoneMic-v1.0-debug.apk) | First-ever version: microphone-to-Bluetooth playback, device selection and effects. |
| 1.1 | [APK](../dist/PhoneMic-v1.1-debug.apk) | Vocal preset, feedback protection, hold-to-talk, notification and interruption handling. |
| 1.2 | [APK](../dist/PhoneMic-v1.2-debug.apk) | Experimental SpeexDSP speaker-reference cancellation and calibration. |
| 1.2.1 | [APK](../dist/PhoneMic-v1.2.1-debug.apk) | Stronger calibration probe, delay validation and diagnostic messages. |
| 1.2.2 | [APK](../dist/PhoneMic-v1.2.2-debug.apk) | Longer calibration and delay-aligned validation window. |
| 1.3 | [APK](../dist/PhoneMic-v1.3-debug.apk) | Separate Home and Settings, creator portrait. |
| 1.3.1 | [APK](../dist/PhoneMic-v1.3.1-debug.apk) | Ridhwan S. credit and GitHub, Instagram and LinkedIn links. |

All available APKs are debug-signed test builds. Android may require uninstalling before installing an older version; uninstalling clears app settings. APK checksums are listed in [SHA256SUMS.txt](../dist/SHA256SUMS.txt).

Nearby-speaker cancellation remains experimental: calibration previously failed on the Redmi Note 13 5G / boAt Aavante Bar 480 setup. Synthetic tests pass, but physical-device success after the calibration fix is unconfirmed.
