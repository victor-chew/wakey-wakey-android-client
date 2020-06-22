package org.randseq.wakeywakey.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.randseq.wakeywakey.service.AlarmReceiver;

import java.util.Calendar;

import it.justonetouch.util.ParcelableUtil;

public class Alarm implements Parcelable {

    public static final String TAG = "Alarm";
    private int intentId = -1, hh, mm;
    private String revId, msg, tone;
    private boolean[] repeat;

    public Alarm(String revId, int intentId) {
        this.revId = revId;
        this.intentId = intentId;
    }

    public Alarm(String revId, String msg, String tone) {
        this.revId = revId;
        this.msg = msg;
        this.tone = tone;
    }

    public Alarm(String revId, int hh, int mm, String msg, String tone, boolean[] repeat) {
        this.revId = revId;
        this.hh = hh;
        this.mm = mm;
        this.msg = msg;
        this.tone = tone;
        this.repeat = repeat;
    }

    int getIntentId() {
        int result = 0;
        for (int i=0; i<repeat.length; i++) {
            if (repeat[i]) intentId += Math.pow(2, i);
        }
        return result*10000 + hh*1000 + mm;
    }

    public boolean isScheduled() {
        return intentId > 0;
    }

    public void schedule(Context context) {
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        boolean repeatSpecified = false;
        for (boolean b : repeat) repeatSpecified |= b;
        if (repeatSpecified && !repeat[day-1]) {
            Log.d("Alarm.schedule", "revId: " + revId + "; not scheduled; repeatSpecified = " + repeatSpecified + "; day = " + day);
            return;
        };
        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.HOUR_OF_DAY, hh);
        cal2.set(Calendar.MINUTE, mm);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.DAY_OF_WEEK, day);
        long alarmTime = cal2.getTimeInMillis();
        if (alarmTime > now && Math.abs(alarmTime - now) <= 30L*60*1000) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            final Bundle bundle = new Bundle();
            bundle.putByteArray(Alarm.TAG, ParcelableUtil.marshall(this));
            intent.putExtras(bundle);
            intentId = getIntentId();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            Log.d("Alarm.schedule",
                "revId: " + revId + "; intentId: " + String.valueOf(intentId) + "; " + String.valueOf(hh) + ":" + String.valueOf(mm));
        } else {
            Log.d("Alarm.schedule", "revId: " + revId + "; not scheduled; alarmTime = " + alarmTime + "; now = " + now);
        }
    }

    public void cancel(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        final Bundle bundle = new Bundle();
        bundle.putByteArray(Alarm.TAG, ParcelableUtil.marshall(this));
        intent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        //Log.d("Alarm.cancel", "revId: " + revId + "; intentId: " + String.valueOf(intentId));
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(revId);
        dest.writeString(msg);
        dest.writeString(tone);
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel source) {
            String revId = source.readString();
            String msg = source.readString();
            String tone = source.readString();
            return new Alarm(revId, msg, tone);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    public String getRevId() {
        return revId;
    }

    public String getMsg() {
        return msg;
    }

    public String getTone() {
        return tone;
    }
}
