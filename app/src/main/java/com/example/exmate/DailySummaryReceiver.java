package com.example.exmate;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Calendar;

public class DailySummaryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        long start = getTodayStart();
        long end = getTodayEnd();

        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot expSnap) {

                        double expense = 0;
                        for (DataSnapshot s : expSnap.getChildren()) {
                            Double amt = s.child("amount").getValue(Double.class);
                            Long time = s.child("time").getValue(Long.class);
                            if (amt != null && time != null
                                    && time >= start && time <= end) {
                                expense += amt;
                            }
                        }

                        userRef.child("incomes")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot incSnap) {

                                        double income = 0;
                                        for (DataSnapshot s : incSnap.getChildren()) {
                                            Double amt = s.child("amount").getValue(Double.class);
                                            Long time = s.child("time").getValue(Long.class);
                                            if (amt != null && time != null
                                                    && time >= start && time <= end) {
                                                income += amt;
                                            }
                                        }

                                        sendSummaryNotification(
                                                context, income, expense
                                        );
                                    }

                                    @Override public void onCancelled(DatabaseError error) {}
                                });
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void sendSummaryNotification(
            Context context,
            double income,
            double expense
    ) {
        String msg = "Income: ₹" + income + " | Expense: ₹" + expense;

        Notification notification =
                new NotificationCompat.Builder(context, "daily_summary")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("📊 Daily Summary")
                        .setContentText(msg)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(4001, notification);
    }

    private long getTodayStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getTodayEnd() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
