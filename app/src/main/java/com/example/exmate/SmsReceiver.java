package com.example.exmate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "auto_transaction_channel";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction()))
            return;

        // 🔥 Ensure Firebase is initialized in background
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null) return;

        for (Object pdu : pdus) {

            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }

            String msg = sms.getMessageBody().toLowerCase(Locale.US);

            // ❌ Ignore OTP / promo
            if (msg.contains("otp") || msg.contains("password") || msg.contains("offer"))
                continue;

            boolean isDebit = msg.contains("debit") || msg.contains("debited");
            boolean isCredit = msg.contains("credit") || msg.contains("credited");

            if (!isDebit && !isCredit) continue;

            double amount = extractAmount(msg);
            if (amount <= 0) continue;

            saveToFirebase(context, isCredit, amount, msg);
        }
    }

    private double extractAmount(String msg) {
        try {
            Pattern pattern = Pattern.compile(
                    "(rs\\.?\\s?|inr\\s?|by\\s)(\\d+(\\.\\d{1,2})?)"
            );
            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(2));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void saveToFirebase(Context context, boolean isCredit, double amount, String fullMsg) {

        String uid = context
                .getSharedPreferences("USER_PREF", Context.MODE_PRIVATE)
                .getString("UID", null);

        if (uid == null) return;

        // 🔁 DUPLICATE CHECK
        String refNo = SmsUtils.extractRefNo(fullMsg);
        if (refNo != null) {
            boolean already =
                    context.getSharedPreferences("SMS_DUP", 0)
                            .getBoolean(refNo, false);
            if (already) return;
        }

        String category = SmsUtils.detectCategory(fullMsg);

        TransactionModel model;
        DatabaseReference targetRef;

        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        long time = System.currentTimeMillis();

        if (isCredit) {
            model = new TransactionModel("Income", amount, time, category);
            targetRef = baseRef.child("incomes");
        } else {
            model = new TransactionModel("Expense", amount, time, category, true);
            targetRef = baseRef.child("expenses");
        }

        targetRef.push().setValue(model)
                .addOnSuccessListener(unused -> {

                    // ✅ mark ref as processed
                    if (refNo != null) {
                        context.getSharedPreferences("SMS_DUP", 0)
                                .edit()
                                .putBoolean(refNo, true)
                                .apply();
                    }

                    showNotification(context,
                            isCredit ? "Income Added" : "Expense Added",
                            "₹" + amount + " • " + category);
                })
                .addOnFailureListener(e -> {
                    try {
                        // ⛔ OFFLINE → QUEUE IT
                        JSONObject obj = new JSONObject();
                        obj.put("type", isCredit ? "Income" : "Expense");
                        obj.put("amount", amount);
                        obj.put("time", time);
                        obj.put("category", category);
                        SmsUtils.addToQueue(context, obj);
                    } catch (Exception ignored) {}
                });
    }
    private void showNotification(Context context, String title, String msg) {

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Transactions",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}