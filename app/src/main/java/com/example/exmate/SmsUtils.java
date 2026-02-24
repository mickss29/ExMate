package com.example.exmate;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsUtils {

    // ================= REF NO =================
    public static String extractRefNo(String msg) {
        Pattern p = Pattern.compile("(refno|utr|txn id|txnid)[\\s:]*([a-z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(msg);
        if (m.find()) return m.group(2);
        return null;
    }

    // ================= CATEGORY =================
    public static String detectCategory(String msg) {
        msg = msg.toLowerCase(Locale.US);

        if (msg.contains("zomato") || msg.contains("swiggy")) return "Food";
        if (msg.contains("uber") || msg.contains("ola")) return "Travel";
        if (msg.contains("amazon") || msg.contains("flipkart")) return "Shopping";
        if (msg.contains("electric") || msg.contains("bill")) return "Bills";
        if (msg.contains("salary") || msg.contains("credited")) return "Salary";

        return "Auto (SMS)";
    }

    // ================= QUEUE =================
    public static void addToQueue(Context c, JSONObject obj) {
        try {
            String raw = c.getSharedPreferences("SMS_QUEUE", 0)
                    .getString("queue", "[]");

            JSONArray arr = new JSONArray(raw);
            arr.put(obj);

            c.getSharedPreferences("SMS_QUEUE", 0)
                    .edit()
                    .putString("queue", arr.toString())
                    .apply();

        } catch (Exception ignored) {}
    }
}