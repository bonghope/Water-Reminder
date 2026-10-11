package com.example.waterreminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.waterreminder.models.WaterLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WaterReminder.db";
    // Never lower this version after a release; add migrations for future schema changes.
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_WATER_LOG = "WaterLog";
    public static final String TABLE_NAME = TABLE_WATER_LOG;

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_RAW_AMOUNT = "raw_amount";
    public static final String COLUMN_AMOUNT = "amount_ml";
    public static final String COLUMN_HYDRATION_AMOUNT = "hydration_amount";
    public static final String COLUMN_DRINK_TYPE = "drink_type";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DATE_STRING = "date_string";
    public static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_WATER_LOG + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CATEGORY_ID + " INTEGER, " +
                COLUMN_RAW_AMOUNT + " INTEGER, " +
                COLUMN_AMOUNT + " INTEGER, " +
                COLUMN_HYDRATION_AMOUNT + " INTEGER, " +
                COLUMN_DRINK_TYPE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_DATE_STRING + " TEXT, " +
                COLUMN_CREATED_AT + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            java.util.Set<String> columns = new java.util.HashSet<>();
            try (Cursor cursor = db.rawQuery("PRAGMA table_info(WaterLog)", null)) {
                while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            if (columns.isEmpty()) {
                onCreate(db);
                return;
            }
            addColumnIfMissing(db, columns, "category_id", "INTEGER");
            addColumnIfMissing(db, columns, "raw_amount", "INTEGER");
            addColumnIfMissing(db, columns, "amount_ml", "INTEGER");
            addColumnIfMissing(db, columns, "hydration_amount", "INTEGER");
            addColumnIfMissing(db, columns, "drink_type", "TEXT");
            addColumnIfMissing(db, columns, "timestamp", "INTEGER");
            addColumnIfMissing(db, columns, "date_string", "TEXT");
            addColumnIfMissing(db, columns, "created_at", "TEXT");

            // Preserve existing values and fill the fields absent in older releases.
            db.execSQL("UPDATE WaterLog SET raw_amount = COALESCE(raw_amount, amount_ml, 0)");
            db.execSQL("UPDATE WaterLog SET amount_ml = COALESCE(amount_ml, raw_amount)");
            db.execSQL("UPDATE WaterLog SET category_id = COALESCE(category_id, CASE drink_type WHEN 'Trà' THEN 2 WHEN 'Cà phê' THEN 2 WHEN 'Trà / Cà phê' THEN 2 WHEN 'Trà / Cafe' THEN 2 WHEN 'Nước ngọt' THEN 3 WHEN 'Sữa' THEN 4 ELSE 1 END)");
            db.execSQL("UPDATE WaterLog SET hydration_amount = COALESCE(hydration_amount, CASE category_id WHEN 2 THEN CAST(raw_amount * 0.8 AS INTEGER) WHEN 3 THEN CAST(raw_amount * 0.7 AS INTEGER) ELSE raw_amount END)");
            db.execSQL("UPDATE WaterLog SET drink_type = COALESCE(drink_type, CASE category_id WHEN 2 THEN 'Trà / Cà phê' WHEN 3 THEN 'Nước ngọt' WHEN 4 THEN 'Sữa' ELSE 'Nước lọc' END)");
            db.execSQL("UPDATE WaterLog SET created_at = COALESCE(created_at, datetime(timestamp / 1000, 'unixepoch', 'localtime'))");
        }
    }

    private void addColumnIfMissing(SQLiteDatabase db, java.util.Set<String> columns, String name, String type) {
        if (!columns.contains(name)) {
            db.execSQL("ALTER TABLE " + TABLE_WATER_LOG + " ADD COLUMN " + name + " " + type);
            columns.add(name);
        }
    }

    // Hàm gọi tổng nước uống hôm nay cho M2.1 (gộp từ nhacuongnuoc)
    public int getWaterToday(String dateString) {
        int totalWater = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_HYDRATION_AMOUNT + ") FROM " + TABLE_WATER_LOG + " WHERE " + COLUMN_DATE_STRING + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{dateString});

        if (cursor.moveToFirst()) {
            totalWater = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return totalWater;
    }

    // Hàm gọi tổng lượng nước thô (ml) theo ngày
    public int getRawWaterToday(String dateString) {
        int totalWater = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_RAW_AMOUNT + ") FROM " + TABLE_WATER_LOG + " WHERE " + COLUMN_DATE_STRING + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{dateString});

        if (cursor.moveToFirst()) {
            totalWater = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return totalWater;
    }

    public long insertLog(WaterLog log, String dateString) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_ID, log.getCategoryId());
        values.put(COLUMN_RAW_AMOUNT, log.getRawAmount());
        values.put(COLUMN_AMOUNT, log.getRawAmount());
        values.put(COLUMN_HYDRATION_AMOUNT, log.getHydrationAmount());
        values.put(COLUMN_DRINK_TYPE, getDrinkTypeName(log.getCategoryId()));
        values.put(COLUMN_TIMESTAMP, log.getTimestamp());
        values.put(COLUMN_DATE_STRING, dateString);
        values.put(COLUMN_CREATED_AT, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(log.getTimestamp())));

        long id = db.insert(TABLE_WATER_LOG, null, values);
        db.close();
        return id;
    }

    private String getDrinkTypeName(int categoryId) {
        switch (categoryId) {
            case 2: return "Trà / Cà phê";
            case 3: return "Nước ngọt";
            case 4: return "Sữa";
            default: return "Nước lọc";
        }
    }

    public long insertLog(int amount, String dateString) {
        long time = System.currentTimeMillis();
        WaterLog log = new WaterLog(1, amount, amount, time);
        return insertLog(log, dateString);
    }

    public List<WaterLog> getLogsByDate(String dateString) {
        List<WaterLog> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WATER_LOG, null,
                COLUMN_DATE_STRING + "=?", new String[]{dateString},
                null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID));
                int rawAmount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RAW_AMOUNT));
                int hydrationAmount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HYDRATION_AMOUNT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                WaterLog log = new WaterLog(categoryId, rawAmount, hydrationAmount, timestamp);
                log.setId(id);
                list.add(log);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void updateLog(int id, int categoryId, int rawAmount, int hydrationAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_ID, categoryId);
        values.put(COLUMN_RAW_AMOUNT, rawAmount);
        values.put(COLUMN_AMOUNT, rawAmount);
        values.put(COLUMN_HYDRATION_AMOUNT, hydrationAmount);
        values.put(COLUMN_DRINK_TYPE, getDrinkTypeName(categoryId));

        db.update(TABLE_WATER_LOG, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteLog(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WATER_LOG, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}