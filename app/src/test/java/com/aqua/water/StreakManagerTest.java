package com.aqua.water;

import com.aqua.water.database.StreakManager;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

public class StreakManagerTest {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private String getDateOffset(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -daysAgo);
        return sdf.format(cal.getTime());
    }

    @Test
    public void testStreakZero_whenNoWaterLogged() {
        Map<String, Integer> data = new HashMap<>();
        StreakManager manager = new StreakManager(date -> data.getOrDefault(date, 0), null);
        StreakManager.StreakInfo info = manager.getStreakInfo(2000);

        assertEquals(0, info.currentStreak);
        assertFalse(info.isGoalAchievedToday);
        assertEquals(2000, info.remainingMl);
    }

    @Test
    public void testStreakOne_whenGoalAchievedTodayOnly() {
        Map<String, Integer> data = new HashMap<>();
        data.put(getDateOffset(0), 2000);

        StreakManager manager = new StreakManager(date -> data.getOrDefault(date, 0), null);
        StreakManager.StreakInfo info = manager.getStreakInfo(2000);

        assertEquals(1, info.currentStreak);
        assertTrue(info.isGoalAchievedToday);
        assertEquals(0, info.remainingMl);
    }

    @Test
    public void testStreakConsecutiveThreeDays_whenAllAchieved() {
        Map<String, Integer> data = new HashMap<>();
        data.put(getDateOffset(0), 2200); // Today
        data.put(getDateOffset(1), 2000); // Yesterday
        data.put(getDateOffset(2), 2500); // 2 days ago
        data.put(getDateOffset(3), 1000); // 3 days ago: failed

        StreakManager manager = new StreakManager(date -> data.getOrDefault(date, 0), null);
        StreakManager.StreakInfo info = manager.getStreakInfo(2000);

        assertEquals(3, info.currentStreak);
        assertTrue(info.isGoalAchievedToday);
        assertEquals(3, info.bestStreak);
    }

    @Test
    public void testStreakPreservedToday_whenYesterdayAchievedButTodayPending() {
        Map<String, Integer> data = new HashMap<>();
        data.put(getDateOffset(0), 500);  // Today: not yet reached 2000
        data.put(getDateOffset(1), 2100); // Yesterday: reached
        data.put(getDateOffset(2), 2000); // 2 days ago: reached

        StreakManager manager = new StreakManager(date -> data.getOrDefault(date, 0), null);
        StreakManager.StreakInfo info = manager.getStreakInfo(2000);

        assertEquals(2, info.currentStreak); // Maintained from yesterday
        assertFalse(info.isGoalAchievedToday);
        assertEquals(1500, info.remainingMl);
    }

    @Test
    public void testStreakBroken_whenYesterdayFailedAndTodayPending() {
        Map<String, Integer> data = new HashMap<>();
        data.put(getDateOffset(0), 500);  // Today: not reached
        data.put(getDateOffset(1), 800);  // Yesterday: failed
        data.put(getDateOffset(2), 2000); // 2 days ago

        StreakManager manager = new StreakManager(date -> data.getOrDefault(date, 0), null);
        StreakManager.StreakInfo info = manager.getStreakInfo(2000);

        assertEquals(0, info.currentStreak);
        assertFalse(info.isGoalAchievedToday);
    }

    @Test
    public void testFlameTiers_progression() {
        // 0-2 days: Khởi Động
        StreakManager.FlameTier tier0 = StreakManager.getFlameTier(0);
        assertEquals("Ngọn Lửa Khởi Động", tier0.name);
        StreakManager.FlameTier tier2 = StreakManager.getFlameTier(2);
        assertEquals("Ngọn Lửa Khởi Động", tier2.name);

        // 3-6 days: Lửa Lam
        StreakManager.FlameTier tier3 = StreakManager.getFlameTier(3);
        assertEquals("Ngọn Lửa Lam", tier3.name);
        assertEquals("#0284C7", tier3.startColor);

        // 7-13 days: Lửa Tím
        StreakManager.FlameTier tier7 = StreakManager.getFlameTier(7);
        assertEquals("Ngọn Lửa Tím", tier7.name);
        assertEquals("#7E22CE", tier7.startColor);

        // 14-29 days: Hoàng Kim
        StreakManager.FlameTier tier14 = StreakManager.getFlameTier(14);
        assertEquals("Ngọn Lửa Hoàng Kim", tier14.name);
        assertEquals("#D97706", tier14.startColor);

        // 30+ days: Thần Thoại
        StreakManager.FlameTier tier30 = StreakManager.getFlameTier(30);
        assertEquals("Ngọn Lửa Thần Thoại", tier30.name);
    }

    @Test
    public void testMilestones_accuracy() {
        assertTrue(StreakManager.isMilestone(3));
        assertTrue(StreakManager.isMilestone(7));
        assertTrue(StreakManager.isMilestone(14));
        assertTrue(StreakManager.isMilestone(30));
        assertFalse(StreakManager.isMilestone(1));
        assertFalse(StreakManager.isMilestone(2));
        assertFalse(StreakManager.isMilestone(4));
        assertFalse(StreakManager.isMilestone(8));
    }
}
