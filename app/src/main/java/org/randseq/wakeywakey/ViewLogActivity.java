package org.randseq.wakeywakey;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.cloudant.sync.documentstore.DocumentRevision;
import com.cloudant.sync.documentstore.DocumentStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ViewLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_activity);
        TextView logText = (TextView)findViewById(R.id.logText);
        logText.setMovementMethod(new ScrollingMovementMethod());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        logText.append("<<< Most recent error >>>\n");
        String lastError = prefs.getString("lastError", "None");
        long lastErrorTime = prefs.getLong("lastErrorTime", 0);
        if (lastErrorTime > 0) {
            SimpleDateFormat formatter = new SimpleDateFormat();
            Date date = new Date();
            date.setTime(lastErrorTime);
            logText.append(formatter.format(date) + "\n");
            logText.append(lastError + "\n");
        } else {
            logText.append("None\n");
        }

        logText.append("\n<<< Prefs >>>\n");
        logText.append("connector: " + prefs.getString("connector", "") + "\n");
        logText.append("listName: " + prefs.getString("listName", "") + "\n");
        logText.append("listPin: " + prefs.getString("listPin", "") + "\n");
        logText.append("tags: " + prefs.getString("tags", "") + "\n");
        logText.append("dbName: " + prefs.getString("dbName", "") + "\n");
        logText.append("dbUser: " + prefs.getString("dbUser", "") + "\n");
        logText.append("dbPass: " + prefs.getString("dbPass", "") + "\n");
        logText.append("started: " + (prefs.getBoolean("started", false) ? "true" : "false") + "\n");

        logText.append("\n<<< Database >>>\n");
        File path = getApplicationContext().getDir("documentstores", Context.MODE_PRIVATE);
        try {
            DocumentStore ds = DocumentStore.getInstance(new File(path, "alarms"));
            int count = ds.database().getDocumentCount();
            List<DocumentRevision> all = ds.database().read(0, count, true);
            for(DocumentRevision rev : all) {
                String id = rev.getRevision();
                String doc = new String(rev.getBody().asBytes());
                logText.append(id + ": " + doc + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }


    }

    public void onDismiss(View view) {
        finish();
    }
}
