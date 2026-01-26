package com.example.exmate;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    // ================= EXPENSE =================
    public static void showExpenseSummaryNotification(
            Context context,
            double addedAmount,
            String category
    ) {
        if (!hasPermission(context)) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        // 1️⃣ Read budget of selected category
        userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .child("categories")
                .child(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {

                        int budget = snap.exists() ? snap.getValue(Integer.class) : 0;

                        // 2️⃣ Read expenses of same category
                        userRef.child("expenses")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot exSnap) {

                                        int used = 0;

                                        for (DataSnapshot ds : exSnap.getChildren()) {
                                            String cat = ds.child("category").getValue(String.class);
                                            Integer amt = ds.child("amount").getValue(Integer.class);
                                            Long time = ds.child("time").getValue(Long.class);

                                            if (cat == null || amt == null || time == null) continue;
                                            if (!cat.equalsIgnoreCase(category)) continue;

                                            String m = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                                    .format(new Date(time));
                                            if (!m.equals(monthKey)) continue;

                                            used += amt;
                                        }

                                        showExpenseNotification(
                                                context,
                                                addedAmount,
                                                category,
                                                used,
                                                budget
                                        );
                                    }

                                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                                });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private static void showExpenseNotification(
            Context context,
            double added,
            String category,
            int used,
            int budget
    ) {

        int left = Math.max(budget - used, 0);
        double percent = budget == 0 ? 0 : (used * 100.0) / budget;

        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        String title;

        style.addLine("₹" + added + " expense added on " + category);

        if (budget > 0) {
            style.addLine("Spent: ₹" + used + " / ₹" + budget);
            style.addLine("Remaining: ₹" + left);

            if (percent >= 100) {
                title = category + " budget exceeded 🚨";
            } else if (percent >= 80) {
                title = category + " budget almost used ⚠️";
            } else {
                title = "Expense added • " + category;
            }
        } else {
            title = "Expense added • " + category;
            style.addLine("No budget set for this category");
        }

        style.addLine("Tap to open dashboard");

        Notification n = new NotificationCompat.Builder(context, "transaction_alert")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setStyle(style)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), n);
    }

    // ================= INCOME =================
    public static void showIncomeSummaryNotification(
            Context context,
            double amount,
            String source
    ) {
        if (!hasPermission(context)) return;

        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification n = new NotificationCompat.Builder(context, "transaction_alert")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Income added 💰")
                .setContentText("₹" + amount + " received from " + source)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), n);
    }

    // ================= PERMISSION =================
    private static boolean hasPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED);
    }
}
