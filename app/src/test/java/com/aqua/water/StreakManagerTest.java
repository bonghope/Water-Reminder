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
}
