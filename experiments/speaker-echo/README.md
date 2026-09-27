# Retired speaker-cancellation experiment

This code is preserved for research and historical reference. It is not compiled or packaged into Phone Mic v1.4 build 9 or later. The app has no calibration controls, probe sound or native SpeexDSP dependency.

Repeated tests on the user's Redmi Note 13 5G / Android 15 / boAt Aavante Bar 480 failed calibration despite longer probes and gain controls. Reported reduction was only 0.2–0.3 dB. A fixed linear simulation passes, but adding 300 ppm relative timing drift to a 737 ms simulated echo path reproduces 0.3 dB reduction (match 0.37). At 1000 ppm the test signal match falls below threshold. This demonstrates a fixed-delay limitation; it does not prove the phone's exact hardware failure cause.

The implementation lacks continuous clock compensation and delay tracking. Longer/louder tests do not solve that limitation. The user authorized removal if reliable calibration could not be established. Acceptance thresholds were not weakened to manufacture a pass.

Sources, licenses and previous tests are retained here. Run tools/test-echo.ps1 for the experimental host-native tests with Visual Studio C build tools installed. They do not validate the current shipped audio path or real Bluetooth hardware.
