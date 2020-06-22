package org.randseq.wakeywakey.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.randseq.wakeywakey.alarm.Alarm;

public class ReplicationReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent serviceIntent = new Intent(context, ReplicationService.class);
        startWakefulService(context, serviceIntent);
    }
}
