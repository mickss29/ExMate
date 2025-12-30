package com.example.exmate;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Calendar;

public class DailySummaryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        final long start = getTodayStart();
        final long end = getTodayEnd();

        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot expSnap) {

                        final double[] expense = {0}; // effectively final
                        for (DataSnapshot s : expSnap.getChildren()) {
                            Double amt = s.child("amount").getValue(Double.class);
                            Long time = s.child("time").getValue(Long.class);
                            if (amt != null && time != null
                                    && time >= start && time <= end) {
                                expense[0] += amt;
                            }
                        }

                        userRef.child("incomes")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot incSnap) {

                                        double income = 0;
                                        for (DataSnapshot s : incSnap.getChildren()) {
                                            Double amt = s.child("amount").getValue(Double.class);
                                            Long time = s.child("time").getValue(Long.class);
                                            if (amt != null && time != null
                                                    && time >= start && time <= end) {
                                                income += amt;
                                            }
                                        }

                                        // expense[0] use
                                        sendSummaryNotification(context, income, expense[0]);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendSummaryNotification(Context context, double income, double expense) {
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
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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
