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

    private static final String CHANNEL_ID = "transaction_alert";

    // =========================
    // EXPENSE PREMIUM NOTIFICATION
    // =========================
    public static void showExpenseSummaryNotification(
            Context context,
            double addedAmount,
            String category
    ) {

        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        // 🔹 Read category budget
        userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .child("categories")
                .child(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot budgetSnap) {

                        int budget = budgetSnap.exists()
                                ? budgetSnap.getValue(Integer.class) : 0;

                        // 🔹 Read category expenses
                        userRef.child("expenses")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot expenseSnap) {

                                        int used = 0;

                                        for (DataSnapshot ds : expenseSnap.getChildren()) {
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

                                        showPremiumExpenseUI(
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

    // =========================
    // PREMIUM UI BUILDER
    // =========================
    private static void showPremiumExpenseUI(
            Context context,
            double added,
            String category,
            int used,
            int budget
    ) {

        int left = Math.max(budget - used, 0);
        int percent = budget == 0 ? 0 : (int) ((used * 100f) / budget);

        String statusLine;
        String title;
        String emoji;

        if (budget == 0) {
            emoji = "🧾";
            title = "Expense added";
            statusLine = "No budget set for " + category;
        }
        else if (used > budget) {
            emoji = "🚨";
            title = "Budget Exceeded!";
            statusLine = "Over by ₹" + (used - budget);
        }
        else if (percent >= 80) {
            emoji = "⚠️";
            title = "Near budget limit";
            statusLine = "Only ₹" + left + " left";
        }
        else {
            emoji = "💸";
            title = "Expense added";
            statusLine = "₹" + left + " remaining";
        }

        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 💎 Premium Big Text UI
        NotificationCompat.BigTextStyle style =
                new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(emoji + " " + title + " • " + category)
                        .bigText(
                                "₹" + added + " expense added\n\n" +
                                        "Spent: ₹" + used + " / ₹" + budget + "\n" +
                                        "Remaining: ₹" + left + "\n\n" +
                                        statusLine + "\n\n" +
                                        "Tap to open dashboard"
                        );

        Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(emoji + " " + title)
                        .setContentText("₹" + added + " on " + category)
                        .setStyle(style)
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .build();

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), notification);
    }

    // =========================
    // INCOME PREMIUM UI
    // =========================
    public static void showIncomeSummaryNotification(
            Context context,
            double addedAmount,
            String source
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.BigTextStyle style =
                new NotificationCompat.BigTextStyle()
                        .setBigContentTitle("💰 Income received")
                        .bigText(
                                "₹" + addedAmount + " credited\n\n" +
                                        "Source: " + source + "\n\n" +
                                        "Tap to view dashboard"
                        );

        Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("💰 Income added")
                        .setContentText("₹" + addedAmount + " from " + source)
                        .setStyle(style)
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .build();

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), notification);
    }
}
