package com.example.waterreminder.database;

import com.example.waterreminder.models.WaterLog;
import java.util.ArrayList;
import java.util.List;

public class MockDatabase {
    // List tĩnh lưu dữ liệu tạm trên RAM (tắt app sẽ mất)
    private static List<WaterLog> fakeLogs = new ArrayList<>();

    // Tạo sẵn 2 bản ghi để test UI
    static {
        long now = System.currentTimeMillis();
        fakeLogs.add(new WaterLog(1, 200, 200, now - 3600000)); // Uống nước lọc 1 tiếng trước
        fakeLogs.add(new WaterLog(2, 300, 240, now - 7200000)); // Uống trà 2 tiếng trước
    }

    // Hàm lấy danh sách lịch sử
    public static List<WaterLog> getLogs() {
        return fakeLogs;
    }

    // Hàm thêm nước (chèn vào đầu danh sách để hiện lên trên cùng)
    public static void addLog(WaterLog log) {
        fakeLogs.add(0, log);
    }
    // Hàm xóa bản ghi mới nhất (dùng cho tính năng Hoàn tác)
    public static void removeLastLog() {
        if (!fakeLogs.isEmpty()) {
            fakeLogs.remove(0); // Xóa phần tử ở vị trí đầu tiên
        }
    }
}