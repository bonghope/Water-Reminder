package com.aqua.water.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import android.view.ViewGroup;
import android.view.Gravity;
import android.util.TypedValue;
import android.view.WindowInsets;
import android.widget.*;
import com.aqua.water.R;
import com.aqua.water.database.StreakManager;
import com.aqua.water.database.WaterStore;
import com.aqua.water.services.ReminderReceiver;
import com.example.waterreminder.database.AppConfigManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Binds the XML screens to local data and user actions. */
public class MainActivity extends Activity {
    private WaterStore db;
    private StreakManager streakManager;
    private SharedPreferences prefs;
    private AppConfigManager config;
    private WaterView waterView;
    private boolean resumed, week, month;
    private int tab;
    private int selectedDrink = 1;
    private boolean addCardVisible = true;
    private float floatingStreakX = -1, floatingStreakY = -1;
    private final int[] screens = {R.layout.screen_home, R.layout.screen_history, R.layout.screen_stats,
            R.layout.screen_pet, R.layout.screen_reminders, R.layout.screen_settings};
    private final int[] navigation = {R.id.nav_0, R.id.nav_1, R.id.nav_2, R.id.nav_3, R.id.nav_4, R.id.nav_5};

        @Override public void onCreate(Bundle state) {
        prefs = getSharedPreferences("AppConfig", 0);
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        setTheme(dark ? R.style.Theme_Aqua_Dark : R.style.Theme_Aqua);
        super.onCreate(state);
        db = new WaterStore(this);
        streakManager = new StreakManager(this);
        config = new AppConfigManager(this);
        if (state != null) {
            tab = Math.max(0, Math.min(screens.length - 1, state.getInt("tab", 0)));
            week = state.getBoolean("week", false);
            month = state.getBoolean("month", false);
            selectedDrink = Math.max(1, Math.min(4, state.getInt("selectedDrink", 1)));
            addCardVisible = state.getBoolean("addCardVisible", true);
            floatingStreakX = state.getFloat("floatingStreakX", -1);
            floatingStreakY = state.getFloat("floatingStreakY", -1);
        }
        show();
    }

    @Override protected void onResume() {
        super.onResume(); resumed = true;
        ReminderReceiver.ensureScheduled(this);
        show();
    }
    @Override protected void onPause() {
        resumed = false;
        if (waterView != null) waterView.setActive(false);
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (db != null) db.close();
        if (streakManager != null) streakManager.close();
        super.onDestroy();
    }
        @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", tab);
        state.putBoolean("week", week);
        state.putBoolean("month", month);
        state.putInt("selectedDrink", selectedDrink);
        state.putBoolean("addCardVisible", addCardVisible);
        state.putFloat("floatingStreakX", floatingStreakX);
        state.putFloat("floatingStreakY", floatingStreakY);
    }

    private int goal() { return Math.max(1, prefs.getInt("WATER_GOAL", 2000)); }
    private int total() { return db.total(WaterStore.date(System.currentTimeMillis())); }
    private void label(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(value);
    }
    private void click(int id, Runnable action) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(view -> action.run());
    }

    private void show() {
        WaterView previousWater = waterView;
        if (previousWater != null) previousWater.setActive(false);
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT < 30) flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        View root = findViewById(R.id.content_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        View body = findViewById(R.id.page_body);
        ViewGroup.LayoutParams size = body.getLayoutParams();
        size.width = tab == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.min(getResources().getDisplayMetrics().widthPixels,
                (int) (600 * getResources().getDisplayMetrics().density));
        body.setLayoutParams(size);
        int horizontalPadding = (int) (20 * getResources().getDisplayMetrics().density);
        int bodyPadding = tab == 0 ? 0 : horizontalPadding;
        body.setPadding(bodyPadding, body.getPaddingTop(), bodyPadding, body.getPaddingBottom());
        if (tab == 0) body.setPadding(0, 4, 0, 0);
        View header = findViewById(R.id.page_header);
        int headerPadding = tab == 0 ? horizontalPadding : 0;
        header.setPadding(headerPadding, 0, headerPadding, 0);

        // Draggable Floating Streak Flame & Tier Badge
        StreakManager.StreakInfo headerStreak = streakManager.getStreakInfo(goal());
        StreakManager.FlameTier currentTier = StreakManager.getFlameTier(headerStreak.currentStreak);

        TextView headerStreakIcon = findViewById(R.id.header_streak_icon);
        if (headerStreakIcon != null) {
            headerStreakIcon.setText(currentTier.icon);
            headerStreakIcon.setTextSize(currentTier.badgeIconSp);
            if (headerStreak.isGoalAchievedToday) {
                headerStreakIcon.animate().cancel();
                android.animation.ObjectAnimator pulseX = android.animation.ObjectAnimator.ofFloat(headerStreakIcon, "scaleX", 1.0f, 1.16f, 1.0f);
                android.animation.ObjectAnimator pulseY = android.animation.ObjectAnimator.ofFloat(headerStreakIcon, "scaleY", 1.0f, 1.16f, 1.0f);
                pulseX.setDuration(950);
                pulseY.setDuration(950);
                pulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                pulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                pulseX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                pulseY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                pulseX.start();
                pulseY.start();
            } else {
                headerStreakIcon.animate().cancel();
                headerStreakIcon.setScaleX(1.0f);
                headerStreakIcon.setScaleY(1.0f);
            }
        }
        label(R.id.header_streak_text, String.valueOf(headerStreak.currentStreak));
        TextView headerStreakText = findViewById(R.id.header_streak_text);
        if (headerStreakText != null) {
            headerStreakText.setTextColor(Color.parseColor(currentTier.startColor));
        }
        View floatingBadge = findViewById(R.id.floating_streak_badge);
        if (floatingBadge != null) {
            android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
            badgeBg.setCornerRadius(24 * getResources().getDisplayMetrics().density);
            badgeBg.setColor(Color.WHITE);
            badgeBg.setStroke((int) (2f * getResources().getDisplayMetrics().density), Color.parseColor(currentTier.startColor));
            floatingBadge.setBackground(badgeBg);
            floatingBadge.setAlpha(headerStreak.isGoalAchievedToday ? 1.0f : 0.92f);
            floatingBadge.setScaleX(currentTier.scaleMultiplier);
            floatingBadge.setScaleY(currentTier.scaleMultiplier);
            if (floatingStreakX >= 0 && floatingStreakY >= 0) {
                floatingBadge.setX(floatingStreakX);
                floatingBadge.setY(floatingStreakY);
            }
            floatingBadge.post(() -> {
                if (floatingStreakX >= 0 && floatingStreakY >= 0) {
                    floatingBadge.setX(floatingStreakX);
                    floatingBadge.setY(floatingStreakY);
                }
            });
            floatingBadge.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY, startX, startY;
                private boolean isDragging;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            dX = view.getX() - event.getRawX();
                            dY = view.getY() - event.getRawY();
                            startX = event.getRawX();
                            startY = event.getRawY();
                            isDragging = false;
                            view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).start();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dist = (float) Math.hypot(event.getRawX() - startX, event.getRawY() - startY);
                            if (dist > 12) isDragging = true;
                            if (isDragging) {
                                float newX = event.getRawX() + dX;
                                float newY = event.getRawY() + dY;
                                View parent = (View) view.getParent();
                                if (parent != null) {
                                    float maxX = parent.getWidth() - view.getWidth();
                                    float maxY = parent.getHeight() - view.getHeight() - (75 * getResources().getDisplayMetrics().density);
                                    float minY = 20 * getResources().getDisplayMetrics().density;
                                    newX = Math.max(0, Math.min(newX, maxX));
                                    newY = Math.max(minY, Math.min(newY, maxY));
                                }
                                view.setX(newX);
                                view.setY(newY);
                                floatingStreakX = newX;
                                floatingStreakY = newY;
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                            if (!isDragging) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                showStreakDialog(headerStreak);
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        FrameLayout content = findViewById(R.id.screen_container);
        getLayoutInflater().inflate(screens[tab], content, true);
        FrameLayout scene = findViewById(R.id.scene);
        WaterView inflatedWater = findViewById(R.id.water);
        waterView = null;
        if (tab == 0) {
            if (previousWater != null) {
                if (previousWater.getParent() != null) ((ViewGroup) previousWater.getParent()).removeView(previousWater);
                scene.removeView(inflatedWater);
                scene.addView(previousWater, 0, new FrameLayout.LayoutParams(-1, -1));
                waterView = previousWater;
            } else waterView = inflatedWater;
            waterView.setProgress(total() / (float) goal());
            waterView.setActive(resumed);
        } else inflatedWater.setVisibility(View.GONE);
        for (int i = 0; i < navigation.length; i++) {
            final int next = i;
            TextView item = findViewById(navigation[i]);
            int color = i == tab ? getColor(R.color.aqua_blue) : Color.rgb(143, 163, 184);
            item.setTextColor(color);
            item.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(color));
            item.setSelected(i == tab);
            item.setOnClickListener(v -> { tab = next; show(); });
        }
        click(R.id.nav_add_water, () -> {
            if (tab == 0) {
                addCardVisible = !addCardVisible;
            } else {
                tab = 0;
                addCardVisible = true;
            }
            show();
        });
        switch (tab) {
            case 0: bindHome(); break;
            case 1: bindHistory(); break;
            case 2: bindStats(); break;
            case 3: bindPet(); break;
            case 4: bindReminders(); break;
            case 5: bindSettings(); break;
        }
        updateAddCard();
    }

    private void bindHome() {
        int amount = total();
        int waterGoal = goal();
        float percentage = ((float) amount / waterGoal) * 100f;
        int remaining = Math.max(0, waterGoal - amount);

        label(R.id.home_percent, Math.round(percentage) + "%");
        label(R.id.home_amount, amount + " / " + waterGoal + " ml");
        if (remaining <= 0) {
            label(R.id.home_remaining, "Đã hoàn thành mục tiêu!");
        } else {
            label(R.id.home_remaining, "Còn " + remaining + " ml để hoàn thành mục tiêu");
        }


        int[] drinkIds = {R.id.ivWater, R.id.ivTea, R.id.ivSweet, R.id.ivMilk};
        for (int i = 0; i < drinkIds.length; i++) {
            final int category = i + 1;
            click(drinkIds[i], () -> { selectedDrink = category; updateDrinkSelection(); });
        }
        updateDrinkSelection();
        click(R.id.btn100ml, () -> add(100));
        click(R.id.btn200ml, () -> add(200));
        click(R.id.btn300ml, () -> add(300));
        click(R.id.btn500ml, () -> add(500));
        click(R.id.btnCustom, () -> number("Lượng nước (ml)", 0, false));
        click(R.id.btnCloseCard, () -> { addCardVisible = false; updateAddCard(); });
        click(R.id.btnReopenCard, () -> { addCardVisible = true; updateAddCard(); });
        click(R.id.tvGoToHistory, () -> { tab = 1; show(); });
        updateAddCard();
    }

    private void updateDrinkSelection() {
        int[] ids = {R.id.ivWater, R.id.ivTea, R.id.ivSweet, R.id.ivMilk};
        for (int i = 0; i < ids.length; i++) {
            View icon = findViewById(ids[i]);
            boolean selected = selectedDrink == i + 1;
            icon.setSelected(selected);
            icon.setBackgroundResource(selected ? R.drawable.bg_circle_selected : R.drawable.bg_circle_unselected);
        }
    }

    private void updateAddCard() {
        boolean visible = tab == 0 && addCardVisible;
        View overlay = findViewById(R.id.add_water_overlay);
        if (overlay != null) overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        View card = findViewById(R.id.cardAddWater);
        if (card != null) card.setVisibility(addCardVisible ? View.VISIBLE : View.GONE);
        View reopen = findViewById(R.id.btnReopenCard);
        if (reopen != null) reopen.setVisibility(View.GONE);

        View homeBox = findViewById(R.id.home_progress_box);
        if (homeBox != null && tab == 0) {
            TextView tvTitle = findViewById(R.id.home_title);
            TextView tvPercent = findViewById(R.id.home_percent);
            TextView tvAmount = findViewById(R.id.home_amount);
            TextView tvRemaining = findViewById(R.id.home_remaining);
            float density = getResources().getDisplayMetrics().density;

            if (addCardVisible) {
                // Đang mở thêm nước: phần chữ góc trái bên trên, cỡ chữ bình thường
                homeBox.animate().translationY(0f).setDuration(220).start();
                if (homeBox instanceof LinearLayout) {
                    ((LinearLayout) homeBox).setGravity(Gravity.START);
                }
                if (tvTitle != null) {
                    tvTitle.setGravity(Gravity.START);
                    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                }
                if (tvPercent != null) {
                    tvPercent.setGravity(Gravity.START);
                    tvPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
                }
                if (tvAmount != null) {
                    tvAmount.setGravity(Gravity.START);
                    tvAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                }
                if (tvRemaining != null) {
                    tvRemaining.setGravity(Gravity.START);
                    tvRemaining.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                }
            } else {
                // Tắt thêm nước: chuyển xuống giữa, cỡ chữ to lên 1 chút
                View pageScroll = findViewById(R.id.page_scroll);
                float targetY = 160f * density;
                if (pageScroll != null && pageScroll.getHeight() > 0) {
                    targetY = Math.max(120f * density, (pageScroll.getHeight() - homeBox.getHeight()) * 0.42f);
                }
                homeBox.animate().translationY(targetY).setDuration(220).start();
                if (homeBox instanceof LinearLayout) {
                    ((LinearLayout) homeBox).setGravity(Gravity.CENTER_HORIZONTAL);
                }
                if (tvTitle != null) {
                    tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
                    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                }
                if (tvPercent != null) {
                    tvPercent.setGravity(Gravity.CENTER_HORIZONTAL);
                    tvPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
                }
                if (tvAmount != null) {
                    tvAmount.setGravity(Gravity.CENTER_HORIZONTAL);
                    tvAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                }
                if (tvRemaining != null) {
                    tvRemaining.setGravity(Gravity.CENTER_HORIZONTAL);
                    tvRemaining.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                }
            }
        }
    }

    private void add(int amount) {
        int before = total();
        int target = goal();
        String[] types = {"Nước lọc", "Trà", "Nước ngọt", "Sữa"};
        db.add(amount, types[selectedDrink - 1]);
        int after = total();

        // Giữ mở khung thêm nước theo yêu cầu người dùng
        addCardVisible = true;
        if (before < target && after >= target) {
            StreakManager.StreakInfo newStreak = streakManager.getStreakInfo(target);
            showCelebrationDialog(newStreak);
        } else {
            Toast.makeText(this, "Đã thêm " + amount + " ml!", Toast.LENGTH_SHORT).show();
        }
        show();
    }

    private void showCelebrationDialog(StreakManager.StreakInfo info) {
        StreakManager.FlameTier tier = StreakManager.getFlameTier(info.currentStreak);
        boolean isMilestone = StreakManager.isMilestone(info.currentStreak);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        int padding = (int) (22 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // Animated Flame Circle Box (Kích thước to dần theo cấp độ)
        FrameLayout flameContainer = new FrameLayout(this);
        int flameSize = (int) (tier.dialogSizeDp * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(flameSize, flameSize);
        fcLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        fcLp.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);
        flameContainer.setLayoutParams(fcLp);

        android.graphics.drawable.GradientDrawable flameBg = new android.graphics.drawable.GradientDrawable();
        flameBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        flameBg.setColors(new int[]{Color.parseColor(tier.startColor), Color.parseColor(tier.endColor)});
        flameBg.setStroke((int) (3f * getResources().getDisplayMetrics().density), Color.parseColor(tier.strokeColor));
        flameContainer.setBackground(flameBg);

        TextView tvFire = new TextView(this);
        tvFire.setText(tier.icon);
        tvFire.setTextSize(tier.dialogIconSp);
        tvFire.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams fLp = new FrameLayout.LayoutParams(-1, -1);
        fLp.gravity = android.view.Gravity.CENTER;
        tvFire.setLayoutParams(fLp);
        flameContainer.addView(tvFire);
        layout.addView(flameContainer);

        // Animate the flame icon (burst bounce & rotational wobble)
        flameContainer.setScaleX(0.3f);
        flameContainer.setScaleY(0.3f);
        flameContainer.animate()
                .scaleX(tier.scaleMultiplier * 1.15f).scaleY(tier.scaleMultiplier * 1.15f)
                .setDuration(350)
                .withEndAction(() -> {
                    flameContainer.animate().scaleX(tier.scaleMultiplier).scaleY(tier.scaleMultiplier).setDuration(180).start();
                    tvFire.animate().rotation(15f).setDuration(100).withEndAction(() ->
                            tvFire.animate().rotation(-15f).setDuration(100).withEndAction(() ->
                                    tvFire.animate().rotation(0f).setDuration(100).start()
                            ).start()
                    ).start();
                }).start();

        TextView tvTitle = new TextView(this);
        if (isMilestone) {
            tvTitle.setText("🎉 MỞ KHÓA " + tier.name.toUpperCase() + "!");
            tvTitle.setTextColor(Color.parseColor(tier.startColor));
        } else {
            tvTitle.setText("Hoàn thành mục tiêu hôm nay!");
            tvTitle.setTextColor(Color.parseColor("#ff5722"));
        }
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTitle);

        boolean dark = prefs.getBoolean("THEME_DARK", false);
        int primaryColor = dark ? Color.parseColor("#E4F5FF") : Color.parseColor("#183E64");

        TextView tvMsg = new TextView(this);
        StringBuilder sb = new StringBuilder();
        sb.append("\nChuỗi \"").append(streakManager.getStreakName()).append("\" đạt ").append(info.currentStreak).append(" ngày liên tiếp.\n");
        if (isMilestone) {
            sb.append("\n🔥 ").append(tier.effectDescription).append("\n");
        }
        sb.append("\nThú cưng đã đạt Cấp 4: Chiến Binh Nước.\n\nHãy tiếp tục duy trì vào ngày mai nhé.");
        tvMsg.setText(sb.toString());
        tvMsg.setTextSize(14);
        tvMsg.setTextColor(primaryColor);
        tvMsg.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvMsg);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Tuyệt vời", (d, w) -> d.dismiss())
                .create();
        showRoundedDialog(dialog);
    }

    private void showStreakDialog(StreakManager.StreakInfo info) {
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        int primaryColor = dark ? Color.parseColor("#E4F5FF") : Color.parseColor("#183E64");
        int secondaryColor = dark ? Color.parseColor("#A2BFD0") : Color.parseColor("#7393AA");
        int accentColor = dark ? Color.parseColor("#38BDF8") : Color.parseColor("#0284C7");
        int dimColor = dark ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8");

        StreakManager.FlameTier currentTier = StreakManager.getFlameTier(info.currentStreak);
        StreakManager.FlameTier nextTier = StreakManager.getNextFlameTier(info.currentStreak);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        scroll.addView(layout);

        // 1. Tên Chuỗi (gọn gàng, kèm icon đổi tên)
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(android.view.Gravity.CENTER);
        nameRow.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));

        TextView tvStreakName = new TextView(this);
        tvStreakName.setText(streakManager.getStreakName());
        tvStreakName.setTextSize(17);
        tvStreakName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStreakName.setTextColor(primaryColor);
        nameRow.addView(tvStreakName);

        TextView tvEdit = new TextView(this);
        tvEdit.setText(" ✎");
        tvEdit.setTextSize(16);
        tvEdit.setTextColor(Color.parseColor("#FF5722"));
        tvEdit.setPadding((int) (4 * getResources().getDisplayMetrics().density), 0, 0, 0);
        nameRow.addView(tvEdit);
        nameRow.setClickable(true);
        nameRow.setFocusable(true);
        nameRow.setOnClickListener(v -> promptChangeStreakName(info));
        layout.addView(nameRow);

        // 2. Biểu tượng ngọn lửa lớn với kích thước to dần & gradient đổi màu theo cấp
        FrameLayout fireBox = new FrameLayout(this);
        int boxSize = (int) (currentTier.dialogSizeDp * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams fbLp = new LinearLayout.LayoutParams(boxSize, boxSize);
        fbLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        fbLp.bottomMargin = (int) (6 * getResources().getDisplayMetrics().density);
        fireBox.setLayoutParams(fbLp);

        android.graphics.drawable.GradientDrawable fbBg = new android.graphics.drawable.GradientDrawable();
        fbBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        fbBg.setColors(new int[]{Color.parseColor(currentTier.startColor), Color.parseColor(currentTier.endColor)});
        fbBg.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.parseColor(currentTier.strokeColor));
        fireBox.setBackground(fbBg);

        TextView tvFire = new TextView(this);
        tvFire.setText(currentTier.icon);
        tvFire.setTextSize(currentTier.dialogIconSp);
        tvFire.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams fLp = new FrameLayout.LayoutParams(-1, -1);
        fLp.gravity = android.view.Gravity.CENTER;
        tvFire.setLayoutParams(fLp);
        fireBox.addView(tvFire);

        // Hiệu ứng ngọn lửa bùng cháy liên tục (TikTok flame pulsing)
        android.animation.ObjectAnimator fbPulseX = android.animation.ObjectAnimator.ofFloat(tvFire, "scaleX", 1.0f, 1.15f, 1.0f);
        android.animation.ObjectAnimator fbPulseY = android.animation.ObjectAnimator.ofFloat(tvFire, "scaleY", 1.0f, 1.15f, 1.0f);
        fbPulseX.setDuration(900);
        fbPulseY.setDuration(900);
        fbPulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        fbPulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        fbPulseX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        fbPulseY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        fbPulseX.start();
        fbPulseY.start();

        fireBox.setClickable(true);
        fireBox.setFocusable(true);
        fireBox.setOnClickListener(v -> {
            v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            ).start();
            tvFire.animate().rotation(20f).setDuration(100).withEndAction(() ->
                    tvFire.animate().rotation(-20f).setDuration(100).withEndAction(() ->
                            tvFire.animate().rotation(0f).setDuration(100).start()
                    ).start()
            ).start();
            Toast.makeText(this, currentTier.name + " (Cấp " + currentTier.level + ") • " + currentTier.effectDescription, Toast.LENGTH_SHORT).show();
        });
        layout.addView(fireBox);

        TextView tvTierLabel = new TextView(this);
        tvTierLabel.setText(currentTier.name + " • Cấp " + currentTier.level);
        tvTierLabel.setTextSize(13);
        tvTierLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTierLabel.setTextColor(Color.parseColor(currentTier.startColor));
        tvTierLabel.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTierLabel);

        // 3. Số ngày chuỗi to rõ kiểu TikTok
        TextView tvCount = new TextView(this);
        tvCount.setText(String.valueOf(info.currentStreak));
        tvCount.setTextSize(40);
        tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCount.setTextColor(Color.parseColor("#FF5722"));
        tvCount.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvCount);

        TextView tvUnit = new TextView(this);
        tvUnit.setText("NGÀY LIÊN TIẾP");
        tvUnit.setTextSize(13);
        tvUnit.setTypeface(null, android.graphics.Typeface.BOLD);
        tvUnit.setTextColor(primaryColor);
        tvUnit.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvUnit);

        TextView tvBest = new TextView(this);
        tvBest.setText("Kỷ lục: " + info.bestStreak + " ngày");
        tvBest.setTextSize(12);
        tvBest.setTextColor(secondaryColor);
        tvBest.setGravity(android.view.Gravity.CENTER);
        tvBest.setPadding(0, (int) (2 * getResources().getDisplayMetrics().density), 0, (int) (12 * getResources().getDisplayMetrics().density));
        layout.addView(tvBest);

        // 4. Trạng thái 1 câu ngắn gọn kiểu TikTok
        TextView tvStatusPill = new TextView(this);
        tvStatusPill.setGravity(android.view.Gravity.CENTER);
        int pillPadH = (int) (14 * getResources().getDisplayMetrics().density);
        int pillPadV = (int) (8 * getResources().getDisplayMetrics().density);
        tvStatusPill.setPadding(pillPadH, pillPadV, pillPadH, pillPadV);
        tvStatusPill.setTextSize(13);
        tvStatusPill.setTypeface(null, android.graphics.Typeface.BOLD);

        android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
        statusBg.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        if (info.isGoalAchievedToday) {
            tvStatusPill.setText("✓ Đã thắp sáng hôm nay!");
            tvStatusPill.setTextColor(Color.parseColor("#16A34A"));
            statusBg.setColor(Color.parseColor(dark ? "#2616A34A" : "#DCFCE7"));
        } else {
            tvStatusPill.setText("⚡ Còn " + info.remainingMl + " ml để giữ lửa!");
            tvStatusPill.setTextColor(Color.parseColor("#EA580C"));
            statusBg.setColor(Color.parseColor(dark ? "#26EA580C" : "#FFEDD5"));
        }
        tvStatusPill.setBackground(statusBg);
        layout.addView(tvStatusPill);

        // 5. Mốc hiệu ứng ngọn lửa (3, 7, 14, 30 ngày...)
        TextView tvNextMilestone = new TextView(this);
        if (nextTier != null) {
            int remainingDays = nextTier.minDays - info.currentStreak;
            tvNextMilestone.setText("Mốc tiếp theo: " + nextTier.minDays + " ngày (mở khóa " + nextTier.name + " - còn " + remainingDays + " ngày)");
        } else {
            tvNextMilestone.setText("Đã chinh phục mọi hiệu ứng ngọn lửa! 👑");
        }
        tvNextMilestone.setTextSize(12);
        tvNextMilestone.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNextMilestone.setTextColor(accentColor);
        tvNextMilestone.setGravity(android.view.Gravity.CENTER);
        tvNextMilestone.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        layout.addView(tvNextMilestone);

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout milestonesRow = new LinearLayout(this);
        milestonesRow.setOrientation(LinearLayout.HORIZONTAL);
        milestonesRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        milestonesRow.setPadding((int) (4 * getResources().getDisplayMetrics().density), (int) (4 * getResources().getDisplayMetrics().density), (int) (4 * getResources().getDisplayMetrics().density), (int) (10 * getResources().getDisplayMetrics().density));

        for (StreakManager.FlameTier ft : StreakManager.getAllFlameTiers()) {
            if (ft.minDays == 0) continue; // Skip tier 0 in milestones bar
            boolean unlocked = ft.isUnlocked(info.currentStreak);
            LinearLayout pill = new LinearLayout(this);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(android.view.Gravity.CENTER);
            int itemPadH = (int) (12 * getResources().getDisplayMetrics().density);
            int itemPadV = (int) (8 * getResources().getDisplayMetrics().density);
            pill.setPadding(itemPadH, itemPadV, itemPadH, itemPadV);

            android.graphics.drawable.GradientDrawable pillBg = new android.graphics.drawable.GradientDrawable();
            pillBg.setCornerRadius(14 * getResources().getDisplayMetrics().density);
            if (unlocked) {
                pillBg.setColor(Color.parseColor(dark ? "#33" + ft.startColor.substring(1) : "#15" + ft.startColor.substring(1)));
                pillBg.setStroke((int) (1.5f * getResources().getDisplayMetrics().density), Color.parseColor(ft.startColor));
            } else {
                pillBg.setColor(Color.parseColor(dark ? "#1E293B" : "#F1F5F9"));
                pillBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), Color.parseColor(dark ? "#334155" : "#E2E8F0"));
            }
            pill.setBackground(pillBg);

            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-2, -2);
            pLp.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
            pill.setLayoutParams(pLp);

            TextView ic = new TextView(this);
            ic.setText(unlocked ? ft.icon : "🔒");
            ic.setTextSize(unlocked ? (14 + ft.level * 2) : 15);
            ic.setGravity(android.view.Gravity.CENTER);
            pill.addView(ic);

            TextView txt = new TextView(this);
            txt.setText(ft.minDays + " ngày");
            txt.setTextSize(11);
            txt.setTypeface(null, android.graphics.Typeface.BOLD);
            txt.setTextColor(unlocked ? Color.parseColor(ft.startColor) : dimColor);
            txt.setGravity(android.view.Gravity.CENTER);
            pill.addView(txt);

            pill.setClickable(true);
            pill.setFocusable(true);
            pill.setOnClickListener(v -> {
                Toast.makeText(this, ft.minDays + " ngày: " + ft.name + " (" + ft.effectDescription + ")", Toast.LENGTH_SHORT).show();
            });

            milestonesRow.addView(pill);
        }
        hScroll.addView(milestonesRow);
        layout.addView(hScroll);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setNegativeButton("Đổi tên", (d, w) -> promptChangeStreakName(info))
                .setPositiveButton("Đóng", null)
                .create();
        showRoundedDialog(dialog);
    }

    private void promptChangeStreakName(StreakManager.StreakInfo info) {
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        int primaryColor = dark ? Color.parseColor("#E4F5FF") : Color.parseColor("#183E64");
        int secondaryColor = dark ? Color.parseColor("#A2BFD0") : Color.parseColor("#7393AA");

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(streakManager.getStreakName());
        input.setSelection(input.getText().length());
        input.setHint("Nhập tên chuỗi...");
        input.setTextColor(primaryColor);
        input.setHintTextColor(secondaryColor);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);

        TextView tvGuide = new TextView(this);
        tvGuide.setText("Đặt tên cho chuỗi ngày của bạn:");
        tvGuide.setTextColor(secondaryColor);
        tvGuide.setTextSize(13);
        tvGuide.setPadding(0, 0, 0, 10);
        box.addView(tvGuide);
        box.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Đổi tên chuỗi")
                .setView(box)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        streakManager.setStreakName(name);
                        Toast.makeText(this, "Đã lưu tên chuỗi: " + streakManager.getStreakName(), Toast.LENGTH_SHORT).show();
                        show();
                        showStreakDialog(streakManager.getStreakInfo(goal()));
                    }
                })
                .create();
        showRoundedDialog(dialog);
    }

    private void number(String title, int initial, boolean isGoal) {
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        int primaryColor = dark ? Color.parseColor("#E4F5FF") : Color.parseColor("#183E64");
        int secondaryColor = dark ? Color.parseColor("#A2BFD0") : Color.parseColor("#7393AA");

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (initial > 0) input.setText(String.valueOf(initial));
        input.setTextColor(primaryColor);
        input.setHintTextColor(secondaryColor);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(padding, padding, padding, 0);
        box.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(box)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int amount = Integer.parseInt(input.getText().toString().trim());
                if (amount <= 0) throw new NumberFormatException();
                if (isGoal) { config.setWaterGoal(amount); show(); }
                else add(amount);
                dialog.dismiss();
            } catch (NumberFormatException e) { input.setError("Nhập số nguyên lớn hơn 0"); }
        }));
        showRoundedDialog(dialog);
    }

    private void showRoundedDialog(AlertDialog dialog) {
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(getDrawable(R.drawable.bg_card));
            window.getDecorView().setClipToOutline(true);
        }
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        int primaryColor = dark ? Color.parseColor("#E4F5FF") : Color.parseColor("#183E64");
        int accentColor = dark ? Color.parseColor("#38BDF8") : Color.parseColor("#00A6D7");
        int secondaryColor = dark ? Color.parseColor("#A2BFD0") : Color.parseColor("#7393AA");

        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId > 0 && dialog.findViewById(titleId) instanceof TextView) {
            ((TextView) dialog.findViewById(titleId)).setTextColor(primaryColor);
        }
        int messageId = getResources().getIdentifier("message", "id", "android");
        if (messageId > 0 && dialog.findViewById(messageId) instanceof TextView) {
            ((TextView) dialog.findViewById(messageId)).setTextColor(secondaryColor);
        }

        android.widget.Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setTextColor(accentColor);
            posBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        android.widget.Button negBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negBtn != null) {
            negBtn.setTextColor(secondaryColor);
        }
    }

    private void bindHistory() {
        click(R.id.filter_today, () -> { week = false; show(); });
        click(R.id.filter_week, () -> { week = true; show(); });
        findViewById(week ? R.id.filter_week : R.id.filter_today).setSelected(true);

        View cardToday = findViewById(R.id.card_history_today);
        View hint = findViewById(R.id.history_hint);

        if (!week) {
            // Tab Hôm nay: hiện tổng hôm nay và danh sách từng lần uống
            if (cardToday != null) cardToday.setVisibility(View.VISIBLE);
            if (hint != null) hint.setVisibility(View.VISIBLE);
            label(R.id.history_total, total() + " ml");
            bindLogs(false, 0);
        } else {
            // Tab 7 ngày qua: ẩn tổng hôm nay, hiển thị thống kê tổng hợp theo từng ngày
            if (cardToday != null) cardToday.setVisibility(View.GONE);
            if (hint != null) hint.setVisibility(View.GONE);
            bind7DaysDailyHistory();
        }
    }

    private void bind7DaysDailyHistory() {
        LinearLayout list = findViewById(R.id.log_list);
        list.removeAllViews();
        int waterGoal = goal();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String[] dayNames = {"", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

        for (int i = 1; i <= 7; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -i);
            String dateStr = WaterStore.date(cal.getTimeInMillis());
            int dayTotal = db.total(dateStr);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String title = (i == 1) ? "Hôm qua (" + dayFormat.format(cal.getTime()) + ")"
                                    : dayNames[dayOfWeek] + " (" + dayFormat.format(cal.getTime()) + ")";

            View item = getLayoutInflater().inflate(R.layout.item_daily_history, list, false);
            TextView tvDate = item.findViewById(R.id.daily_date);
            TextView tvSub = item.findViewById(R.id.daily_subtext);
            TextView tvAmount = item.findViewById(R.id.daily_amount);
            ProgressBar pb = item.findViewById(R.id.daily_progress);

            tvDate.setText(title);
            tvAmount.setText(dayTotal + " ml");
            pb.setMax(waterGoal);
            pb.setProgress(dayTotal);

            if (dayTotal >= waterGoal) {
                int pct = Math.round(((float) dayTotal / waterGoal) * 100f);
                tvSub.setText("✓ Đạt mục tiêu (" + pct + "%)");
                tvSub.setTextColor(Color.parseColor("#16A34A"));
                tvAmount.setTextColor(Color.parseColor("#16A34A"));
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A")));
            } else if (dayTotal > 0) {
                int pct = Math.round(((float) dayTotal / waterGoal) * 100f);
                tvSub.setText("Đạt " + pct + "% mục tiêu (" + dayTotal + "/" + waterGoal + " ml)");
            } else {
                tvSub.setText("Chưa có bản ghi uống nước");
                tvAmount.setTextColor(Color.parseColor("#94A3B8"));
            }

            list.addView(item);
        }
    }

    private void bindLogs(boolean range, int limit) {
        LinearLayout list = findViewById(R.id.log_list);
        try (Cursor cursor = db.logs(range)) {
            int count = 0;
            while (cursor.moveToNext() && (limit == 0 || count < limit)) {
                count++;
                long id = cursor.getLong(0);
                View item = getLayoutInflater().inflate(R.layout.item_water_log, list, false);
                ((TextView) item.findViewById(R.id.log_amount)).setText(cursor.getInt(1) + " ml • " + cursor.getString(2));
                ((TextView) item.findViewById(R.id.log_time)).setText(new SimpleDateFormat(range ? "dd/MM – HH:mm" : "HH:mm",
                        Locale.getDefault()).format(new Date(cursor.getLong(3))));
                int amount = cursor.getInt(1);
                String type = cursor.getString(2);
                item.findViewById(R.id.log_edit).setOnClickListener(v -> {
                    ((SwipeLogLayout) item).close(); editLog(id, amount, type);
                });
                item.findViewById(R.id.log_delete).setOnClickListener(v -> {
                    ((SwipeLogLayout) item).close();
                    showRoundedDialog(new AlertDialog.Builder(this).setTitle("Xóa lần uống này?").setNegativeButton("Hủy", null)
                            .setPositiveButton("Xóa", (d, i) -> { db.remove(id); show(); }).create());
                });
                list.addView(item);
            }
            if (count == 0) getLayoutInflater().inflate(R.layout.view_empty_logs, list, true);
        }
    }

    private void editLog(long id, int amount, String type) {
        String[] names = {"Nước lọc", "Trà / Cà phê", "Nước ngọt", "Sữa"};
        String[] types = {"Nước lọc", "Trà", "Nước ngọt", "Sữa"};
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, padding, padding, 0);
        Spinner drink = new Spinner(this, Spinner.MODE_DROPDOWN);
        drink.setPopupBackgroundDrawable(getDrawable(R.drawable.bg_card));
        drink.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        for (int i = 0; i < names.length; i++) if (names[i].equals(type)) drink.setSelection(i);
        form.addView(drink);
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Lượng nước (ml)"); input.setText(String.valueOf(amount));
        form.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Chỉnh sửa bản ghi").setView(form)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int value = Integer.parseInt(input.getText().toString().trim());
                if (value <= 0) throw new NumberFormatException();
                db.edit(id, types[drink.getSelectedItemPosition()], value);
                dialog.dismiss(); show();
            } catch (NumberFormatException e) { input.setError("Nhập số nguyên lớn hơn 0"); }
        }));
        showRoundedDialog(dialog);
    }

    private void bindStats() {
        click(R.id.stats_week, () -> { month = false; show(); });
        click(R.id.stats_month, () -> { month = true; show(); });
        int days = month ? 30 : 7, sum = 0, success = 0;
        int[] values = new int[days]; String[] labels = new String[days];
        Calendar day = Calendar.getInstance(); day.add(Calendar.DATE, -days + 1);
        for (int i = 0; i < days; i++) {
            values[i] = db.total(WaterStore.date(day.getTimeInMillis())); sum += values[i];
            if (values[i] >= goal()) success++;
            if (!month) {
                labels[i] = getDayOfWeekLabel(day.get(Calendar.DAY_OF_WEEK));
            } else {
                labels[i] = new SimpleDateFormat("dd/MM", Locale.US).format(day.getTime());
            }
            day.add(Calendar.DATE, 1);
        }
        ((WaterChartView) findViewById(R.id.water_chart)).setData(values, labels, goal());
        label(R.id.chart_goal, "Đường nét đứt: mục tiêu " + goal() + " ml");

        
        label(R.id.stats_average, "Trung bình " + sum / days + " ml / ngày");
        label(R.id.stats_success, "Đạt mục tiêu " + success + " / " + days + " ngày");
        label(R.id.stats_total, "Tổng lượng nước " + sum + " ml");
    }

    private String getDayOfWeekLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "T2";
            case Calendar.TUESDAY: return "T3";
            case Calendar.WEDNESDAY: return "T4";
            case Calendar.THURSDAY: return "T5";
            case Calendar.FRIDAY: return "T6";
            case Calendar.SATURDAY: return "T7";
            case Calendar.SUNDAY: return "CN";
            default: return "";
        }
    }

    // Xử lý M2.1: Tiến độ uống nước & Tiến hóa linh vật giọt nước 4 cấp độ
    private void updateProgressAndMascot(int currentWater, int waterGoal) {
        int remaining = Math.max(0, waterGoal - currentWater);
        float percentage = ((float) currentWater / Math.max(1, waterGoal)) * 100f;

        int level;
        String levelName;
        String levelDesc;

        // 4 cấp độ tiến hóa: <30% (Lv.1), <70% (Lv.2), <100% (Lv.3), >=100% (Lv.4)
        if (percentage < 30f) {
            level = 0;
            levelName = "Mầm Nước (Cấp 1)";
            levelDesc = "Cấp 1 • Giai đoạn mầm nước (" + Math.round(percentage) + "%) • Còn: " + remaining + " ml";
        } else if (percentage < 70f) {
            level = 1;
            levelName = "Bé Nước (Cấp 2)";
            levelDesc = "Cấp 2 • Giai đoạn bé nước (" + Math.round(percentage) + "%) • Còn: " + remaining + " ml";
        } else if (percentage < 100f) {
            level = 2;
            levelName = "Giọt Nước (Cấp 3)";
            levelDesc = "Cấp 3 • Giai đoạn giọt nước (" + Math.round(percentage) + "%) • Còn: " + remaining + " ml";
        } else {
            level = 3;
            levelName = "Chiến Binh Nước (Cấp 4)";
            levelDesc = "Cấp 4 • Đạt 100% mục tiêu hôm nay (" + Math.round(percentage) + "%)";
        }

        label(R.id.pet_name, levelName);
        label(R.id.pet_level, levelDesc);

        ProgressBar progress = findViewById(R.id.pet_progress);
        if (progress != null) {
            progress.setMax(waterGoal);
            progress.setProgress(currentWater);
        }

        PetView petView = findViewById(R.id.pet_mascot);
        if (petView != null) {
            petView.setLevel(level);
        }
    }

    private void bindPet() {
        updateProgressAndMascot(total(), goal());

        // Mascot touch interactive dialogue
        PetView petView = findViewById(R.id.pet_mascot);
        if (petView != null) {
            petView.setOnPetClickListener(lv -> {
                String dialogue;
                switch (lv) {
                    case 1: dialogue = "Tớ là Bé Nước, tớ biết đi rồi nè!"; break;
                    case 2: dialogue = "Tớ là Giọt Nước, sắp về đích rồi cố lên bạn ơi!"; break;
                    case 3: dialogue = "Chiến Binh Nước! Cảm ơn bạn đã hoàn thành xuất sắc hôm nay."; break;
                    default: dialogue = "Tớ là Mầm Nước, hãy bổ sung thêm nước nhé."; break;
                }
                TextView tvDiag = findViewById(R.id.pet_dialogue);
                if (tvDiag != null) {
                    tvDiag.setText(dialogue);
                }
                Toast.makeText(this, dialogue, Toast.LENGTH_SHORT).show();
            });
        }

        // Streak status in Pet Screen
        StreakManager.StreakInfo petStreak = streakManager.getStreakInfo(goal());
        StreakManager.FlameTier petFlame = StreakManager.getFlameTier(petStreak.currentStreak);
        label(R.id.pet_streak_icon, petFlame.icon);
        label(R.id.pet_streak_title, streakManager.getStreakName() + " • " + petStreak.currentStreak + " ngày (" + petFlame.name + ")");
        label(R.id.pet_streak_desc, petStreak.isGoalAchievedToday ? "✓ Đã thắp sáng hôm nay" : "⚡ Còn " + petStreak.remainingMl + " ml để giữ lửa");
        click(R.id.pet_streak_card, () -> showStreakDialog(petStreak));

        // 4 Evolution Levels
        click(R.id.pet_small, () -> previewPetLevel(0, "Cấp 1: Mầm Nước (< 30% mục tiêu)"));
        click(R.id.pet_growing, () -> previewPetLevel(1, "Cấp 2: Bé Nước (30% - 69% mục tiêu)"));
        click(R.id.pet_healthy, () -> previewPetLevel(2, "Cấp 3: Giọt Nước (70% - 99% mục tiêu)"));
        click(R.id.pet_bright, () -> previewPetLevel(3, "Cấp 4: Chiến Binh Nước (>= 100% mục tiêu)"));
    }

    private void previewPetLevel(int previewLevel, String msg) {
        PetView pv = findViewById(R.id.pet_mascot);
        if (pv != null) {
            pv.setLevel(previewLevel);
            pv.triggerTapAnimation(pv.getWidth() / 2f, pv.getHeight() / 2f);
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void bindReminders() {
        Switch reminder = findViewById(R.id.reminder_switch);
        View intervalCard = findViewById(R.id.card_interval);
        boolean isOn = prefs.getBoolean("REMINDER_IS_ON", false);
        if (reminder != null) {
            reminder.setOnCheckedChangeListener(null);
            reminder.setChecked(isOn);
            reminder.setOnCheckedChangeListener((button, on) -> {
                if (on && Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                    reminder.setChecked(false); requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10); return;
                }
                config.setReminderOn(on);
                if (intervalCard != null) {
                    intervalCard.setVisibility(on ? View.VISIBLE : View.GONE);
                }
                if (on) checkReminderPermissions();
            });
        }
        if (intervalCard != null) {
            intervalCard.setVisibility(isOn ? View.VISIBLE : View.GONE);
        }
        bindInterval();
    }

    private void checkReminderPermissions() {
        if (!ReminderReceiver.notificationsAllowed(this)) {
            Intent settings = Build.VERSION.SDK_INT >= 26
                    ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            offerPermissionSettings("Thông báo đang bị chặn", "Cho phép Aqua gửi thông báo trong cài đặt hệ thống.", settings);
        } else if (Build.VERSION.SDK_INT >= 31 && !ReminderReceiver.exactAlarmsAllowed(this)) {
            offerPermissionSettings("Nhắc nhở đúng giờ", "Cho phép báo thức chính xác để nhắc đúng giờ. Nếu bỏ qua, Android có thể gửi nhắc muộn hơn.",
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
        }
    }

    private void offerPermissionSettings(String title, String message, Intent settings) {
        showRoundedDialog(new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setNegativeButton("Để sau", null).setPositiveButton("Mở cài đặt", (dialog, which) -> {
                    try { startActivity(settings); }
                    catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(this, "Mở Cài đặt hệ thống > Ứng dụng > Aqua để cấp quyền", Toast.LENGTH_LONG).show();
                    }
                }).create());
    }

    private void bindInterval() {
        label(R.id.interval_current, "Đang chọn: " + prefs.getInt("REMINDER_INTERVAL", 60) + " phút");
        int[] ids = {R.id.interval_60, R.id.interval_90, R.id.interval_120};
        int[] minutes = {60, 90, 120};
        for (int i = 0; i < ids.length; i++) {
            final int value = minutes[i];
            click(ids[i], () -> { config.setReminderInterval(value); show(); });
        }
    }

    private void bindSettings() {
        label(R.id.goal_button, goal() + " ml ›");
        click(R.id.goal_button, () -> number("Mục tiêu hàng ngày (ml)", goal(), true));

        // Switch chế độ tối ở cuối (ngay trên Aqua)
        Switch theme = findViewById(R.id.theme_switch);
        if (theme != null) {
            theme.setOnCheckedChangeListener(null);
            theme.setChecked(prefs.getBoolean("THEME_DARK", false));
            theme.setOnCheckedChangeListener((button, dark) -> {
                config.setThemeDark(dark);
                recreate();
            });
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != 10) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            config.setReminderOn(true);
            checkReminderPermissions();
        } else {
            config.setReminderOn(false);
            checkReminderPermissions();
        }
        show();
    }
}
