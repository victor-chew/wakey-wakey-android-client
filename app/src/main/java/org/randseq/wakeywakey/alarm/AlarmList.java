package org.randseq.wakeywakey.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudant.sync.documentstore.DocumentRevision;
import com.cloudant.sync.documentstore.DocumentStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class AlarmList {

    Context context;
    Map<String, Alarm> alarms = new HashMap<>();
    List<String> tags = new ArrayList<String>();

    public AlarmList(Context context) {
        this.context = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String tagsStr = prefs.getString("tags", "");
        StringTokenizer st = new StringTokenizer(tagsStr);
        while (st.hasMoreTokens()) {
            String tag = st.nextToken().toUpperCase();
            if (tag.charAt(0) == '#') tags.add(tag);
        }
    }

    public void initFromPrefs() {
        Map<String, ?> prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE).getAll();
        Set<String> keys = prefs.keySet();
        //Log.d("initFromPrefs", "[START]");
        for (String revId : keys) {
            int intentId = Integer.parseInt(prefs.get(revId).toString());
            Alarm alarm = new Alarm(revId, intentId);
            alarms.put(revId, alarm);
            //Log.d("[ENTRY]", revId + ": " + intentId);
        }
        //Log.d("initFromPrefs", "[END]");
    }

    public void initFromDb() {
        File path = context.getDir("documentstores", Context.MODE_PRIVATE);
        try {
            DocumentStore ds = DocumentStore.getInstance(new File(path, "alarms"));
            int count = ds.database().getDocumentCount();
            List<DocumentRevision> all = ds.database().read(0, count, true);
            //Log.d("initFromDb", "[START]");
            for(DocumentRevision rev : all) {
                String revId = rev.getRevision();
                Alarm alarm = parseAlarm(revId, rev.getBody().asMap());
                if (alarm != null) alarms.put(revId, alarm);
                //if (alarm != null)  Log.d("[ENTRY]", revId + ": " + new String(rev.getBody().asBytes()));
            }
            //Log.d("initFromDb", "[END]");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void scheduleAll() {
        SharedPreferences prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (String revId: alarms.keySet()) {
            Alarm alarm = alarms.get(revId);
            alarm.schedule(context);
            if (alarm.isScheduled()) editor.putInt(revId, alarm.getIntentId());
        }
        editor.commit();
    }

    public void cancelAll() {
        SharedPreferences prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (String revId: alarms.keySet()) {
            Alarm alarm = alarms.get(revId);
            alarm.cancel(context);
            editor.remove(revId);
        }
        editor.commit();
    }

    public void migrateFrom(AlarmList oldAlarms) {
        SharedPreferences prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Cancel all alarms in old list that are not present in new list
        for (String revId: oldAlarms.alarms.keySet()) {
            if (!alarms.containsKey(revId)) {
                oldAlarms.alarms.get(revId).cancel(context);
                editor.remove(revId);
            }
        }

        // Schedule all alarms in new list that are not present in old list
        for (String revId: alarms.keySet())
        {
            if (!oldAlarms.alarms.containsKey(revId)) {
                Alarm alarm = alarms.get(revId);
                alarm.schedule(context);
                if (alarm.isScheduled()) editor.putInt(revId, alarm.getIntentId());
            }
        }

        editor.commit();
    }

    Alarm parseAlarm(String revId, Map<String, Object> map) {
        boolean enabled = ((Boolean)map.get("enabled")).booleanValue();
        if (!enabled) return null;
        String title = (String)map.get("title");
        if (tags.size() == 0) {
            // If no tags specified by user, it will only match tagless entres
            if (title.indexOf('#') >= 0) return null;
        } else {
            boolean matched = false;
            for (String tag : tags) {
                if (title.indexOf(tag) >= 0) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return null;
        }
        String msg = (String)map.get("msg");
        String alarm = (String)map.get("alarm");
        int hh = -1;
        int mm = -1;
        boolean[] repeat = { false, false, false, false, false, false, false };
        final String[] days = { "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT" };
        StringTokenizer st = new StringTokenizer(title);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int day = Arrays.asList(days).indexOf(token);
            if (day >= 0) repeat[day] = true;
            else if (token.equals("WEEKDAYS")) {
                for (int i=0; i<5; i++) repeat[i] = true;
            }
            else if (token.equals("WEEKENDS")) {
                for (int i=5; i<7; i++) repeat[i] = true;
            }
            else if (token.length() == 4 && token.charAt(0) != '#') {
                boolean valid = true;
                for (int i=0; i<token.length(); i++) {
                    if (token.charAt(i) < '0' || token.charAt(i) > '9') {
                        valid = false;
                        break;

                    }
                }
                if (valid) {
                    int hh2 = Integer.parseInt(token.substring(0, 2));
                    int mm2 = Integer.parseInt(token.substring(2, 4));
                    if (hh2 >= 0 && hh2 <= 23 && mm2 >= 0 && mm2 <= 59) {
                        hh = hh2;
                        mm = mm2;
                    }
                }
            }
        }
        if (hh == -1 || mm == -1) return null;
        StringBuffer alarm2 = new StringBuffer();
        for (int i=0; i<alarm.length(); i++) {
            char c = alarm.charAt(i);
            if (c == ' ') alarm2.append('_');
            else alarm2.append(Character.toLowerCase(c));
        }
        int alarmTone = context.getResources().getIdentifier(alarm2.toString(), "raw", context.getPackageName());
        if (alarmTone == 0) alarm = "a_real_hoot";

        return new Alarm(revId, hh, mm, msg, alarm2.toString(), repeat);
    }
}
