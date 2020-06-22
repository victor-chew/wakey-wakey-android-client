package org.randseq.wakeywakey;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.randseq.wakeywakey.alarm.AlarmList;
import org.randseq.wakeywakey.service.ReplicationReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    final String dbHost = "node10.vpslinker.com:6984";
    final String connector = "https://wakey.randseq.org/php/request.php";
    EditText editListCode, editListName, editListPin, editTags;
    ImageButton btnRunStop;
    boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar)findViewById(R.id.appToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("dbHost", dbHost);
        prefsEditor.putString("connector", connector);
        prefsEditor.putString("lastError", "");
        prefsEditor.putLong("lastErrorTime", 0);
        prefsEditor.commit();

        // Get background replication running
        Intent intent = new Intent(getApplicationContext(), ReplicationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 30*1000, pendingIntent);

        editListCode = (EditText) findViewById(R.id.editListCode);
        editListName = (EditText) findViewById(R.id.editListName);
        editListPin = (EditText) findViewById(R.id.editListPin);
        editTags = (EditText)findViewById(R.id.editTags);
        btnRunStop = (ImageButton)findViewById(R.id.btnRunStop);

        editListCode.setText("");
        editListName.setText(prefs.getString("listName", ""));
        editListPin.setText(prefs.getString("listPin", ""));
        editTags.setText(prefs.getString("tags", ""));

        MainActivityTextWatcher textWatcher = new MainActivityTextWatcher(this);
        editListCode.addTextChangedListener(textWatcher);
        editListName.addTextChangedListener(textWatcher);
        editListPin.addTextChangedListener(textWatcher);

        boolean previouslyStarted = prefs.getBoolean("started", false);
        if (previouslyStarted) toggle();
    }

    public void onRunStop(View v) {
        toggle();
    }

    public void onViewLog(View v) {
        viewLog();
    }

    public void viewLog() {
        Intent intent = new Intent(this, ViewLogActivity.class);
        startActivity(intent);
    }

    public void toggle() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();

        String listCode = editListCode.getText().toString().trim();
        String listName = editListName.getText().toString().trim();
        String listPin = editListPin.getText().toString().trim();
        String tags = editTags.getText().toString().trim();

        prefsEditor.putString("tags", tags);
        prefsEditor.commit();

        if (!started) {
            if (listCode.length() == 0 && (listName.length() == 0 || listPin.length() == 0)) {
                Toast.makeText(getApplicationContext(),
                    R.string.error_listname_pin, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        started = !started;
        prefsEditor.putBoolean("started", started);
        prefsEditor.commit();

        editListCode.setEnabled(!started);
        editListName.setEnabled(!started);
        editListPin.setEnabled(!started);
        editTags.setEnabled(!started);
        btnRunStop.setImageResource(started ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        if (started) {

            AlarmList oldAlarms = new AlarmList(this);
            oldAlarms.initFromPrefs();
            oldAlarms.cancelAll();

            AlarmList newAlarms = new AlarmList(this);
            newAlarms.initFromDb();
            newAlarms.scheduleAll();

            String oldListName = prefs.getString("listName", "");
            String oldListPin = prefs.getString("listPin", "");

            if (listCode.length() > 0) {
                prefsEditor.putString("listCode", listCode);
                prefsEditor.commit();
                new DatabaseSetup(this, oldListName, oldListPin).execute(prefs);
            } else {
                prefsEditor.putString("listName", listName);
                prefsEditor.putString("listPin", listPin);
                prefsEditor.putString("dbName", "");
                prefsEditor.putString("dbUser", "");
                prefsEditor.putString("dbPass", "");
                prefsEditor.commit();
                new DatabaseSetup(this, oldListName, oldListPin).execute(prefs);
            }
        }
    }

    public void stop() {
        if (started) toggle();
    }

    public void started() {
        String listCode = editListCode.getText().toString().trim();
        if (listCode.length() > 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.remove("listCode");
            prefsEditor.commit();
            editListCode.setText("");
            editListName.setText(prefs.getString("listName", ""));
            editListPin.setText(prefs.getString("listPin", ""));
        }
    }
}

class MainActivityTextWatcher implements TextWatcher {

    private MainActivity mainActivity;

    public MainActivityTextWatcher(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!mainActivity.started) {
            if (mainActivity.editListCode.getText().length() > 0) {
                mainActivity.editListName.setEnabled(false);
                mainActivity.editListPin.setEnabled(false);
            } else {
                mainActivity.editListName.setEnabled(true);
                mainActivity.editListPin.setEnabled(true);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
}

class DatabaseSetup extends AsyncTask<SharedPreferences, Void, Boolean> {
    MainActivity mainActivity;
    String oldListName, oldListPin;

    public DatabaseSetup(MainActivity mainActivity, String oldListName, String oldListPin) {
        this.mainActivity = mainActivity;
        this.oldListName = oldListName;
        this.oldListPin = oldListPin;
    }

    @Override
    protected Boolean doInBackground(SharedPreferences... params) {
        SharedPreferences prefs = params[0];
        SharedPreferences.Editor prefsEditor = prefs.edit();
        try {
            String connector = prefs.getString("connector", "");
            String listCode = prefs.getString("listCode", "");
            String listName = prefs.getString("listName", "");
            String listPin = prefs.getString("listPin", "");

            if (listCode.length() > 0) {
                JsonObject json = sendRequest(connector, "redeem-code", listCode);
                listName = json.get("listName").getAsString();
                listPin = json.get("listPin").getAsString();
                prefsEditor.putString("listName", listName);
                prefsEditor.putString("listPin", listPin);
                prefsEditor.commit();
            }

            String dbName = prefs.getString("dbName", "");
            String dbUser = prefs.getString("dbUser", "");
            String dbPass = prefs.getString("dbPass", "");
            boolean listChanged = !oldListName.equals(listName) || !oldListPin.equals(listPin);
            boolean dbInfoMissing = dbName.length() == 0 || dbUser.length() == 0 || dbPass.length() == 0;

            if (listChanged || dbInfoMissing) {
                // Clear out old couchdb first, otherwise sync will end up with previous documents
                File path = mainActivity.getDir("documentstores", Context.MODE_PRIVATE);
                mainActivity.deleteDatabase("alarms");

                JsonObject json = sendRequest(connector, "open", listName, listPin);
                prefsEditor.putString("dbName", json.get("dbName").getAsString());
                prefsEditor.putString("dbUser", json.get("dbUser").getAsString());
                prefsEditor.putString("dbPass", json.get("dbPass").getAsString());
                prefsEditor.commit();
            }

            return true;

        } catch(Exception e) {

            e.printStackTrace();

            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            prefsEditor.putString("lastError", stackTrace.toString());
            prefsEditor.putLong("lastErrorTime", System.currentTimeMillis());
            prefsEditor.commit();

            return false;
        }
    }

    JsonObject sendRequest(String connector, String cmd, String code) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("cmd", cmd);
        params.put("code", code);
        return sendRequest(connector, params);
    }

    JsonObject sendRequest(String connector, String cmd, String name, String pin) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("cmd", cmd);
        params.put("name", name);
        params.put("pin", pin);
        return sendRequest(connector, params);
    }

    JsonObject sendRequest(String connector, Map<String, String> params) throws IOException {
        StringBuffer json = new StringBuffer("{");
        for(String key: params.keySet()) {
            String value = params.get(key);
            json.append("\""); json.append(key); json.append("\":");
            json.append("\""); json.append(value); json.append("\",");
        }
        json.setLength(json.length() - 1);
        json.append("}");

        URL url = new URL(connector);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.connect();

        OutputStream os = conn.getOutputStream();
        byte[] input = json.toString().getBytes("utf-8");
        os.write(input, 0, input.length);
        os.flush();
        os.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder reply = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            reply.append(line.trim());
        }

        conn.disconnect();

        JsonObject result = new JsonParser().parse(reply.toString()).getAsJsonObject();
        if (!result.get("status").getAsString().equals("ok"))
            throw new IOException(result.get("statusText").getAsString());

        return result;
    }

    @Override
    protected void onPostExecute(Boolean ok) {
        if (ok.booleanValue()) mainActivity.started();
        else {
            mainActivity.stop();
            mainActivity.viewLog();
        }
    }
}

