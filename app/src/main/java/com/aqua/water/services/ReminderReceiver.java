package com.aqua.water.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.aqua.water.ui.MainActivity;
import com.example.waterreminder.database.AppConfigManager;

/** Module 3: schedule/cancel alarms and deliver reminders in the original receiver. */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "water";
    public static final int NOTIFICATION_ID = 1;
    public static final String EXTRA_DUE_AT = "due_at";
    private static final String STATE_FILE = "ReminderState";
    private static final String NEXT_AT = "next_at";
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Nhắc uống nước", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Nhắc uống nước theo chu kỳ bạn đã chọn");
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    public static boolean notificationsAllowed(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (!manager.areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        }
        return true;
    }
    public static boolean exactAlarmsAllowed(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    private static SharedPreferences state(Context context) {
        return context.getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE);
    }
    public static long nextReminderAt(Context context) { return state(context).getLong(NEXT_AT, 0); }
    private static PendingIntent operation(Context context, long dueAt) {
        // Matches the old project's PendingIntent, replacing its existing alarm.
        Intent intent = new Intent(context, ReminderReceiver.class).putExtra(EXTRA_DUE_AT, dueAt);
        return PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static synchronized void cancel(Context context) {
        context.getSystemService(AlarmManager.class).cancel(operation(context, 0));
        state(context).edit().remove(NEXT_AT).commit();
        context.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
    }
    /** Restart only for changed settings, reboot or clock changes. */
    public static synchronized void restart(Context context) { install(context, true); }
    /** Preserve the deadline when the app opens again. */
    public static synchronized void ensureScheduled(Context context) { install(context, false); }
    private static void install(Context context, boolean reset) {
        ensureChannel(context);
        AppConfigManager config = new AppConfigManager(context);
        if (!config.isReminderOn() || !notificationsAllowed(context)) { cancel(context); return; }
        long now = System.currentTimeMillis();
        long dueAt = reset ? 0 : nextReminderAt(context);
        if (dueAt <= now) dueAt = now + config.getReminderInterval() * 60_000L;
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent pending = operation(context, dueAt);
        alarms.cancel(pending);
        if (!state(context).edit().putLong(NEXT_AT, dueAt).commit()) {
            Log.e("AquaReminder", "Cannot persist reminder deadline"); return;
        }
        try {
            if (exactAlarmsAllowed(context)) {
                try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending); }
                catch (SecurityException permissionChanged) {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending);
                }
            } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pending);
        } catch (SecurityException e) {
            state(context).edit().remove(NEXT_AT).commit();
            Log.w("AquaReminder", "Android refused reminder scheduling", e);
        }
    }
    /** Ignore obsolete or duplicate deliveries after disabling/replacing an alarm. */
    static synchronized boolean consume(Context context, Intent intent) {
        long deliveredAt = intent.getLongExtra(EXTRA_DUE_AT, 0);
        long expectedAt = nextReminderAt(context);
        if (deliveredAt == 0 || deliveredAt != expectedAt || System.currentTimeMillis() < deliveredAt
                || !new AppConfigManager(context).isReminderOn()) return false;
        state(context).edit().remove(NEXT_AT).commit();
        return true;
    }

    /** Compatibility with callers in the original backend package. */
    public static void schedule(Context context) { restart(context); }
    @Override public void onReceive(Context context, Intent intent) {
        if (!consume(context, intent)) return;
        try {
            if (!notificationsAllowed(context)) return;
            ensureChannel(context);
            Intent openApp = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent open = PendingIntent.getActivity(context, 2, openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL_ID)
                    : new Notification.Builder(context);
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Đến giờ uống nước!")
                    .setContentText("Một ly nước nhỏ giúp bạn luôn tươi tỉnh. 💧")
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setAutoCancel(true).setContentIntent(open);
            if (Build.VERSION.SDK_INT < 26) builder.setDefaults(Notification.DEFAULT_ALL);
            context.getSystemService(NotificationManager.class)
                    .notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.w("AquaReminder", "Notification permission was revoked", e);
        } finally { restart(context); }
    }
}
