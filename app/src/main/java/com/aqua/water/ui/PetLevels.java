package com.aqua.water.ui;

/** Shared names and thresholds for the pet screen and goal celebrations. */
public final class PetLevels {
    private static final String[] NAMES = {"Mầm Nước", "Bé Nước", "Giọt Nước", "Chiến Binh Nước"};
    private PetLevels() {}
    public static int level(int amount, int goal) {
        double progress = amount / (double) Math.max(1, goal);
        return progress < 0.3 ? 0 : progress < 0.7 ? 1 : progress < 1 ? 2 : 3;
    }
    public static String name(int level) { return NAMES[Math.max(0, Math.min(3, level))]; }
    public static String title(int level) { return name(level) + " (Cấp " + (level + 1) + ")"; }
}
