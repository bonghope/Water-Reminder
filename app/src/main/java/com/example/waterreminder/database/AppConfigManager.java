package com.example.waterreminder.database;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfigManager {
    public static final String PREF_NAME = "AppConfig";
    public static final String KEY_WATER_GOAL = "WATER_GOAL";
    public static final String KEY_REMINDER_IS_ON = "REMINDER_IS_ON";
    public static final String KEY_REMINDER_INTERVAL = "REMINDER_INTERVAL";
    public static final String KEY_THEME_DARK = "THEME_DARK";

    private final SharedPreferences prefs;

    public AppConfigManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getWaterGoal() {
        return prefs.getInt(KEY_WATER_GOAL, 2000);
    }

    public void setWaterGoal(int goal) {
        prefs.edit().putInt(KEY_WATER_GOAL, goal).apply();
    }

    public boolean isReminderOn() {
        return prefs.getBoolean(KEY_REMINDER_IS_ON, false);
    }

    public void setReminderOn(boolean isOn) {
        prefs.edit().putBoolean(KEY_REMINDER_IS_ON, isOn).apply();
    }

    public int getReminderInterval() {
        return prefs.getInt(KEY_REMINDER_INTERVAL, 60);
    }

    public void setReminderInterval(int minutes) {
        prefs.edit().putInt(KEY_REMINDER_INTERVAL, minutes).apply();
    }

    public boolean isThemeDark() {
        return prefs.getBoolean(KEY_THEME_DARK, false);
    }

    public void setThemeDark(boolean isDark) {
        prefs.edit().putBoolean(KEY_THEME_DARK, isDark).apply();
    }
}