package org.randseq.wakeywakey.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cloudant.sync.documentstore.DocumentStore;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import org.randseq.wakeywakey.alarm.AlarmList;

import java.io.File;
import java.net.URI;

public class ReplicationService extends IntentService {
    public ReplicationService() {
        super("ReplicationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startReplication(intent);
        ReplicationReceiver.completeWakefulIntent(intent);
    }

    private void startReplication(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean started = prefs.getBoolean("started", false);
        if (!started) return;

        File path = getApplicationContext().getDir("documentstores", Context.MODE_PRIVATE);
        try {
            DocumentStore ds = DocumentStore.getInstance(new File(path, "alarms"));
            String dbUrl = getDbUri();
            if (dbUrl.length() > 0) {
                URI uri = new URI(getDbUri());
                Replicator replicator = ReplicatorBuilder.pull().from(uri).to(ds).build();
                replicator.getEventBus().register(new ReplicationListener(getApplicationContext()));
                replicator.start();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String getDbUri() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String dbHost = prefs.getString("dbHost", "");
        String dbName = prefs.getString("dbName", "");
        String dbUser = prefs.getString("dbUser", "");
        String dbPass = prefs.getString("dbPass", "");
        if (dbName.length() == 0 || dbUser.length() == 0 || dbPass.length() == 0) return "";
        return "https://" + dbUser + ":" + dbPass + "@" + dbHost + "/" + dbName;
    }
}
