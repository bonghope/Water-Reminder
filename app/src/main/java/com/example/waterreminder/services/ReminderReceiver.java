package com.example.waterreminder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Chuyển tiếp tới com.aqua.water.services.ReminderReceiver để hiển thị notification & đặt lịch tiếp theo
        com.aqua.water.services.ReminderReceiver receiver = new com.aqua.water.services.ReminderReceiver();
        receiver.onReceive(context, intent);
    }

    public static void schedule(Context context) {
        com.aqua.water.services.ReminderReceiver.schedule(context);
    }
}