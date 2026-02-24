package com.example.exmate;

import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

public class SmsQueueSync {

    public static void sync(Context c, String uid) {

        try {
            String raw = c.getSharedPreferences("SMS_QUEUE", 0)
                    .getString("queue", "[]");

            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) return;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                TransactionModel model;

                if ("Income".equals(o.getString("type"))) {
                    model = new TransactionModel(
                            "Income",
                            o.getDouble("amount"),
                            o.getLong("time"),
                            o.getString("category")
                    );
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("incomes")
                            .push()
                            .setValue(model);
                } else {
                    model = new TransactionModel(
                            "Expense",
                            o.getDouble("amount"),
                            o.getLong("time"),
                            o.getString("category"),
                            true
                    );
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("expenses")
                            .push()
                            .setValue(model);
                }
            }

            // ✅ Clear queue
            c.getSharedPreferences("SMS_QUEUE", 0)
                    .edit()
                    .putString("queue", "[]")
                    .apply();

        } catch (Exception ignored) {}
    }
}