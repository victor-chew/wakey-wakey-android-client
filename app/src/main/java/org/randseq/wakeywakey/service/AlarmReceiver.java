package org.randseq.wakeywakey.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.randseq.wakeywakey.alarm.Alarm;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        final Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(Alarm.TAG, bundle.getByteArray(Alarm.TAG));
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startWakefulService(context, serviceIntent);
    }
}
