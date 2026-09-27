package com.phonemic.app;

import android.app.*;
import android.content.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.*;
import java.util.*;

public final class MicService extends Service {
    public static volatile boolean running;
    public static volatile String status = "Ready when you are";
    public static volatile float level;
    public static volatile int selectedOutputId=-1;
    public static volatile boolean paused, talkPressed, feedbackReduced;
    public static volatile String echoCancellation="Echo cancellation: checked when live";
    public static volatile AudioSettings settings = AudioSettings.vocal();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean active;
    private volatile boolean routeConfirmed;
    private Thread worker;
    private AudioManager manager;
    private AudioFocusRequest focus;
    private PowerManager.WakeLock wakeLock;
    private int selectedId;
    private final SessionState session=new SessionState();
    private AudioAttributes attributes;
    private MediaSession mediaSession;
    private boolean destroyed;
    private String pendingAlert;
    private final AudioDeviceCallback devices = new AudioDeviceCallback() {
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
            for (AudioDeviceInfo d : removed) if (d.getId()==selectedId) end("Speaker disconnected. Reconnect to continue.",true);
        }
    };
    public static boolean bluetooth(AudioDeviceInfo d) {
        return d.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT>=31 && (d.getType()==AudioDeviceInfo.TYPE_BLE_SPEAKER || d.getType()==AudioDeviceInfo.TYPE_BLE_HEADSET));
    }
    public static List<AudioDeviceInfo> outputs(AudioManager manager) {
        List<AudioDeviceInfo> result = new ArrayList<>();
        for (AudioDeviceInfo d : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) if (bluetooth(d)) result.add(d);
        return result;
    }
    @Override public void onCreate() {
        super.onCreate();
        manager = getSystemService(AudioManager.class);
        manager.registerAudioDeviceCallback(devices, main);
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("live", "Live microphone", NotificationManager.IMPORTANCE_LOW));
        NotificationChannel alerts=new NotificationChannel("alerts","Microphone interruptions",NotificationManager.IMPORTANCE_DEFAULT);
        alerts.setSound(null,null); // A new alert must not feed sound back into the microphone.
        getSystemService(NotificationManager.class).createNotificationChannel(alerts);
        mediaSession=new MediaSession(this,"PhoneMic");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onStop() {end("Microphone stopped",false);}
            @Override public void onPause() {end("Microphone stopped",false);}
            @Override public void onCustomAction(String action,Bundle extras) {if("STOP_MIC".equals(action))end("Microphone stopped",false);}
        },main);
        mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Phone Mic · Live microphone").build());
        mediaSession.setActive(true);
    }
    private PendingIntent openApp() {
        return PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification liveNotification() {
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,MicService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(this,"live").setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(paused?"Phone Mic paused":"Phone Mic is active").setContentText(status)
            .setContentIntent(openApp()).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_stop),"Stop microphone",stop).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(0));
        if(Build.VERSION.SDK_INT>=31) builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }
    private void publishStatus(String message) {
        status=message;
        if(destroyed || !running) return;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_STOP | PlaybackState.ACTION_PAUSE)
            .addCustomAction("STOP_MIC","Stop microphone",R.drawable.ic_stop)
            .setState(paused?PlaybackState.STATE_PAUSED:PlaybackState.STATE_PLAYING,PlaybackState.PLAYBACK_POSITION_UNKNOWN,paused?0:1).build());
        getSystemService(NotificationManager.class).notify(1,liveNotification());
    }
    private void showAlert(String message) {
        NotificationManager notifications=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        if(!notifications.areNotificationsEnabled())return;
        notifications.notify(2,new Notification.Builder(this,"alerts").setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Phone Mic stopped").setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(openApp()).setAutoCancel(true).build());
    }
    // User-controlled continuous audio has no fixed duration. The foreground service
    // owns this lock and releases it in onDestroy on every stop/error path.
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent==null || "STOP".equals(intent.getAction())) { end("Microphone stopped",false); return START_NOT_STICKY; }
        if("SHOW_CONTROLS".equals(intent.getAction())) {
            if(running)publishStatus(status);else stopSelf();
            return START_NOT_STICKY;
        }
        if (running || worker!=null) return START_NOT_STICKY;
        selectedId=intent.getIntExtra("device",-1);
        selectedOutputId=selectedId;
        paused=false;talkPressed=false;feedbackReduced=false;status="Connecting to speaker…";
        try {
            Notification notification=liveNotification();
            if (Build.VERSION.SDK_INT>=30) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(1,notification);
            attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(this::focusChanged,main).build();
            if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) throw new IllegalStateException("Another app is using audio. Try again.");
            wakeLock=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PhoneMic:LiveAudio");
            session.start();running=true;getSystemService(NotificationManager.class).cancel(2);
            startWorker();
        } catch (Exception e) { end("Could not start: " + e.getMessage(),true); }
        return START_NOT_STICKY;
    }
    private void focusChanged(int change) {
        if(destroyed || session.get()==SessionState.State.STOPPED)return;
        if(change==AudioManager.AUDIOFOCUS_LOSS) {
            end("Audio taken by another app. Open Phone Mic to restart.",true);
        } else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            session.temporaryLoss(settings.resumeAfterInterruption);
            if(session.get()==SessionState.State.STOPPED) {end("Interrupted. Open Phone Mic to restart.",true);return;}
            active=false;paused=true;talkPressed=false;level=0;
            publishStatus("Paused for a call or other audio. Will resume when audio focus returns.");
        } else if(change==AudioManager.AUDIOFOCUS_GAIN && session.get()==SessionState.State.PAUSED) {
            if(!settings.resumeAfterInterruption) {end("Automatic resume is off. Open Phone Mic to restart.",true);return;}
            session.focusGained();paused=false;
            publishStatus("Audio restored. Reconnecting microphone…");
            if(worker==null)startWorker();
        }
    }
    @android.annotation.SuppressLint("WakelockTimeout")
    private void startWorker() {
        if(destroyed || session.get()!=SessionState.State.LIVE || worker!=null)return;
        routeConfirmed=false;active=true;paused=false;
        if(!wakeLock.isHeld())wakeLock.acquire(); // Continuous user-controlled foreground audio; released below.
        worker=new Thread(() -> stream(attributes),"PhoneMic-Audio");worker.start();
        publishStatus("Connecting to speaker…");
    }
    private void stream(AudioAttributes attrs) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        AudioRecord recorder=null; AudioTrack track=null;
        AcousticEchoCanceler canceller=null;
        String failure=null;
        try {
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("Microphone permission is required.");
            AudioDeviceInfo output=null, input=null;
            for(AudioDeviceInfo d:outputs(manager)) if(d.getId()==selectedId) output=d;
            for(AudioDeviceInfo d:manager.getDevices(AudioManager.GET_DEVICES_INPUTS)) if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC) {input=d; break;}
            if(output==null) throw new IllegalStateException("Connect a Bluetooth speaker first.");
            if(input==null) throw new IllegalStateException("Phone microphone is unavailable.");
            int rate=48000;
            int recordMin=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            int playMin=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(recordMin<=0 || playMin<=0) throw new IllegalStateException("48 kHz audio is unsupported on this phone.");
            recorder=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(Math.max(recordMin*2,3840)).build();
            track=new AudioTrack.Builder().setAudioAttributes(attrs)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(playMin,3840)).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED || track.getState()!=AudioTrack.STATE_INITIALIZED) throw new IllegalStateException("Audio could not be initialized.");
            try {
                if(AcousticEchoCanceler.isAvailable())canceller=AcousticEchoCanceler.create(recorder.getAudioSessionId());
            } catch(RuntimeException ignored) { /* Some devices advertise an effect they cannot create. */ }
            boolean effectEnabled=settings.protection;
            configureCanceller(canceller,effectEnabled);
            if(!recorder.setPreferredDevice(input) || !track.setPreferredDevice(output)) throw new IllegalStateException("Android could not select that audio route.");
            // Route changes mute immediately. Never intentionally fall back to the phone speaker.
            final AudioTrack liveTrack=track;
            track.addOnRoutingChangedListener(router -> {
                if(!active || !routeConfirmed) return;
                try {
                    AudioDeviceInfo routed=liveTrack.getRoutedDevice();
                    if(active && (routed==null || routed.getId()!=selectedId)) {
                        liveTrack.setVolume(0); end("Audio output changed. Select your Bluetooth speaker again.",true);
                    }
                } catch(IllegalStateException ignored) { /* Worker has already released the track. */ }
            },main);
            track.setVolume(0); track.play();
            short[] block=new short[480];
            // Feed silence until Android confirms the requested route, before starting capture.
            long deadline=SystemClock.elapsedRealtime()+4000;
            while(active && SystemClock.elapsedRealtime()<deadline) {
                if(track.write(block,0,block.length,AudioTrack.WRITE_BLOCKING)<0) throw new IllegalStateException("Speaker output failed.");
                AudioDeviceInfo routed=track.getRoutedDevice();
                if(routed!=null && routed.getId()==selectedId) break;
            }
            if(!active) return;
            if(track.getRoutedDevice()==null || track.getRoutedDevice().getId()!=selectedId) throw new IllegalStateException("Android did not route to the selected speaker. Choose it in Bluetooth settings.");
            routeConfirmed=true;
            recorder.startRecording();
            if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException("Microphone is busy or disabled.");
            track.setVolume(1);
            String liveStatus="Live · " + output.getProductName();
            main.post(() -> { if(active && !destroyed) publishStatus(liveStatus); });
            AudioProcessor processor=new AudioProcessor(rate);
            while(active) {
                int read=0;
                while(active && read<block.length) {
                    int count=recorder.read(block,read,block.length-read,AudioRecord.READ_BLOCKING);
                    if(count<=0)throw new IllegalStateException("Microphone disconnected or unavailable.");
                    read+=count;
                }
                if(!active)break;
                AudioDeviceInfo routed=track.getRoutedDevice(), source=recorder.getRoutedDevice();
                if(routed==null || routed.getId()!=selectedId || source==null || source.getType()!=AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                    track.setVolume(0); throw new IllegalStateException("Audio route changed. Reconnect and try again.");
                }
                AudioSettings snapshot=settings;
                boolean desiredEffect=snapshot.protection;
                if(desiredEffect!=effectEnabled) {effectEnabled=desiredEffect;configureCanceller(canceller,effectEnabled);}
                level=processor.process(block,read,snapshot,!snapshot.holdToTalk || talkPressed);
                boolean reduced=snapshot.protection && processor.protectionGain()<1;
                if(reduced!=feedbackReduced) {
                    feedbackReduced=reduced;
                    main.post(() -> {if(active && !destroyed)publishStatus(reduced?"Feedback tone detected · output reduced. Lower speaker volume.":liveStatus);});
                }
                int offset=0;
                while(active && offset<read) {
                    int wrote=track.write(block,offset,read-offset,AudioTrack.WRITE_BLOCKING);
                    if(wrote<=0) throw new IllegalStateException("Speaker playback failed.");
                    offset+=wrote;
                }
            }
        } catch(Exception e) { if(active) failure=e.getMessage(); }
        finally {
            active=false;
            if(canceller!=null)canceller.release();
            if(recorder!=null) { try {recorder.stop();} catch(Exception ignored) {} recorder.release(); }
            if(track!=null) { try {track.pause(); track.flush();} catch(Exception ignored) {} track.release(); }
            String message=failure;
            main.post(() -> {
                worker=null;routeConfirmed=false;
                if(wakeLock!=null && wakeLock.isHeld())wakeLock.release();
                if(destroyed) return;
                if(message!=null && session.get()==SessionState.State.LIVE)end(message,true);
                else if(session.get()==SessionState.State.LIVE)startWorker();
                else if(session.get()==SessionState.State.STOPPED)stopSelf();
            });
        }
    }
    private void configureCanceller(AcousticEchoCanceler effect,boolean enable) {
        if(effect==null) {echoCancellation="Phone echo cancellation unavailable · software protection still works";return;}
        try {
            boolean ok=effect.setEnabled(enable)==AudioEffect.SUCCESS;
            echoCancellation=ok && enable && effect.getEnabled()?"Phone echo cancellation enabled · Bluetooth effectiveness varies":
                enable?"Phone echo cancellation could not be enabled":"Phone echo cancellation off";
        } catch(RuntimeException e) {echoCancellation="Phone echo cancellation unavailable";}
    }
    private void end(String message,boolean alert) {
        if(destroyed)return;
        session.stop();active=false;paused=false;talkPressed=false;status=message;
        if(alert)pendingAlert=message;
        if(worker==null)stopSelf();
    }
    @Override public void onDestroy() {
        destroyed=true;session.stop();active=false; running=false;paused=false;talkPressed=false;level=0;
        manager.unregisterAudioDeviceCallback(devices);
        if(focus!=null) manager.abandonAudioFocusRequest(focus);
        if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        mediaSession.setActive(false);mediaSession.release();
        if(pendingAlert!=null)showAlert(pendingAlert);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) {return null;}
}
