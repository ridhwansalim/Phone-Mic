# Physical-device acceptance checks

## Version 1.2 priority checks

1. Update to v1.2 on the Redmi Note 13 5G / Android 15 with the boAt Aavante Bar 480. Start at low physical/media volume with the speaker in the intended position.
2. Leave Cancel speaker echo and Feedback protection enabled, tap Go live and confirm calibration. Remain quiet for the probe and verify mic monitoring does not start until successful calibration. Note the measured delay displayed in the app.
3. Speak normally near the phone, then pause. Compare repeating speaker pickup with speaker cancellation disabled at the same low level. Stop immediately if howling begins; do not rely on a high-volume feedback test.
4. Test quiet speech, consonants, continuous sentences and short pauses. Listen for swallowed words, metallic sound or pumping, not just reduced echo.
5. Face the speaker at a moderate level in the calibrated position. Increase cautiously; after any speaker/media volume change, Stop and recalibrate. Confirm whether this arrangement is actually usable before declaring the goal achieved.
6. Turn the speaker volume to zero before calibration. Calibration should fail clearly and must not silently begin uncancelled microphone playback. Test Stop, disconnect and an incoming call during calibration too.
7. After successful calibration, receive/hang up a call. Confirm supported temporary-focus recovery resumes without replaying the test noise and retains cancellation. Stop during the call must cancel resume.
8. Move the phone or speaker; if cancellation degrades, Stop and recalibrate. Record recovery and any changing delay. Bluetooth clock/buffer changes over time need hardware evaluation.
9. Verify EQ, app volume, hold-to-talk, background playback and notifications still work. The known post-effects outgoing audio must remain cancellable.
10. Once stable, run 15–30 minutes with speech and pauses. Record dropouts, heat/battery, changing echo and whether recalibration becomes necessary.

Version 1.2 has native/Java simulation and build validation only; these hardware checks remain outstanding. The user reported v1.1 as working very well without enumerating every retest result.

## General checks

These checks have not been performed by automated DSP tests or Android lint.

1. Fresh install on Android 8+ and a recent Android version. Deny microphone permission, then grant it through app settings. No crash or capture without consent.
2. Connect an A2DP speaker. Confirm the picker shows its name and that speech uses the phone mic, not a headset mic.
3. With two paired speakers, connect/select each using Android's media output settings. Stop, select and restart in Phone Mic. Confirm the actual output matches the selected speaker.
4. Start low, increase app volume and media volume independently. Confirm app volume zero mutes dry audio and echo.
5. Adjust each equalizer band, bass, echo and delay. Confirm audible changes without unstable output. Reset sound and reopen the app to verify settings persistence.
6. Turn off/disconnect the speaker during playback. Confirm the app stops, reports disconnection and does not continue on the phone speaker.
7. Lock the screen for ten minutes. Confirm ongoing audio and the notification Stop action. Reopen after stopping to check state.
8. With Resume after calls enabled, receive/hang up a call. Confirm audio/microphone capture pauses during the call and resumes when Android returns temporary audio focus. Stop during a call and confirm it does not resume. Repeat with automatic resume disabled. Permanent focus loss must require a manual restart.
9. Repeatedly start/stop, rotate the phone, background/reopen, revoke permissions and toggle Bluetooth. Confirm no overlapping workers or lingering microphone indicator.
10. Measure perceived speaking-to-playback delay with the intended speaker. Assess suitability for the intended use; Bluetooth latency varies by hardware and codec.

Record phone model, Android version, speaker model, test outcomes and observed delay before distributing a production release.

## Version 1.1 focused retest

- Install as an update on the Redmi Note 13 5G (Android 15), with the boAt Aavante Bar 480. Confirm the new vocal preset is applied once and later adjustments persist across reopening.
- At low speaker volume, with echo/bass boost off, compare feedback protection on/off. Speak close to the phone and direct the speaker away from it. Do not intentionally sustain loud feedback; stop immediately if howling develops.
- Check whether the app reports platform echo cancellation enabled/unavailable. This is an availability indicator, not proof of cancellation performance.
- Confirm normal speech remains clear, including quiet words. If the tone guard reduces volume, verify the message is visible and gain does not automatically rise again. Lower physical speaker volume before restarting.
- Try hold-to-talk: hold while speaking, release between sentences. Confirm software output mutes, old echo does not replay on the next press, and leaving the app mutes. Hardware-buffered Bluetooth audio may continue briefly.
- Swipe the media notification if the phone permits it. Reopen the app or tap Show notification controls; controls should return without stopping/restarting audio.
- While the app is in the background, disconnect the speaker. Confirm a separate Phone Mic stopped notification remains. Verify both notification permission and the Microphone interruptions channel are enabled.
- Repeat the call checks above, including Stop during a call. Confirm resumed audio uses the same speaker and there are no overlapping audio streams.
- After the sound is acceptable, run 15–30 minutes and assess battery, heat, dropouts and feedback stability.

## User-reported version 1.0 results

Installation, permission allow/deny, connected device listing, playback, built-in microphone, volume, EQ, bass/echo controls, settings/reset, Stop/restart and background/screen-lock playback passed. Feedback/repeating sound remained with echo and bass boost zero. Dismissed notification was not restored until Stop/start. Disconnect was only visible inside the app. Calls paused audio but did not resume it. Two-speaker switching and long-duration use remain untested.
