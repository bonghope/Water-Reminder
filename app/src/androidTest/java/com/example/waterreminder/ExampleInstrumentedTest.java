package com.example.waterreminder;

import com.aqua.water.services.ReminderReceiver;
import com.aqua.water.services.BootReceiver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waterreminder.database.AppConfigManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Uses separate settings files so tests do not overwrite the user's configuration. */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private Context context;
    private AppConfigManager config;
    private boolean denyNotifications;

    @Before public void setUp() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("modules34_test_" + name, mode);
            }
            @Override public int checkSelfPermission(String permission) {
                if (denyNotifications && Manifest.permission.POST_NOTIFICATIONS.equals(permission))
                    return PackageManager.PERMISSION_DENIED;
                return super.checkSelfPermission(permission);
            }
        };
        context.getSharedPreferences("AppConfig", 0).edit().clear().commit();
        context.getSharedPreferences("ReminderState", 0).edit().clear().commit();
        config = new AppConfigManager(context);
        ReminderReceiver.cancel(context);
    }

    @After public void tearDown() {
        ReminderReceiver.cancel(context);
        context.getSharedPreferences("AppConfig", 0).edit().clear().commit();
        context.getSharedPreferences("ReminderState", 0).edit().clear().commit();
        // Restore any real app reminder displaced by the test PendingIntent.
        ReminderReceiver.ensureScheduled(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test public void defaultsAndSettingsSurviveNewManager() {
        assertEquals(2000, config.getWaterGoal());
        assertEquals(60, config.getReminderInterval());
        assertFalse(config.isReminderOn());
        assertFalse(config.isThemeDark());
        config.setWaterGoal(2500); config.setThemeDark(true); config.setReminderInterval(90);
        AppConfigManager reopened = new AppConfigManager(context);
        assertEquals(2500, reopened.getWaterGoal());
        assertEquals(90, reopened.getReminderInterval());
        assertTrue(reopened.isThemeDark());
        assertEquals(0, ReminderReceiver.nextReminderAt(context));
    }

    @Test public void invalidInputsDoNotOverwriteSavedSettings() {
        for (int value : new int[]{0, -1, Integer.MIN_VALUE}) {
            try { config.setWaterGoal(value); fail("Invalid goal was accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        for (int value : new int[]{0, -1, 30, 75, Integer.MAX_VALUE}) {
            try { config.setReminderInterval(value); fail("Invalid interval was accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        assertEquals(2000, config.getWaterGoal()); assertEquals(60, config.getReminderInterval());
    }

    @Test public void corruptLegacySettingsAreRepaired() {
        context.getSharedPreferences("AppConfig", 0).edit().putInt("WATER_GOAL", -2)
                .putString("REMINDER_INTERVAL", "bad").putString("THEME_DARK", "bad")
                .putString("REMINDER_IS_ON", "bad").commit();
        AppConfigManager repaired = new AppConfigManager(context);
        assertEquals(2000, repaired.getWaterGoal()); assertEquals(60, repaired.getReminderInterval());
        assertFalse(repaired.isThemeDark()); assertFalse(repaired.isReminderOn());
    }

    @Test public void enableReschedulePreserveDeadlineAndDisable() {
        assertTrue("Grant POST_NOTIFICATIONS before running tests", ReminderReceiver.notificationsAllowed(context));
        config.setReminderOn(true);
        long first = ReminderReceiver.nextReminderAt(context);
        assertNearInterval(first, 60);
        ReminderReceiver.ensureScheduled(context);
        assertEquals(first, ReminderReceiver.nextReminderAt(context));
        config.setWaterGoal(2400); config.setThemeDark(true); config.setReminderInterval(60);
        assertEquals(first, ReminderReceiver.nextReminderAt(context));
        config.setReminderInterval(120);
        assertNearInterval(ReminderReceiver.nextReminderAt(context), 120);
        config.setReminderOn(false);
        assertFalse(config.isReminderOn()); assertEquals(0, ReminderReceiver.nextReminderAt(context));
        new BootReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
        assertEquals(0, ReminderReceiver.nextReminderAt(context));
    }

    @Test public void bootUpdateAndClockChangesRestoreAlarm() {
        config.setReminderOn(true); config.setReminderInterval(90);
        for (String action : new String[]{Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED}) {
            context.getSharedPreferences("ReminderState", 0).edit().remove("next_at").commit();
            new BootReceiver().onReceive(context, new Intent(action));
            assertNearInterval(ReminderReceiver.nextReminderAt(context), 90);
        }
    }

    @Test public void blockedPermissionDoesNotInstallAnAlarm() {
        denyNotifications = true;
        config.setReminderOn(true);
        assertTrue(config.isReminderOn()); // Keep user preference, clearly report blocked in UI.
        assertEquals(0, ReminderReceiver.nextReminderAt(context));
        denyNotifications = false;
        ReminderReceiver.ensureScheduled(context);
        assertNearInterval(ReminderReceiver.nextReminderAt(context), 60);
    }

    @Test public void dueAlarmNotifiesAndStaleDeliveryCannotNotifyAgain() {
        config.setReminderOn(true);
        long due = System.currentTimeMillis() - 1000;
        context.getSharedPreferences("ReminderState", 0).edit().putLong("next_at", due).commit();
        Intent alarm = new Intent(context, ReminderReceiver.class).putExtra(ReminderReceiver.EXTRA_DUE_AT, due);
        new ReminderReceiver().onReceive(context, alarm);
        StatusBarNotification[] posted = awaitNotifications(1);
        assertEquals(1, posted.length);
        assertEquals("Đến giờ uống nước!", posted[0].getNotification().extras.getString(Notification.EXTRA_TITLE));
        assertNotNull(posted[0].getNotification().contentIntent);
        long next = ReminderReceiver.nextReminderAt(context);
        assertNearInterval(next, 60);
        context.getSystemService(NotificationManager.class).cancel(ReminderReceiver.NOTIFICATION_ID);
        new ReminderReceiver().onReceive(context, alarm);
        assertEquals(0, awaitNotifications(0).length);
        assertEquals(next, ReminderReceiver.nextReminderAt(context));
    }

    @Test public void disabledAndEarlyAlarmsCannotNotify() {
        config.setReminderOn(true);
        long due = ReminderReceiver.nextReminderAt(context);
        Intent early = new Intent(context, ReminderReceiver.class).putExtra(ReminderReceiver.EXTRA_DUE_AT, due);
        new ReminderReceiver().onReceive(context, early);
        assertEquals(due, ReminderReceiver.nextReminderAt(context));
        config.setReminderOn(false);
        new ReminderReceiver().onReceive(context, early);
        assertEquals(0, awaitNotifications(0).length);
        assertEquals(0, ReminderReceiver.nextReminderAt(context));
    }

    private StatusBarNotification[] awaitNotifications(int expected) {
        long timeout = android.os.SystemClock.elapsedRealtime() + 3000;
        StatusBarNotification[] notifications;
        do {
            notifications = context.getSystemService(NotificationManager.class).getActiveNotifications();
            if (notifications.length == expected) return notifications;
            android.os.SystemClock.sleep(30);
        } while (android.os.SystemClock.elapsedRealtime() < timeout);
        return notifications;
    }

    private void assertNearInterval(long at, int minutes) {
        long remaining = at - System.currentTimeMillis();
        assertTrue("Expected " + minutes + " minutes, got " + remaining + "ms", Math.abs(remaining - minutes * 60_000L) < 5000);
    }
}
