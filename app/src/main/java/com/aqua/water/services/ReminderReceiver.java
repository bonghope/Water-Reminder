package com.aqua.water.services;
import com.aqua.water.ui.MainActivity;
import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static void schedule(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("AppConfig",0);
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent i=PendingIntent.getBroadcast(c,1,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        a.cancel(i);
        if(p.getBoolean("REMINDER_IS_ON",false)) a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+p.getInt("REMINDER_INTERVAL",60)*60000L,i);
    }
    public void onReceive(Context c,Intent intent) {
        if(!c.getSharedPreferences("AppConfig",0).getBoolean("REMINDER_IS_ON",false)) return;
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel("water","Nhắc uống nước",NotificationManager.IMPORTANCE_DEFAULT));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"water"):new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Đến giờ uống nước!").setContentText("Một ly nước nhỏ giúp bạn luôn tươi tỉnh. 💧").setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c,2,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        try { nm.notify(1,b.build()); } catch(SecurityException ignored) { }
        schedule(c);
    }
}
