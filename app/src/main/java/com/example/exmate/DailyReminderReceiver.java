package com.example.exmate;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class DailyReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Notification notification =
                new NotificationCompat.Builder(context, "daily_reminder")
                        .setSmallIcon(R.drawable.ic_notification) // your icon
                        .setContentTitle("📝 Daily Reminder")
                        .setContentText("Have you added today’s income & expenses?")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(2001, notification);
    }
}
