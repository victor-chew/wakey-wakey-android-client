package org.randseq.wakeywakey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.randseq.wakeywakey.alarm.Alarm;

import it.justonetouch.util.ParcelableUtil;

public class AlarmAlertActivity extends Activity {

    private static final String TAG = "AlarmAlertActivity";
    private Alarm alarm;
    private MediaPlayer player = null;
    private Thread monitorThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON   |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.alert_activity);
        onNewIntent(getIntent());
    }

    private void startAlarm() {
        TextView alertMsg = (TextView)findViewById(R.id.textAlarmMsg);
        String msg = alarm.getMsg();
        if (msg.length() == 0) msg = "\u23f0";
        alertMsg.setText(msg);
        alertMsg.setOnClickListener(new ClickListener());
        try {
            AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            String path = "android.resource://" + getPackageName() + "/raw/" + alarm.getTone();
            Uri alarmSound = Uri. parse(path);
            player = new MediaPlayer();
            player.setDataSource(this, alarmSound);
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
            monitorForTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        stopPlaying();
        final Bundle bundle = intent.getExtras();
        if (bundle != null) {
            alarm = ParcelableUtil.unmarshall(bundle.getByteArray(Alarm.TAG), Alarm.CREATOR);
            if (alarm != null) startAlarm(); else finish();
        }
    }

    private void monitorForTermination() {
        monitorThread = new Thread() {
            public void run() {
                try {
                    long totalSleep = 0;
                    while (true) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences prefs = getApplicationContext().getSharedPreferences("alarms", Context.MODE_PRIVATE);
                                int intentId = prefs.getInt(alarm.getRevId(), 0);
                                if (intentId == 0) monitorThread.interrupt();
                            }
                        });
                        Thread.sleep(5*1000);
                        totalSleep += 5*1000;
                        if (totalSleep >= 10*60*1000) monitorThread.interrupt();
                    }
                } catch (InterruptedException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }
        };
        monitorThread.start();
    }

    public class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        stopPlaying();
        super.onDestroy();
    }

    void stopPlaying() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.reset();
                player.release();
                player = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
