package com.example.waterreminder.database;

import android.content.Context;
import android.content.SharedPreferences;
import com.aqua.water.services.ReminderReceiver;

/** Module 4: the single entry point for validated, device-local settings. */
public class AppConfigManager {
    public static final String PREF_NAME = "AppConfig";
    public static final String KEY_WATER_GOAL = "WATER_GOAL";
    public static final String KEY_REMINDER_IS_ON = "REMINDER_IS_ON";
    public static final String KEY_REMINDER_INTERVAL = "REMINDER_INTERVAL";
    public static final String KEY_THEME_DARK = "THEME_DARK";
    public static final int DEFAULT_GOAL = 2000;
    public static final int DEFAULT_INTERVAL = 60;
    private final Context context;
    private final SharedPreferences prefs;

    public AppConfigManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Persist defaults and repair invalid older settings without replacing valid values.
        SharedPreferences.Editor defaults = prefs.edit();
        if (!prefs.contains(KEY_WATER_GOAL) || readInt(KEY_WATER_GOAL, 0) <= 0)
            defaults.putInt(KEY_WATER_GOAL, DEFAULT_GOAL);
        if (!validInterval(readInt(KEY_REMINDER_INTERVAL, 0)))
            defaults.putInt(KEY_REMINDER_INTERVAL, DEFAULT_INTERVAL);
        if (!(prefs.getAll().get(KEY_REMINDER_IS_ON) instanceof Boolean))
            defaults.putBoolean(KEY_REMINDER_IS_ON, false);
        if (!(prefs.getAll().get(KEY_THEME_DARK) instanceof Boolean))
            defaults.putBoolean(KEY_THEME_DARK, false);
        defaults.apply();
    }
    private int readInt(String key, int fallback) {
        try { return prefs.getInt(key, fallback); }
        catch (ClassCastException e) { return fallback; }
    }
    private static boolean validInterval(int minutes) { return minutes == 60 || minutes == 90 || minutes == 120; }
    public int getWaterGoal() { return prefs.getInt(KEY_WATER_GOAL, DEFAULT_GOAL); }
    public void setWaterGoal(int goal) {
        if (goal <= 0) throw new IllegalArgumentException("Mục tiêu phải là số nguyên lớn hơn 0");
        prefs.edit().putInt(KEY_WATER_GOAL, goal).apply();
    }
    public boolean isReminderOn() { return prefs.getBoolean(KEY_REMINDER_IS_ON, false); }
    public void setReminderOn(boolean on) {
        if (!prefs.edit().putBoolean(KEY_REMINDER_IS_ON, on).commit())
            throw new IllegalStateException("Không thể lưu cài đặt nhắc nhở");
        ReminderReceiver.restart(context);
    }
    public int getReminderInterval() { return prefs.getInt(KEY_REMINDER_INTERVAL, DEFAULT_INTERVAL); }
    public void setReminderInterval(int minutes) {
        if (!validInterval(minutes)) throw new IllegalArgumentException("Chọn 60, 90 hoặc 120 phút");
        if (getReminderInterval() == minutes) return;
        if (!prefs.edit().putInt(KEY_REMINDER_INTERVAL, minutes).commit())
            throw new IllegalStateException("Không thể lưu chu kỳ nhắc nhở");
        ReminderReceiver.restart(context);
    }
    public boolean isThemeDark() { return prefs.getBoolean(KEY_THEME_DARK, false); }
    public void setThemeDark(boolean dark) { prefs.edit().putBoolean(KEY_THEME_DARK, dark).apply(); }
}
