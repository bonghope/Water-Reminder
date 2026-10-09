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
        if (previousWater != null) previousWater.setActive(false);
        boolean dark = prefs.getBoolean("THEME_DARK", false);
        setTheme(dark ? R.style.Theme_Aqua_Dark : R.style.Theme_Aqua);
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
        FrameLayout content = findViewById(R.id.screen_container);
        getLayoutInflater().inflate(screens[tab], content, true);
        label(R.id.header_date, new SimpleDateFormat("dd MMM", new Locale("vi")).format(new Date()));
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
        label(R.id.home_percent, Math.round(amount * 100f / goal()) + "%");
        label(R.id.home_amount, amount + " / " + goal() + " ml");
        label(R.id.home_remaining, "Còn " + Math.max(0, goal() - amount) + " ml để đạt mục tiêu");
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
        boolean visible = tab == 0 && addCardVisible;
        findViewById(R.id.add_water_overlay).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.cardAddWater).setVisibility(addCardVisible ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnReopenCard).setVisibility(View.GONE);
    }

    private void add(int amount) {
        String type = new String[]{"Nước lọc", "Trà", "Nước ngọt", "Sữa"}[selectedDrink - 1];
        db.add(amount, type); show();
        Toast.makeText(this, "Đã thêm " + amount + " ml 💧", Toast.LENGTH_SHORT).show();
    }

    private void number(String title, int value, boolean isGoal) {
        EditText input = (EditText) getLayoutInflater().inflate(R.layout.dialog_amount, null);
        if (value > 0) input.setText(String.valueOf(value));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int amount = Integer.parseInt(input.getText().toString().trim());
                if (amount <= 0) throw new NumberFormatException();
                if (isGoal) { prefs.edit().putInt("WATER_GOAL", amount).apply(); show(); }
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
            labels[i] = new SimpleDateFormat("dd/MM", Locale.US).format(day.getTime()); day.add(Calendar.DATE, 1);
        }
        ((WaterChartView) findViewById(R.id.water_chart)).setData(values, labels, goal());
        label(R.id.chart_goal, "Đường nét đứt: mục tiêu " + goal() + " ml");
        label(R.id.stats_average, "Trung bình " + sum / days + " ml / ngày");
        label(R.id.stats_success, "Đạt mục tiêu " + success + " / " + days + " ngày");
        label(R.id.stats_total, "Tổng lượng nước " + sum + " ml");
    }

    private void bindPet() {
        int level = Math.min(3, total() / Math.max(1, goal() / 3));
        label(R.id.pet_name, new String[]{"Giọt nước nhỏ", "Giọt nước vui vẻ", "Giọt nước khỏe mạnh", "Giọt nước rực rỡ"}[level]);
        label(R.id.pet_level, "Cấp " + (level + 1) + " • Tiến hóa theo tiến độ hôm nay");
        ProgressBar progress = findViewById(R.id.pet_progress); progress.setMax(goal()); progress.setProgress(total());
        Runnable tip = () -> Toast.makeText(this, "Uống nước để tiến hóa bạn nhỏ!", Toast.LENGTH_SHORT).show();
        click(R.id.pet_small, tip); click(R.id.pet_healthy, tip); click(R.id.pet_bright, tip);
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
