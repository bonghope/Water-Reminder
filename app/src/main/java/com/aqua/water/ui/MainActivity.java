package com.aqua.water.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import com.aqua.water.R;
import com.aqua.water.database.WaterStore;
import com.aqua.water.services.ReminderReceiver;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Binds the XML screens to local data and user actions. */
public class MainActivity extends Activity {
    private WaterStore db;
    private SharedPreferences prefs;
    private WaterView waterView;
    private boolean resumed, week, month;
    private int tab;
    private int selectedDrink = 1;
    private boolean addCardVisible = true;
    private final int[] screens = {R.layout.screen_home, R.layout.screen_history, R.layout.screen_stats,
            R.layout.screen_pet, R.layout.screen_reminders, R.layout.screen_settings};
    private final int[] navigation = {R.id.nav_0, R.id.nav_1, R.id.nav_2, R.id.nav_3, R.id.nav_4, R.id.nav_5};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new WaterStore(this);
        prefs = getSharedPreferences("AppConfig", 0);
        if (state != null) {
            tab = Math.max(0, Math.min(screens.length - 1, state.getInt("tab", 0)));
            week = state.getBoolean("week", false);
            month = state.getBoolean("month", false);
            selectedDrink = Math.max(1, Math.min(4, state.getInt("selectedDrink", 1)));
            addCardVisible = state.getBoolean("addCardVisible", true);
        }
        show();
    }

    @Override protected void onResume() { super.onResume(); resumed = true; show(); }
    @Override protected void onPause() {
        resumed = false;
        if (waterView != null) waterView.setActive(false);
        super.onPause();
    }
    @Override protected void onDestroy() { db.close(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", tab); state.putBoolean("week", week); state.putBoolean("month", month);
        state.putInt("selectedDrink", selectedDrink);
        state.putBoolean("addCardVisible", addCardVisible);
    }

    private int goal() { return Math.max(1, prefs.getInt("WATER_GOAL", 2000)); }
    private int total() { return db.total(WaterStore.date(System.currentTimeMillis())); }
    private void label(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    private void click(int id, Runnable action) { findViewById(id).setOnClickListener(v -> action.run()); }

    private void show() {
        WaterView previousWater = waterView;
        waterView = null;
        setContentView(R.layout.activity_main);
        findViewById(R.id.content_frame).setOnApplyWindowInsetsListener((view, insets) -> {
            int top = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
            } else {
                top = insets.getSystemWindowInsetTop();
            }
            view.setPadding(view.getPaddingLeft(), top, view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewGroup root = findViewById(R.id.content_frame);
        root.removeAllViews();
        getLayoutInflater().inflate(screens[tab], root, true);
        getLayoutInflater().inflate(R.layout.view_navigation, root, true);
        WaterView inflatedWater = findViewById(R.id.water_view);
        if (inflatedWater != null) {
            waterView = inflatedWater;
            if (previousWater != null) {
                waterView.copyFrom(previousWater);
            }
            waterView.setData(total(), goal());
            waterView.setActive(resumed);
        } else if (inflatedWater != null) inflatedWater.setVisibility(View.GONE);
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
            tab = 0;
            addCardVisible = true;
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
        label(R.id.home_remaining, "Còn " + remaining + " ml để đạt mục tiêu");
        int[] drinkIds = {R.id.ivWater, R.id.ivTea, R.id.ivSweet, R.id.ivMilk};
        for (int i = 0; i < drinkIds.length; i++) {
            final int category = i + 1;
            click(drinkIds[i], () -> { selectedDrink = category; updateDrinkSelection(); });
        }
        updateDrinkSelection();
        click(R.id.btn100ml, () -> add(100)); click(R.id.btn200ml, () -> add(200));
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
        View card = findViewById(R.id.cardAddWater);
        View reopen = findViewById(R.id.btnReopenCard);
        if (card != null && reopen != null) {
            card.setVisibility(addCardVisible ? View.VISIBLE : View.GONE);
            reopen.setVisibility(addCardVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void add(int amount) {
        String[] types = {"Nước lọc", "Trà", "Nước ngọt", "Sữa"};
        db.add(amount, types[selectedDrink - 1]);
        if (waterView != null) waterView.animateDrink();
        show();
    }

    private void number(String title, int initial, boolean goal) {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (initial > 0) input.setText(String.valueOf(initial));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(padding, padding, padding, 0);
        box.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(box)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int value = Integer.parseInt(input.getText().toString().trim());
                if (value <= 0) throw new NumberFormatException();
                if (goal) prefs.edit().putInt("WATER_GOAL", value).apply(); else add(value);
                dialog.dismiss(); show();
            } catch (NumberFormatException e) { input.setError("Nhập số nguyên lớn hơn 0"); }
        }));
        showRoundedDialog(dialog);
    }

    private void showRoundedDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_card);
        }
        dialog.show();
    }

    private void bindHistory() {
        click(R.id.filter_today, () -> { week = false; show(); });
        click(R.id.filter_week, () -> { week = true; show(); });
        findViewById(week ? R.id.filter_week : R.id.filter_today).setSelected(true);
        label(R.id.history_total, total() + " ml"); bindLogs(week, 0);
    }

    private void bindLogs(boolean range, int limit) {
        LinearLayout list = findViewById(R.id.log_list);
        try (Cursor cursor = db.logs(range)) {
            int count = 0;
            while (cursor.moveToNext() && (limit == 0 || count < limit)) {
                count++;
                long id = cursor.getLong(0);
                View item = getLayoutInflater().inflate(R.layout.item_water_log, list, false);
                ((TextView) item.findViewById(R.id.log_amount)).setText("💧 " + cursor.getInt(1) + " ml • " + cursor.getString(2));
                ((TextView) item.findViewById(R.id.log_time)).setText(new SimpleDateFormat(range ? "dd/MM · HH:mm" : "HH:mm",
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

    // Xử lý M2.1: Tiến độ uống nước & Tiến hóa linh vật giọt nước (Tích hợp logic từ nhacuongnuoc)
    private void updateProgressAndMascot(int currentWater, int waterGoal) {
        int remaining = Math.max(0, waterGoal - currentWater);
        float percentage = ((float) currentWater / Math.max(1, waterGoal)) * 100f;

        int level;
        String levelName;
        String levelDesc;

        // Logic tiến hóa giọt nước theo 4 mốc phần trăm: <30%, <70%, <100%, >=100%
        if (percentage < 30f) {
            level = 0;
            levelName = "Giọt nước nhỏ (Lv.1)";
            levelDesc = "Cấp 1 • Mới khởi đầu (" + Math.round(percentage) + "%) • Còn lại: " + remaining + " ml";
        } else if (percentage < 70f) {
            level = 1;
            levelName = "Giọt nước vui vẻ (Lv.2)";
            levelDesc = "Cấp 2 • Đang lớn (" + Math.round(percentage) + "%) • Còn lại: " + remaining + " ml";
        } else if (percentage < 100f) {
            level = 2;
            levelName = "Giọt nước khỏe mạnh (Lv.3)";
            levelDesc = "Cấp 3 • Sắp về đích (" + Math.round(percentage) + "%) • Còn lại: " + remaining + " ml";
        } else {
            level = 3;
            levelName = "Giọt nước rực rỡ (Lv.4)";
            levelDesc = "Cấp 4 • Đạt mục tiêu (" + Math.round(percentage) + "%) • Chúc mừng bạn 🎉";
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
        click(R.id.pet_small, () -> Toast.makeText(this, "Lv.1: Dưới 30% mục tiêu", Toast.LENGTH_SHORT).show());
        click(R.id.pet_healthy, () -> Toast.makeText(this, "Lv.3: Đạt từ 70% đến 99% mục tiêu", Toast.LENGTH_SHORT).show());
        click(R.id.pet_bright, () -> Toast.makeText(this, "Lv.4: Hoàn thành 100% mục tiêu ngày", Toast.LENGTH_SHORT).show());
    }

    private void bindReminders() {
        Switch reminder = findViewById(R.id.reminder_switch);
        reminder.setChecked(prefs.getBoolean("REMINDER_IS_ON", false));
        reminder.setOnCheckedChangeListener((button, on) -> {
            if (on && Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                reminder.setChecked(false); requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10); return;
            }
            prefs.edit().putBoolean("REMINDER_IS_ON", on).apply(); ReminderReceiver.schedule(this);
        });
        bindInterval();
    }

    private void bindInterval() {
        label(R.id.interval_current, "Đang chọn: " + prefs.getInt("REMINDER_INTERVAL", 60) + " phút");
        int[] ids = {R.id.interval_60, R.id.interval_90, R.id.interval_120};
        int[] minutes = {60, 90, 120};
        for (int i = 0; i < ids.length; i++) {
            final int value = minutes[i];
            click(ids[i], () -> { prefs.edit().putInt("REMINDER_INTERVAL", value).apply(); ReminderReceiver.schedule(this); show(); });
        }
    }

    private void bindSettings() {
        label(R.id.goal_button, goal() + " ml ›");
        click(R.id.goal_button, () -> number("Mục tiêu hàng ngày (ml)", goal(), true));
        Switch theme = findViewById(R.id.theme_switch); theme.setChecked(prefs.getBoolean("THEME_DARK", false));
        theme.setOnCheckedChangeListener((button, dark) -> { prefs.edit().putBoolean("THEME_DARK", dark).apply(); show(); });
        bindInterval();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != 10) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean("REMINDER_IS_ON", true).apply(); ReminderReceiver.schedule(this);
        } else Toast.makeText(this, "Cần quyền thông báo để bật nhắc nhở", Toast.LENGTH_SHORT).show();
        show();
    }
}