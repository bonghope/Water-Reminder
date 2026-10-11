package com.aqua.water.ui;

/** Each reached milestone changes the flame rather than awarding a badge. */
public final class StreakFlame {
    private static final int[] DAYS = {0, 3, 7, 14, 30, 50, 100, 365};
    private static final String[] NAMES = {"Lửa khởi đầu", "Lửa ấm", "Lửa bền bỉ", "Lửa bùng cháy", "Lửa hoàng kim", "Lửa xanh", "Lửa tím", "Lửa cầu vồng"};
    private static final int[] COLORS = {0xFFFF7043, 0xFFFF5722, 0xFFFF9800, 0xFFF44336, 0xFFFFC107, 0xFF29B6F6, 0xFFAB47BC, 0xFFEC407A};
    private StreakFlame() {}
    public static int tier(int days) {
        int tier = 0;
        for (int i = 1; i < DAYS.length; i++) if (days >= DAYS[i]) tier = i;
        return tier;
    }
    public static int nextMilestone(int days) {
        for (int day : DAYS) if (day > days) return day;
        return -1;
    }
    public static String name(int days) { return days <= 0 ? "Chưa thắp lửa" : NAMES[tier(days)]; }
    public static int color(int days) { return days <= 0 ? 0xFF94A3B8 : COLORS[tier(days)]; }
}
