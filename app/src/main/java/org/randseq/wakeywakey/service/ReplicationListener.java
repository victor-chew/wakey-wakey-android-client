package org.randseq.wakeywakey.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudant.sync.event.Subscribe;
import com.cloudant.sync.event.notifications.ReplicationCompleted;
import com.cloudant.sync.event.notifications.ReplicationErrored;

import org.randseq.wakeywakey.alarm.Alarm;
import org.randseq.wakeywakey.alarm.AlarmList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

public class ReplicationListener {

    private final Context context;

    public ReplicationListener(Context context) {
        this.context = context;
    }

    @Subscribe
    public void complete(ReplicationCompleted e) {
        AlarmList oldAlarms = new AlarmList(context);
        AlarmList newAlarms = new AlarmList(context);
        oldAlarms.initFromPrefs();
        newAlarms.initFromDb();
        newAlarms.migrateFrom(oldAlarms);
    }

    @Subscribe
    public void error(ReplicationErrored e) {
        StringWriter stackTrace = new StringWriter();
        e.errorInfo.printStackTrace(new PrintWriter(stackTrace));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("lastError", stackTrace.toString());
        prefs.edit().putLong("lastErrorTime", System.currentTimeMillis());
    }
}