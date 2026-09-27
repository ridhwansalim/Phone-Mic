# Phone Mic 1.3 interface

Home now contains Start/Stop, input meter, conditional hold-to-talk button, connected Bluetooth speaker selection, connection shortcut, output volume and creator photo credit. Audio configuration is accessed through Settings: speaker echo calibration, hold-to-talk mode, feedback protection, interruption resume, notification restoration, device refresh, equalizer, bass, echo, preset and license information. Settings retains a Stop microphone action.

Navigation uses two views within MainActivity so changing settings does not restart the foreground audio service or lose its state. Android Back returns from Settings to Home; current page survives activity recreation. Switching pages releases hold-to-talk. Existing preference keys are retained.

The supplied portrait is copied unchanged to drawable-nodpi/creator_portrait.jpg. Home shows a cropped viewport via ImageView, with a larger original portrait on tap. Version 1.3.1 credits the user-confirmed name Ridhwan S. and adds external profile buttons: GitHub https://github.com/ridhwansalim (retrieved from portfolio project memory), Instagram https://www.instagram.com/ridhwan_salim/ and LinkedIn https://www.linkedin.com/in/ridhwan-s/ (both supplied by the user). Missing browser handlers show a message instead of crashing. No identity is inferred from the photo.

Hardware verification: open Settings while live, adjust effects, return with Back, stop from either page, rotate on Settings, check photo at large font sizes. This update does not claim to resolve the outstanding physical-speaker calibration failure.
