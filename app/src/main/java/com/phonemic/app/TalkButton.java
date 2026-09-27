package com.phonemic.app;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.Button;

/** Hold for touch input; an accessibility/keyboard click toggles transmission. */
public final class TalkButton extends Button {
    public TalkButton(Context context) {super(context);}
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled())return false;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(MicService.running && !MicService.paused)MicService.talkPressed=true;
                setPressed(true);return true;
            case MotionEvent.ACTION_UP:
                performClick();
                MicService.talkPressed=false;setPressed(false);return true;
            case MotionEvent.ACTION_CANCEL:
                MicService.talkPressed=false;setPressed(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick() {
        super.performClick();
        if(isEnabled() && MicService.running && !MicService.paused)MicService.talkPressed=!MicService.talkPressed;
        return true;
    }
}
