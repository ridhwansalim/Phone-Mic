# Calibration 1.2.2

Reported hardware result: match 0.31, estimated delay 946 ms, microphone RMS 1805, cancellation reduction 0.2 dB. Speaker is detected but useful cancellation is not established; the exact hardware cause remains unknown.

Extended probe from three to six seconds and capture from 4.5 to 7.5 seconds. Evaluate cancellation during the final steady probe second, shifted by the estimated acoustic delay. This avoids penalizing a long-delay route by evaluating after only about one second of effective training. Success threshold unchanged. UI estimates 8–12 seconds including background training.

Added complete pipeline/feedback-loop coverage at 946 ms alongside 300 ms. Both simulations passed (about 31 dB echo reduction on controlled linear paths). This does not reproduce the phone/speaker codec, room or changing clock behavior, and does not establish hardware success. Request the new complete error if the retry still fails; do not tell the user to keep repeating identical calibration attempts.
