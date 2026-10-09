package com.example.waterreminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.waterreminder.models.WaterLog;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WaterReminder.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_WATER_LOG = "WaterLog";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_RAW_AMOUNT = "raw_amount";
    public static final String COLUMN_HYDRATION_AMOUNT = "hydration_amount";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DATE_STRING = "date_string";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_WATER_LOG + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CATEGORY_ID + " INTEGER, " +
                COLUMN_RAW_AMOUNT + " INTEGER, " +
                COLUMN_HYDRATION_AMOUNT + " INTEGER, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_DATE_STRING + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WATER_LOG);
        onCreate(db);
    }

    public long insertLog(WaterLog log, String dateString) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_ID, log.getCategoryId());
        values.put(COLUMN_RAW_AMOUNT, log.getRawAmount());
        values.put(COLUMN_HYDRATION_AMOUNT, log.getHydrationAmount());
        values.put(COLUMN_TIMESTAMP, log.getTimestamp());
        values.put(COLUMN_DATE_STRING, dateString);

        long id = db.insert(TABLE_WATER_LOG, null, values);
        db.close();
        return id;
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
        values.put(COLUMN_HYDRATION_AMOUNT, hydrationAmount);

        db.update(TABLE_WATER_LOG, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteLog(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WATER_LOG, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}