package org.randseq.wakeywakey.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.randseq.wakeywakey.alarm.Alarm;
import org.randseq.wakeywakey.AlarmAlertActivity;

public class AlarmService extends IntentService {
    public AlarmService() {
        super("AlarmService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startAlarm(intent);
        AlarmReceiver.completeWakefulIntent(intent);
    }

    private void startAlarm(Intent intent) {
        final Intent serviceIntent = new Intent(this, AlarmAlertActivity.class);
        final Bundle bundle = intent.getExtras();
        serviceIntent.putExtra(Alarm.TAG, bundle.getByteArray(Alarm.TAG));
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(serviceIntent);
    }
}

