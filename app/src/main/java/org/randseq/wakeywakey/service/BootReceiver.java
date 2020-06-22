package org.randseq.wakeywakey.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.randseq.wakeywakey.alarm.AlarmList;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
            intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))
        {
            Log.d("Wakey Wakey", "Boot completed");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean started = prefs.getBoolean("started", false);
            if (!started) return;

            // Get background replication running
            Intent intent2 = new Intent(context, ReplicationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 30*1000, pendingIntent);
        }
    }
}
