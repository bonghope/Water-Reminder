package com.aqua.water.services;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { public void onReceive(Context c,Intent i) { ReminderReceiver.schedule(c); } }
