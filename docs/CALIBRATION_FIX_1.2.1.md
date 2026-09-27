# Calibration update 1.2.1

User reported every v1.2 calibration failed on Redmi Note 13 5G / Android 15 with boAt Aavante Bar 480. The generic message hid the actual cause; device root cause remains unconfirmed.

- Later probe validation now searches within 20 ms of the first delay estimate instead of requiring an identical sample alignment. This tolerates small changes during calibration, but does not implement continuous clock tracking.
- Probe PCM amplitude doubled from 1800 to 3600 before filtering/fade (6 dB increase). Start at low physical speaker volume.
- Errors distinguish clipping, weak probe match, unsupported delay and insufficient measured cancellation, including correlation, delay and mic RMS diagnostics. Native initialization errors are no longer disguised as room noise.
- Cancellation effectiveness checks remain required. Disable Cancel speaker echo to use the previously working microphone mode.

Native/Java echo tests passed, including full calibrated simulation, double talk, closed-loop stability and failed calibration. Hardware success is not yet established. If calibration fails again, report the entire new message and whether the test sound is audible.
