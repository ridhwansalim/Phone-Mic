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
    private final int bg=Color.rgb(12,18,24), card=Color.rgb(23,33,43), accent=Color.rgb(101,225,184), muted=Color.rgb(157,175,190);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private LinearLayout content;
    private LinearLayout homePage, settingsPage;
    private ScrollView pageScroll;
    private boolean showingSettings;
    private TextView state, deviceNote, protectionNote;
    private Spinner devices;
    private Button start, talk;
    private ProgressBar meter;
    private List<AudioDeviceInfo> outputs=new ArrayList<>();
    private final float[] bands=new float[5];
    private float volume=.35f, bass=0, echo=0;
    private int delay=220;
    private boolean protection=true, holdToTalk=false, resumeAfterInterruption=true;
    private CheckBox protectionSwitch;
    private CheckBox speakerSwitch;
    private boolean speakerCancellation=true;
    private final List<SeekBar> sliders=new ArrayList<>();
    private final AudioDeviceCallback callback=new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] d) {refreshDevices();}
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] d) {refreshDevices();}
    };
    private final Runnable update=new Runnable() {
        @Override public void run() {
            state.setText(MicService.status);
            start.setText(MicService.running ? (MicService.calibrating?"■   Stop calibration":"■   Stop microphone") : "●   Go live");
            speakerSwitch.setEnabled(!MicService.running);
            devices.setEnabled(!MicService.running);
            meter.setProgress(Math.round(MicService.level*100));
            talk.setVisibility(holdToTalk?View.VISIBLE:View.GONE);
            talk.setEnabled(MicService.running && !MicService.paused && !MicService.calibrating);
            talk.setText(MicService.talkPressed?"Speaking · release to mute":"Hold to talk");
            protectionNote.setText(MicService.calibrating?MicService.echoCancellation:!protection?"Feedback protection is off\n"+MicService.echoCancellation:MicService.feedbackReduced?
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
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(24),dp(24),dp(24),dp(32));
        scroll.addView(content); setContentView(scroll);
        pageScroll=scroll;
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout root=content;
        homePage=new LinearLayout(this);homePage.setOrientation(LinearLayout.VERTICAL);root.addView(homePage);
        settingsPage=new LinearLayout(this);settingsPage.setOrientation(LinearLayout.VERTICAL);root.addView(settingsPage);settingsPage.setVisibility(View.GONE);
        content=homePage;
        button(content,"Settings",v->showPage(true));
        label(content,"PHONE MIC  /  LIVE AUDIO",12,accent,true);
        label(content,"Your voice.\nA little louder.",34,Color.WHITE,true);
        label(content,"Turn your phone into a microphone for your Bluetooth speaker.",15,muted,false);

        LinearLayout live=section("MICROPHONE");
        state=label(live,MicService.status,18,Color.WHITE,true);
        content=settingsPage;
        button(content,"← Back to microphone",v->showPage(false));
        label(content,"Settings",30,Color.WHITE,true);
        label(content,"Make it sound like you. Changes save automatically.",14,muted,false);
        button(content,"Stop microphone",v->{if(MicService.running)startService(new Intent(this,MicService.class).setAction("STOP"));showPage(false);});
        LinearLayout controls=section("MICROPHONE & PLAYBACK");
        speakerSwitch=check(controls,"Cancel speaker echo · calibrate before going live",speakerCancellation);
        speakerSwitch.setOnCheckedChangeListener((b,checked)->{speakerCancellation=checked;getPreferences(0).edit().putBoolean("speakerCancellation",checked).apply();});
        label(controls,"Plays a soft test sound for about 8–12 seconds. Keep quiet and keep the phone and speaker still. Recalibrate after moving them or changing speaker volume. Cancels this app’s playback, not other music.",12,muted,false);
        meter=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); meter.setMax(100); meter.setProgressTintList(ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(8)); mp.setMargins(0,dp(14),0,dp(16)); live.addView(meter,mp);
        start=button(live,"●   Go live",v->toggle()); start.setBackgroundTintList(ColorStateList.valueOf(accent)); start.setTextColor(bg);
        talk=new TalkButton(this);talk.setText(R.string.hold_to_talk);talk.setAllCaps(false);talk.setTextSize(15);
        live.addView(talk,new LinearLayout.LayoutParams(-1,dp(54)));
        talk.setContentDescription("Hold to talk. With accessibility controls, activate to toggle speaking, activate again to mute.");
        CheckBox holdSwitch=check(controls,"Hold to talk for a nearby speaker",holdToTalk);
        holdSwitch.setOnCheckedChangeListener((b,checked)->{holdToTalk=checked;MicService.talkPressed=false;publish();});
        label(controls,"Hold-to-talk mutes when released or when the app leaves the screen. Bluetooth may still play sound already buffered.",12,muted,false);
        protectionSwitch=check(controls,"Feedback protection",protection);
        protectionSwitch.setOnCheckedChangeListener((b,checked)->{protection=checked;publish();});
        protectionNote=label(controls,"",12,muted,false);
        CheckBox resumeSwitch=check(controls,"Resume after calls / temporary interruptions",resumeAfterInterruption);
        resumeSwitch.setOnCheckedChangeListener((b,checked)->{resumeAfterInterruption=checked;publish();});
        label(controls,"When enabled, the mic becomes live again when Android returns audio focus. Stop always cancels automatic resume.",12,muted,false);
        button(controls,"Show notification controls",v->showNotificationControls());
        label(live,"Speak close to the phone. Point the speaker away from it and start at low speaker volume. Protection reduces risk; it cannot guarantee feedback-free Bluetooth audio.",12,muted,false);

        content=homePage;
        LinearLayout output=section("BLUETOOTH OUTPUT");
        devices=new Spinner(this); output.addView(devices,new LinearLayout.LayoutParams(-1,dp(52)));
        deviceNote=label(output,"",13,muted,false);
        button(output,"Connect a speaker  ↗",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        button(controls,"Refresh Bluetooth devices",v->refreshDevices());
        slider(output,"Output volume",100,Math.round(volume*100),"%",p->{volume=p/100f;publish();});
        label(output,"This sets the app’s level. Your phone’s volume buttons control media volume.",12,muted,false);

        creatorCard();
        content=settingsPage;
        LinearLayout eq=section("SHAPE YOUR SOUND");
        label(eq,"Equalizer",22,Color.WHITE,true);
        label(eq,"Vocal preset: less rumble and muddiness, gentle voice presence. A starting point you can adjust for your voice and speaker.",13,muted,false);
        String[] names={"100 Hz · Low","400 Hz · Warmth","1 kHz · Voice","4 kHz · Presence","10 kHz · Air"};
        for(int i=0;i<5;i++) {final int index=i; slider(eq,names[i],24,Math.round(bands[i])+12,"dB",p->{bands[index]=p-12;publish();});}
        LinearLayout effects=section("ADD SOME ATMOSPHERE");
        slider(effects,"Bass boost",12,Math.round(bass),"dB+",p->{bass=p;publish();});
        slider(effects,"Echo",65,Math.round(echo*100),"%",p->{echo=p/100f;publish();});
        slider(effects,"Echo delay",520,delay-80,"ms",p->{delay=p+80;publish();});
        button(effects,"Apply vocal preset",v->{int[] values={25,6,9,12,13,10,0,0,140}; for(int i=0;i<sliders.size();i++)sliders.get(i).setProgress(values[i]);protectionSwitch.setChecked(true);});
        label(content,"Bluetooth adds a delay between speaking and playback. The amount depends on your phone and speaker. Audio stays on your devices; nothing is recorded or uploaded.",13,muted,false);
        button(content,"Open-source audio library",v-> {
            try(java.io.InputStream input=getAssets().open("SpeexDSP-LICENSE.txt")) {
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;
                while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
                new AlertDialog.Builder(this).setTitle("SpeexDSP 1.2.1 · Xiph.Org").setMessage(bytes.toString("UTF-8")).setPositiveButton("Close",null).show();
            } catch(java.io.IOException e){Toast.makeText(this,"License could not be opened",Toast.LENGTH_SHORT).show();}
        });
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
        box.setOnClickListener(v->{
            ImageView photo=new ImageView(this);photo.setImageResource(R.drawable.creator_portrait);photo.setAdjustViewBounds(true);
            ScrollView photoScroll=new ScrollView(this);photoScroll.addView(photo);
            new AlertDialog.Builder(this).setTitle(getString(R.string.creator_name)).setView(photoScroll).setPositiveButton("Close",null).show();
        });
        box.setContentDescription("About the creator. Tap to view photo.");
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
        if(speakerCancellation) {
            new AlertDialog.Builder(this).setTitle("Calibrate speaker echo")
                .setMessage("Set the speaker to a low, comfortable volume. Keep quiet for about 8–12 seconds while a soft test sound plays. Microphone playback begins automatically after calibration succeeds.\n\nKeep the phone and speaker in their intended positions. Stop and recalibrate after a significant move or volume change.")
                .setPositiveButton("Calibrate and go live",(d,w)->begin(id,true)).setNegativeButton("Cancel",null).show();
        } else begin(id,false);
    }
    private void begin(int id,boolean cancelSpeaker) {
        try {startForegroundService(new Intent(this,MicService.class).putExtra("device",id).putExtra("speakerCancellation",cancelSpeaker));}
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
        deviceNote.setText(outputs.isEmpty()?"Pair and connect a speaker for media audio, then return here.":"Choose a connected media audio device. Stop the microphone before switching.");
    }
    private void load() {
        speakerCancellation=MicService.running?MicService.speakerMode:getPreferences(0).getBoolean("speakerCancellation",true);
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
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setOnClickListener(listener);
        parent.addView(b,new LinearLayout.LayoutParams(-1,dp(54)));return b;
    }
    private CheckBox check(LinearLayout parent,String text,boolean checked) {
        CheckBox box=new CheckBox(this);box.setText(text);box.setTextColor(Color.WHITE);box.setButtonTintList(ColorStateList.valueOf(accent));box.setChecked(checked);parent.addView(box);return box;
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

