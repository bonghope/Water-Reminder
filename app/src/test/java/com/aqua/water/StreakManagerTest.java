package com.aqua.water;

import com.aqua.water.database.StreakManager;
import com.aqua.water.ui.StreakFlame;
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
        int[] milestones = {3, 7, 14, 30, 50, 100, 365};
        assertEquals(0, StreakFlame.tier(2));
        assertEquals("Chưa thắp lửa", StreakFlame.name(0));
        for (int i = 0; i < milestones.length; i++) {
            int day = milestones[i];
            assertEquals(i, StreakFlame.tier(day - 1));
            assertEquals(i + 1, StreakFlame.tier(day));
            assertNotEquals(StreakFlame.color(day - 1), StreakFlame.color(day));
        }
    }

    @Test
    public void testMilestones_accuracy() {
        assertEquals(3, StreakFlame.nextMilestone(2));
        assertEquals(7, StreakFlame.nextMilestone(3));
        assertEquals(14, StreakFlame.nextMilestone(7));
        assertEquals(30, StreakFlame.nextMilestone(14));
        assertEquals(50, StreakFlame.nextMilestone(30));
        assertEquals(100, StreakFlame.nextMilestone(50));
        assertEquals(365, StreakFlame.nextMilestone(100));
        assertEquals(-1, StreakFlame.nextMilestone(365));
    }
}
