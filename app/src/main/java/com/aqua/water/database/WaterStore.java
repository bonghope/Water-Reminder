package com.aqua.water.database;

import android.content.*;
import android.database.MatrixCursor;
import com.example.waterreminder.database.DatabaseHelper;
import com.example.waterreminder.models.WaterLog;
import android.database.Cursor;
import android.database.sqlite.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WaterStore {
    private final DatabaseHelper backend;
    public WaterStore(Context context) { backend = new DatabaseHelper(context); }
    public static String date(long time) { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time)); }
    private static int category(String type) {
        if ("Trà".equals(type) || "Cà phê".equals(type)) return 2;
        if ("Nước ngọt".equals(type)) return 3;
        if ("Sữa".equals(type)) return 4;
        return 1;
    }
    private static String type(int category) {
        switch (category) {
            case 2: return "Trà / Cà phê";
            case 3: return "Nước ngọt";
            case 4: return "Sữa";
            default: return "Nước lọc";
        }
    }
    private static int hydration(int amount, int category) {
        return category == 2 ? (int) (amount * 0.8) : category == 3 ? (int) (amount * 0.7) : amount;
    }
    public void add(int amount, String type) {
        long time = System.currentTimeMillis(); int category = category(type);
        if (backend.insertLog(new WaterLog(category, amount, hydration(amount, category), time), date(time)) == -1)
            throw new android.database.SQLException("Không thể lưu bản ghi");
    }
    public int total(String date) {
        return backend.getWaterToday(date);
    }
    public Cursor logs(boolean week) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"id", "amount_ml", "drink_type", "timestamp"});
        Calendar day = Calendar.getInstance();
        for (int i = 0; i < (week ? 7 : 1); i++) {
            for (WaterLog log : backend.getLogsByDate(date(day.getTimeInMillis())))
                cursor.addRow(new Object[]{log.getId(), log.getRawAmount(), type(log.getCategoryId()), log.getTimestamp()});
            day.add(Calendar.DATE, -1);
        }
        return cursor;
    }
    public void remove(long id) { backend.deleteLog(Math.toIntExact(id)); }
    public void edit(long id, String type, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int category = category(type);
        backend.updateLog(Math.toIntExact(id), category, amount, hydration(amount, category));
    }
    public void close() { backend.close(); }
}