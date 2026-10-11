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
        badges.add(new StreakBadge(3, "Tia Lửa Mới", "🔥", "Khởi động 3 ngày liên tiếp", "Đồng"));
        badges.add(new StreakBadge(7, "Ngọn Lửa Bền Bỉ", "🔥", "Duy trì 1 tuần liên tiếp", "Bạc"));
        badges.add(new StreakBadge(14, "Bùng Cháy 2 Tuần", "🔥", "2 tuần kiên trì liên tiếp", "Vàng"));
        badges.add(new StreakBadge(30, "Lửa Vàng Hoàng Kim", "🔥", "1 tháng bền bỉ nuôi dưỡng sức khỏe", "Bạch Kim"));
        badges.add(new StreakBadge(50, "Tinh Thể Kim Cương", "🔥", "50 ngày bứt phá giới hạn", "Kim Cương"));
        badges.add(new StreakBadge(100, "Vương Miện Bách Nhật", "🔥", "100 ngày kỷ luật bền bỉ", "Tinh Anh"));
        badges.add(new StreakBadge(365, "Huyền Thoại Thần Nước", "🔥", "1 năm kiên định không ngừng nghỉ", "Huyền Thoại"));
        ALL_BADGES = Collections.unmodifiableList(badges);
    }

    public static List<StreakBadge> getAllBadges() {
        return ALL_BADGES;
    }

    public static StreakBadge getCurrentBadge(int currentStreak) {
        StreakBadge current = new StreakBadge(0, "Ngọn Lửa Khởi Động", "🔥", "Bắt đầu hành trình giữ lửa, uống nước mỗi ngày", "Tập Sự");
        for (StreakBadge b : ALL_BADGES) {
            if (currentStreak >= b.requiredDays) {
                current = b;
            }
        }
        return current;
    }

    public static StreakBadge getNextBadge(int currentStreak) {
        for (StreakBadge b : ALL_BADGES) {
            if (currentStreak < b.requiredDays) {
                return b;
            }
        }
        return null; // All badges unlocked
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
