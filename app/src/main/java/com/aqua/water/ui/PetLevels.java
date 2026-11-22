package com.aqua.water.ui;

/** Shared names and thresholds for the pet screen and goal celebrations. */
public final class PetLevels {
    private static final String[] NAMES = {"Mầm Nước", "Bé Nước", "Giọt Nước", "Chiến Binh Nước"};
    private PetLevels() {}
    private static final int[] GOAL_PERCENT = {0, 30, 70, 100};

    public static int requiredMl(int level, int goal) {
        return (int) ((Math.max(1, goal) * (long) GOAL_PERCENT[level] + 99) / 100);
    }

    public static int level(int amount, int goal) {
        for (int level = GOAL_PERCENT.length - 1; level > 0; level--) {
            if (amount >= requiredMl(level, goal)) return level;
        }
        return 0;
    }
    public static String name(int level) { return NAMES[Math.max(0, Math.min(3, level))]; }
    public static String title(int level) { return name(level) + " (Cấp " + (level + 1) + ")"; }
}
