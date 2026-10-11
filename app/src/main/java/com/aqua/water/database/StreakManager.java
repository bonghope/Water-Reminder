package com.aqua.water.database;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.waterreminder.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StreakManager {
    private static final String PREF_NAME = "StreakConfig";
    private static final String KEY_BEST_STREAK = "BEST_STREAK";
    private static final String KEY_STREAK_NAME = "STREAK_NAME";
    public static final String DEFAULT_STREAK_NAME = "Chuỗi Uống Nước";

    public interface WaterDataProvider {
        int getWaterToday(String dateString);
    }

    public static class FlameTier {
        public final int minDays;
        public final String name;
        public final String icon;
        public final String startColor;
        public final String endColor;
        public final String strokeColor;
        public final String glowColor;
        public final String effectDescription;

        public FlameTier(int minDays, String name, String icon, String startColor, String endColor, String strokeColor, String glowColor, String effectDescription) {
            this.minDays = minDays;
            this.name = name;
            this.icon = icon;
            this.startColor = startColor;
            this.endColor = endColor;
            this.strokeColor = strokeColor;
            this.glowColor = glowColor;
            this.effectDescription = effectDescription;
        }

        public boolean isUnlocked(int streak) {
            return streak >= minDays;
        }
    }

    private static final List<FlameTier> ALL_FLAME_TIERS;
    static {
        List<FlameTier> tiers = new ArrayList<>();
        tiers.add(new FlameTier(0, "Ngọn Lửa Khởi Động", "🔥", "#FF6D00", "#FF3D00", "#FFE082", "#FF9800", "Bắt đầu hành trình giữ lửa, uống đủ nước mỗi ngày"));
        tiers.add(new FlameTier(3, "Ngọn Lửa Lam", "⚡🔥", "#0284C7", "#00E5FF", "#BAE6FD", "#38BDF8", "Mở khóa hiệu ứng Lửa Lam bứt phá 3 ngày liên tiếp!"));
        tiers.add(new FlameTier(7, "Ngọn Lửa Tím", "✨🔥", "#7E22CE", "#E879F9", "#F5D0FE", "#C084FC", "Mở khóa hiệu ứng Tử Diễm huyền bí bền bỉ 1 tuần!"));
        tiers.add(new FlameTier(14, "Ngọn Lửa Hoàng Kim", "🌟🔥", "#D97706", "#FDE047", "#FEF08A", "#FBBF24", "Mở khóa hiệu ứng Lửa Vàng Hoàng Kim 2 tuần liên tiếp!"));
        tiers.add(new FlameTier(30, "Ngọn Lửa Thần Thoại", "👑🔥", "#E11D48", "#FB7185", "#FFE4E6", "#FDA4AF", "Mở khóa hiệu ứng Thần Thoại bất diệt 1 tháng liên tục!"));
        tiers.add(new FlameTier(50, "Tinh Thể Băng Lửa", "💎🔥", "#0D9488", "#2DD4BF", "#CCFBF1", "#5EEAD4", "Hiệu ứng Tinh Thể Băng Lửa 50 ngày bứt phá giới hạn!"));
        tiers.add(new FlameTier(100, "Vương Miện Bách Nhật", "🔱🔥", "#4F46E5", "#818CF8", "#E0E7FF", "#A5B4FC", "Hiệu ứng Vương Miện Bách Nhật 100 ngày kỷ luật đỉnh cao!"));
        tiers.add(new FlameTier(365, "Chiến Binh Bất Diệt", "🔥", "#B91C1C", "#F87171", "#FEE2E2", "#EF4444", "Huyền thoại 1 năm kiên định không ngừng nghỉ!"));
        ALL_FLAME_TIERS = Collections.unmodifiableList(tiers);
    }

    public static List<FlameTier> getAllFlameTiers() {
        return ALL_FLAME_TIERS;
    }

    public static FlameTier getFlameTier(int streakDays) {
        FlameTier current = ALL_FLAME_TIERS.get(0);
        for (FlameTier t : ALL_FLAME_TIERS) {
            if (streakDays >= t.minDays) {
                current = t;
            }
        }
        return current;
    }

    public static FlameTier getNextFlameTier(int streakDays) {
        for (FlameTier t : ALL_FLAME_TIERS) {
            if (streakDays < t.minDays) {
                return t;
            }
        }
        return null;
    }

    public static boolean isMilestone(int streakDays) {
        return streakDays == 3 || streakDays == 7 || streakDays == 14 || streakDays == 30 || streakDays == 50 || streakDays == 100 || streakDays == 365;
    }

    public static class StreakBadge {
        public final int requiredDays;
        public final String name;
        public final String icon;
        public final String description;
        public final String rankName;

        public StreakBadge(int requiredDays, String name, String icon, String description, String rankName) {
            this.requiredDays = requiredDays;
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.rankName = rankName;
        }

        public boolean isUnlocked(int currentStreak) {
            return currentStreak >= requiredDays;
        }
    }

    private static final List<StreakBadge> ALL_BADGES;
    static {
        List<StreakBadge> badges = new ArrayList<>();
        badges.add(new StreakBadge(3, "Ngọn Lửa Lam", "⚡🔥", "Mở khóa hiệu ứng Lửa Lam bứt phá", "Mốc 3 ngày"));
        badges.add(new StreakBadge(7, "Ngọn Lửa Tím", "✨🔥", "Mở khóa hiệu ứng Tử Diễm huyền bí", "Mốc 7 ngày"));
        badges.add(new StreakBadge(14, "Ngọn Lửa Hoàng Kim", "🌟🔥", "Mở khóa hiệu ứng Ánh Kim chói lọi", "Mốc 14 ngày"));
        badges.add(new StreakBadge(30, "Ngọn Lửa Thần Thoại", "👑🔥", "Mở khóa hiệu ứng Thần Thoại bất diệt", "Mốc 30 ngày"));
        badges.add(new StreakBadge(50, "Tinh Thể Băng Lửa", "💎🔥", "50 ngày bứt phá giới hạn", "Mốc 50 ngày"));
        badges.add(new StreakBadge(100, "Vương Miện Bách Nhật", "🔱🔥", "100 ngày kỷ luật bền bỉ", "Mốc 100 ngày"));
        badges.add(new StreakBadge(365, "Chiến Binh Bất Diệt", "🔥", "1 năm kiên định không ngừng nghỉ", "Mốc 1 năm"));
        ALL_BADGES = Collections.unmodifiableList(badges);
    }

    public static List<StreakBadge> getAllBadges() {
        return ALL_BADGES;
    }

    public static StreakBadge getCurrentBadge(int currentStreak) {
        FlameTier t = getFlameTier(currentStreak);
        return new StreakBadge(t.minDays, t.name, t.icon, t.effectDescription, t.minDays == 0 ? "Khởi Động" : (t.minDays + " ngày"));
    }

    public static StreakBadge getNextBadge(int currentStreak) {
        FlameTier t = getNextFlameTier(currentStreak);
        if (t == null) return null;
        return new StreakBadge(t.minDays, t.name, t.icon, t.effectDescription, t.minDays + " ngày");
    }

    private DatabaseHelper db;
    private final WaterDataProvider dataProvider;
    private final SharedPreferences prefs;
    private int inMemoryBestStreak = 0;
    private String inMemoryStreakName = DEFAULT_STREAK_NAME;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public StreakManager(Context context) {
        this(new DatabaseHelper(context), context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    public StreakManager(DatabaseHelper db, SharedPreferences prefs) {
        this(db != null ? db::getWaterToday : date -> 0, prefs);
        this.db = db;
    }

    public StreakManager(WaterDataProvider provider, SharedPreferences prefs) {
        this.dataProvider = provider != null ? provider : date -> 0;
        this.prefs = prefs;
        if (prefs != null) {
            this.inMemoryBestStreak = prefs.getInt(KEY_BEST_STREAK, 0);
            this.inMemoryStreakName = prefs.getString(KEY_STREAK_NAME, DEFAULT_STREAK_NAME);
        }
    }

    public String getStreakName() {
        if (prefs != null) {
            return prefs.getString(KEY_STREAK_NAME, inMemoryStreakName);
        }
        return inMemoryStreakName;
    }

    public void setStreakName(String name) {
        String trimmed = (name != null && !name.trim().isEmpty()) ? name.trim() : DEFAULT_STREAK_NAME;
        this.inMemoryStreakName = trimmed;
        if (prefs != null) {
            prefs.edit().putString(KEY_STREAK_NAME, trimmed).apply();
        }
    }

    public static class StreakInfo {
        public final int currentStreak;
        public final int bestStreak;
        public final boolean isGoalAchievedToday;
        public final int remainingMl;
        public final int todayIntake;
        public final int dailyGoal;

        public StreakInfo(int currentStreak, int bestStreak, boolean isGoalAchievedToday,
                          int remainingMl, int todayIntake, int dailyGoal) {
            this.currentStreak = currentStreak;
            this.bestStreak = bestStreak;
            this.isGoalAchievedToday = isGoalAchievedToday;
            this.remainingMl = remainingMl;
            this.todayIntake = todayIntake;
            this.dailyGoal = dailyGoal;
        }

        public String getStatusSummary() {
            if (isGoalAchievedToday) {
                return "Chuỗi " + currentStreak + " ngày • Đã hoàn thành mục tiêu hôm nay";
            } else if (currentStreak > 0) {
                return "Chuỗi " + currentStreak + " ngày • Còn " + remainingMl + " ml để hoàn thành hôm nay";
            } else {
                return "Uống đủ 100% mục tiêu hôm nay để duy trì chuỗi";
            }
        }
    }

    public StreakInfo getStreakInfo(int dailyGoal) {
        Calendar cal = Calendar.getInstance();
        String todayStr = dateFormat.format(cal.getTime());
        int todayIntake = dataProvider.getWaterToday(todayStr);
        boolean achievedToday = todayIntake >= dailyGoal;
        int remaining = Math.max(0, dailyGoal - todayIntake);

        int streak = 0;
        if (achievedToday) {
            streak = 1;
            Calendar checkCal = Calendar.getInstance();
            checkCal.add(Calendar.DATE, -1);
            while (true) {
                String dStr = dateFormat.format(checkCal.getTime());
                if (dataProvider.getWaterToday(dStr) >= dailyGoal) {
                    streak++;
                    checkCal.add(Calendar.DATE, -1);
                } else {
                    break;
                }
            }
        } else {
            Calendar checkCal = Calendar.getInstance();
            checkCal.add(Calendar.DATE, -1);
            String yesterdayStr = dateFormat.format(checkCal.getTime());
            if (dataProvider.getWaterToday(yesterdayStr) >= dailyGoal) {
                streak = 1;
                checkCal.add(Calendar.DATE, -1);
                while (true) {
                    String dStr = dateFormat.format(checkCal.getTime());
                    if (dataProvider.getWaterToday(dStr) >= dailyGoal) {
                        streak++;
                        checkCal.add(Calendar.DATE, -1);
                    } else {
                        break;
                    }
                }
            } else {
                streak = 0;
            }
        }

        int storedBest = prefs != null ? prefs.getInt(KEY_BEST_STREAK, inMemoryBestStreak) : inMemoryBestStreak;
        int bestStreak = Math.max(storedBest, streak);
        inMemoryBestStreak = bestStreak;
        if (prefs != null && bestStreak > storedBest) {
            prefs.edit().putInt(KEY_BEST_STREAK, bestStreak).apply();
        }

        return new StreakInfo(streak, bestStreak, achievedToday, remaining, todayIntake, dailyGoal);
    }

    public void close() {
        if (db != null) {
            db.close();
        }
    }
}
