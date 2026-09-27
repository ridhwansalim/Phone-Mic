package com.phonemic.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private final int bg=Color.rgb(10,14,21), card=Color.rgb(19,27,38), accent=Color.rgb(113,230,193), muted=Color.rgb(151,166,185);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private LinearLayout content;
    private LinearLayout homePage, settingsPage;
    private ScrollView pageScroll;
    private boolean showingSettings;
    private TextView state, deviceNote, protectionNote, modeNote;
    private Spinner devices;
    private Button start, talk;
    private ProgressBar meter;
    private List<AudioDeviceInfo> outputs=new ArrayList<>();
    private final float[] bands=new float[5];
    private float volume=.35f, bass=0, echo=0;
    private int delay=220;
    private boolean protection=true, holdToTalk=false, resumeAfterInterruption=true;
    private CheckBox protectionSwitch;
    private final List<SeekBar> sliders=new ArrayList<>();
    private final AudioDeviceCallback callback=new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] d) {refreshDevices();}
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] d) {refreshDevices();}
    };
    private final Runnable update=new Runnable() {
        @Override public void run() {
            state.setText(MicService.running && !MicService.paused ? "Microphone is live" : MicService.status);
            modeNote.setText(MicService.running ? "Connected · built-in microphone" : "Built-in microphone → Bluetooth speaker");
            start.setText(MicService.running ? "Stop microphone" : "Start microphone");
            devices.setEnabled(!MicService.running);
            meter.setProgress(Math.round(MicService.level*100));
            talk.setVisibility(holdToTalk?View.VISIBLE:View.GONE);
            talk.setEnabled(MicService.running && !MicService.paused);
            talk.setText(MicService.talkPressed?"Speaking · release to mute":"Hold to talk");
            protectionNote.setText(!protection?"Feedback protection is off\n"+MicService.echoCancellation:MicService.feedbackReduced?
                "Feedback tone detected · output reduced. Lower speaker volume before restarting to reset protection.":
                "Feedback protection on · rumble filter, noise gate and automatic tone reduction.\n"+MicService.echoCancellation);
            handler.postDelayed(this,120);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        audio=getSystemService(AudioManager.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        load();
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(12),dp(20),dp(16));
        scroll.addView(content); setContentView(scroll);
        pageScroll=scroll;
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout root=content;
        homePage=new LinearLayout(this);homePage.setOrientation(LinearLayout.VERTICAL);root.addView(homePage,new LinearLayout.LayoutParams(-1,0,1));
        settingsPage=new LinearLayout(this);settingsPage.setOrientation(LinearLayout.VERTICAL);root.addView(settingsPage,new LinearLayout.LayoutParams(-1,-2));settingsPage.setVisibility(View.GONE);
        content=homePage;
        toolbar(false);
        label(content,"YOUR VOICE, AMPLIFIED",11,accent,true);
        LinearLayout live=section("LIVE MICROPHONE");
        ImageView microphone=new ImageView(this);microphone.setImageResource(R.drawable.ic_mic);microphone.setImageTintList(ColorStateList.valueOf(accent));
        microphone.setPadding(dp(14),dp(14),dp(14),dp(14));microphone.setBackground(surface(Color.rgb(24,49,47),32));
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(64),dp(64));iconParams.gravity=Gravity.CENTER_HORIZONTAL;iconParams.setMargins(0,dp(8),0,dp(6));live.addView(microphone,iconParams);
        state=label(live,"Ready when you are",23,Color.WHITE,true);state.setGravity(Gravity.CENTER);
        modeNote=label(live,"Built-in microphone → Bluetooth speaker",12,muted,false);modeNote.setGravity(Gravity.CENTER);
        meter=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);meter.setMax(100);meter.setProgressTintList(ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(4));mp.setMargins(0,dp(12),0,dp(12));live.addView(meter,mp);
        start=button(live,"Start microphone",v->toggle());start.setBackground(surface(accent,16));start.setTextColor(bg);
        talk=new TalkButton(this);talk.setText(R.string.hold_to_talk);talk.setAllCaps(false);talk.setTextSize(15);talk.setTextColor(accent);talk.setBackground(surface(Color.rgb(24,49,47),16));
        live.addView(talk,new LinearLayout.LayoutParams(-1,dp(56)));
        talk.setContentDescription("Hold to talk. With accessibility controls, activate to toggle speaking, activate again to mute.");

        LinearLayout output=section("OUTPUT");
        LinearLayout routeRow=new LinearLayout(this);routeRow.setGravity(Gravity.CENTER_VERTICAL);output.addView(routeRow);
        devices=new Spinner(this);routeRow.addView(devices,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView pair=label(routeRow,"Pair ↗",13,accent,true);pair.setGravity(Gravity.CENTER);pair.setMinWidth(dp(64));pair.setMinHeight(dp(48));pair.setContentDescription("Pair a speaker in Bluetooth settings");pair.setBackground(selectableBackground());pair.setFocusable(true);pair.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        deviceNote=label(output,"",12,muted,false);
        slider(output,"Output volume",100,Math.round(volume*100),"%",p->{volume=p/100f;publish();});
        label(content,"For a cleaner sound, start at low speaker volume.",12,muted,false).setGravity(Gravity.CENTER);
        View spacer=new View(this);homePage.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        TextView creatorLink=label(homePage,"Created by "+getString(R.string.creator_name)+"  ↗",13,muted,false);
        creatorLink.setGravity(Gravity.CENTER);creatorLink.setMinHeight(dp(48));creatorLink.setOnClickListener(v->showCreator());creatorLink.setContentDescription("Created by Ridhwan S. Open creator profile");
        creatorLink.setFocusable(true);creatorLink.setBackground(selectableBackground());

        content=settingsPage;
        toolbar(true);
        label(content,"Fine-tune your microphone. Changes save automatically.",13,muted,false);
        LinearLayout controls=section("MICROPHONE & PLAYBACK");
        CheckBox holdSwitch=check(controls,"Hold to talk",holdToTalk);
        holdSwitch.setOnCheckedChangeListener((b,checked)->{holdToTalk=checked;MicService.talkPressed=false;publish();});
        label(controls,"Mute on release or when you leave the app. Useful with a nearby speaker.",12,muted,false);
        protectionSwitch=check(controls,"Feedback protection",protection);
        protectionSwitch.setOnCheckedChangeListener((b,checked)->{protection=checked;publish();});
        protectionNote=label(controls,"",12,muted,false);
        CheckBox resumeSwitch=check(controls,"Resume after interruptions",resumeAfterInterruption);
        resumeSwitch.setOnCheckedChangeListener((b,checked)->{resumeAfterInterruption=checked;publish();});
        label(controls,"Resumes when Android returns audio focus. Stop cancels automatic resume.",12,muted,false);
        button(controls,"Notification controls",v->showNotificationControls());
        button(controls,"Refresh Bluetooth devices",v->refreshDevices());
        button(controls,"Stop microphone",v->{if(MicService.running)startService(new Intent(this,MicService.class).setAction("STOP"));showPage(false);});

        LinearLayout eq=section("VOCAL EQUALIZER");
        label(eq,"A little less rumble. A little more clarity.",13,muted,false);
        String[] names={"100 Hz · Low","400 Hz · Warmth","1 kHz · Voice","4 kHz · Presence","10 kHz · Air"};
        for(int i=0;i<5;i++) {final int index=i;slider(eq,names[i],24,Math.round(bands[i])+12,"dB",p->{bands[index]=p-12;publish();});}
        LinearLayout effects=section("VOICE EFFECTS");
        label(effects,"Creative effects for your voice. Leave echo off for clearer speech near a speaker.",12,muted,false);
        slider(effects,"Bass boost",12,Math.round(bass),"dB+",p->{bass=p;publish();});
        slider(effects,"Echo effect",65,Math.round(echo*100),"%",p->{echo=p/100f;publish();});
        slider(effects,"Effect repeat delay",520,delay-80,"ms",p->{delay=p+80;publish();});
        button(effects,"Reset to vocal preset",v->{int[] values={25,6,9,12,13,10,0,0,140};for(int i=0;i<sliders.size();i++)sliders.get(i).setProgress(values[i]);protectionSwitch.setChecked(true);});
        label(content,"Audio stays on your devices. Nothing is recorded or uploaded. Bluetooth delay and speaker feedback depend on your setup.",12,muted,false);
        creatorCard();
        audio.registerAudioDeviceCallback(callback,handler);
        refreshDevices();
        showPage(saved!=null && saved.getBoolean("settingsPage",false));
    }
    private void creatorCard() {
        LinearLayout box=section("MEET THE CREATOR");
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);box.addView(row);
        ImageView portrait=new ImageView(this);portrait.setImageResource(R.drawable.creator_portrait);
        portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);portrait.setContentDescription(getString(R.string.creator_portrait_description));
        GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(20));
        portrait.setBackground(shape);portrait.setClipToOutline(true);
        row.addView(portrait,new LinearLayout.LayoutParams(dp(88),dp(108)));
        LinearLayout credit=new LinearLayout(this);credit.setOrientation(LinearLayout.VERTICAL);credit.setPadding(dp(16),0,0,0);
        row.addView(credit,new LinearLayout.LayoutParams(0,-2,1));
        label(credit,"Designed & built by",12,muted,false);
        label(credit,getString(R.string.creator_name),22,Color.WHITE,true);
        label(credit,"The person behind Phone Mic.",13,muted,false);
        button(box,"GitHub · ridhwansalim ↗",v->openProfile("https://github.com/ridhwansalim"));
        button(box,"Instagram · ridhwan_salim ↗",v->openProfile("https://www.instagram.com/ridhwan_salim/"));
        button(box,"LinkedIn · Ridhwan S. ↗",v->openProfile("https://www.linkedin.com/in/ridhwan-s/"));
        portrait.setOnClickListener(v->showCreator());
        portrait.setFocusable(true);
    }
    private void showCreator() {
        ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(12),dp(24),dp(24));scroll.addView(body);
        ImageView photo=new ImageView(this);photo.setImageResource(R.drawable.creator_portrait);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setContentDescription(getString(R.string.creator_portrait_description));
        photo.setBackground(surface(card,24));photo.setClipToOutline(true);body.addView(photo,new LinearLayout.LayoutParams(-1,dp(240)));
        label(body,"Designed & built by "+getString(R.string.creator_name),20,Color.WHITE,true);
        label(body,"The person behind Phone Mic.",14,muted,false);
        button(body,"GitHub ↗",v->openProfile("https://github.com/ridhwansalim"));
        button(body,"Instagram ↗",v->openProfile("https://www.instagram.com/ridhwan_salim/"));
        button(body,"LinkedIn ↗",v->openProfile("https://www.linkedin.com/in/ridhwan-s/"));
        new AlertDialog.Builder(this).setTitle("Meet the creator").setView(scroll).setPositiveButton("Close",null).show();
    }
    private GradientDrawable surface(int color,int radius) {
        GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(radius));return drawable;
    }
    private android.graphics.drawable.Drawable selectableBackground() {
        android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,value,true);return getDrawable(value.resourceId);
    }
    private void toolbar(boolean settings) {
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);content.addView(bar);
        if(settings)iconButton(bar,R.drawable.ic_back,"Back to microphone",v->showPage(false));
        TextView title=new TextView(this);title.setText(settings?"Settings":"Phone Mic");title.setTextColor(Color.WHITE);title.setTextSize(26);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));title.setGravity(Gravity.CENTER_VERTICAL);
        if(!settings)iconButton(bar,R.drawable.ic_settings,"Open settings",v->showPage(true));
    }
    private void iconButton(LinearLayout parent,int icon,String description,View.OnClickListener listener) {
        ImageButton b=new ImageButton(this);b.setImageResource(icon);b.setContentDescription(description);b.setBackground(selectableBackground());b.setOnClickListener(listener);parent.addView(b,new LinearLayout.LayoutParams(dp(48),dp(48)));
    }
    private void openProfile(String url) {
        try {startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}
        catch(ActivityNotFoundException e) {Toast.makeText(this,"Install a browser to open this profile.",Toast.LENGTH_LONG).show();}
    }
    private void showPage(boolean settings) {
        MicService.talkPressed=false;showingSettings=settings;
        homePage.setVisibility(settings?View.GONE:View.VISIBLE);settingsPage.setVisibility(settings?View.VISIBLE:View.GONE);
        pageScroll.scrollTo(0,0);
    }
    @Override public void onBackPressed() {if(showingSettings)showPage(false);else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out) {out.putBoolean("settingsPage",showingSettings);super.onSaveInstanceState(out);}
    private void toggle() {
        if(MicService.running) {startService(new Intent(this,MicService.class).setAction("STOP"));return;}
        List<String> missing=new ArrayList<>();
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO);
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH_CONNECT);
        if(!missing.isEmpty()) {requestPermissions(missing.toArray(new String[0]),10);return;}
        if(outputs.isEmpty() || devices.getSelectedItemPosition()<0) {Toast.makeText(this,"Connect a Bluetooth speaker in Settings first.",Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notificationAsked",false)) {
            getPreferences(0).edit().putBoolean("notificationAsked",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11); return;
        }
        int id=outputs.get(devices.getSelectedItemPosition()).getId();
        begin(id);
    }
    private void begin(int id) {
        try {startForegroundService(new Intent(this,MicService.class).putExtra("device",id));}
        catch(Exception e) {MicService.status="Could not start microphone: "+e.getMessage();}
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==11) {toggle();return;}
        for(int result:results) if(result!=PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle("Microphone access needed")
                .setMessage("Allow Microphone and Nearby devices in app settings to send your voice to a Bluetooth speaker.")
                .setPositiveButton("App settings",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("Cancel",null).show();return;
        }
        refreshDevices(); toggle();
    }
    private void refreshDevices() {
        int previous=devices.getSelectedItemPosition();
        int id=previous>=0 && previous<outputs.size()? outputs.get(previous).getId():-1;
        if(MicService.running) id=MicService.selectedOutputId;
        outputs=MicService.outputs(audio);
        List<String> names=new ArrayList<>(); int selection=0;
        for(int i=0;i<outputs.size();i++) {names.add(outputs.get(i).getProductName().toString());if(outputs.get(i).getId()==id)selection=i;}
        if(names.isEmpty()) names.add("No Bluetooth speaker connected");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devices.setAdapter(adapter);devices.setSelection(selection);
        deviceNote.setText(outputs.isEmpty()?"Pair a speaker, then return here.":"Use your phone’s volume keys for speaker volume.");
    }
    private void load() {
        // Retired preferences must not re-enable calibration after an upgrade.
        getPreferences(0).edit().remove("speakerCancellation").remove("calibrationGainDb")
            .remove("calibrationOffsetMs").remove("extendedCalibration").apply();
        if(MicService.running) {AudioSettings s=MicService.settings; volume=s.volume;bass=s.bass;echo=s.echo;delay=s.delayMs;protection=s.protection;holdToTalk=s.holdToTalk;resumeAfterInterruption=s.resumeAfterInterruption;System.arraycopy(s.bands,0,bands,0,5);return;}
        android.content.SharedPreferences p=getPreferences(0);
        AudioSettings vocal=AudioSettings.vocal();
        if(p.getInt("presetVersion",0)<2) {
            volume=vocal.volume;bass=vocal.bass;echo=vocal.echo;delay=vocal.delayMs;System.arraycopy(vocal.bands,0,bands,0,5);
            p.edit().putInt("presetVersion",2).apply();
            Toast.makeText(this,"Vocal preset applied: echo off, bass boost off, feedback protection on.",Toast.LENGTH_LONG).show();
        } else {
            volume=p.getFloat("volume",vocal.volume);bass=p.getFloat("bass",0);echo=p.getFloat("echo",0);delay=p.getInt("delay",220);
            for(int i=0;i<5;i++)bands[i]=p.getFloat("band"+i,vocal.bands[i]);
        }
        protection=p.getBoolean("protection",true);holdToTalk=p.getBoolean("holdToTalk",false);resumeAfterInterruption=p.getBoolean("resumeAfterInterruption",true);
        publish();
    }
    private void publish() {
        MicService.settings=new AudioSettings(volume,bass,echo,delay,bands,protection,holdToTalk,resumeAfterInterruption);
        android.content.SharedPreferences.Editor p=getPreferences(0).edit().putFloat("volume",volume).putFloat("bass",bass).putFloat("echo",echo).putInt("delay",delay)
            .putBoolean("protection",protection).putBoolean("holdToTalk",holdToTalk).putBoolean("resumeAfterInterruption",resumeAfterInterruption);
        for(int i=0;i<5;i++)p.putFloat("band"+i,bands[i]);p.apply();
    }
    private interface Changed {void set(int progress);}
    private void slider(LinearLayout parent,String title,int max,int initial,String unit,Changed changed) {
        TextView caption=label(parent,"",14,Color.WHITE,false);
        SeekBar seek=new SeekBar(this);seek.setMax(max);seek.setProgress(initial);seek.setProgressTintList(ColorStateList.valueOf(accent));seek.setThumbTintList(ColorStateList.valueOf(accent));
        seek.setContentDescription(title);parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(44)));sliders.add(seek);
        java.util.function.IntConsumer render=p->{int value=unit.equals("dB")?p-12:unit.equals("ms")?p+80:p;caption.setText(getString(R.string.slider_caption,title,unit.equals("dB+")?"+":"",value,unit.replace("+","")));};
        render.accept(initial);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int p,boolean user) {render.accept(p);changed.set(p);}
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        });
    }
    private LinearLayout section(String title) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(20));box.setBackground(shape);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(20),0,0);content.addView(box,lp);
        label(box,title,11,accent,true);return box;
    }
    private TextView label(LinearLayout parent,String text,int size,int color,boolean bold) {
        TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(6),0,dp(6));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);parent.addView(v);return v;
    }
    private Button button(LinearLayout parent,String text,View.OnClickListener listener) {
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setOnClickListener(listener);b.setTextColor(Color.rgb(224,235,244));b.setBackground(surface(Color.rgb(31,43,57),14));b.setStateListAnimator(null);b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setMinHeight(dp(50));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,0);parent.addView(b,lp);return b;
    }
    private CheckBox check(LinearLayout parent,String text,boolean checked) {
        CheckBox box=new CheckBox(this);box.setText(text);box.setTextColor(Color.WHITE);box.setButtonTintList(ColorStateList.valueOf(accent));box.setChecked(checked);box.setMinHeight(dp(52));parent.addView(box);return box;
    }
    private void showNotificationControls() {
        if(!getSystemService(NotificationManager.class).areNotificationsEnabled()) {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));return;
        }
        if(MicService.running)startService(new Intent(this,MicService.class).setAction("SHOW_CONTROLS"));
        else Toast.makeText(this,"Go live to show microphone controls.",Toast.LENGTH_SHORT).show();
    }
    private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onResume() {super.onResume();refreshDevices();if(MicService.running)startService(new Intent(this,MicService.class).setAction("SHOW_CONTROLS"));handler.post(update);}
    @Override protected void onPause() {MicService.talkPressed=false;handler.removeCallbacks(update);super.onPause();}
    @Override protected void onDestroy() {audio.unregisterAudioDeviceCallback(callback);super.onDestroy();}
}

