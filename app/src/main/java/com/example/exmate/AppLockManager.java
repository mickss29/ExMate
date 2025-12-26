package com.example.exmate;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLockManager {

    private static final String PREF = "app_lock_pref";

    private static final String KEY_PIN = "lock_pin";
    private static final String KEY_ENABLED = "lock_enabled";
    private static final String KEY_UNLOCKED = "lock_unlocked";

    // Auto-lock
    private static final String KEY_LAST_BG_TIME = "last_bg_time";
    private static final long AUTO_LOCK_DELAY = 30 * 1000; // 30 seconds

    // ================= ENABLE APP LOCK =================
    public static void enable(Context context, String pin) {
        SharedPreferences sp =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        sp.edit()
                .putString(KEY_PIN, pin)
                .putBoolean(KEY_ENABLED, true)
                .putBoolean(KEY_UNLOCKED, false)
                .apply();
    }

    // ================= CHECKS =================
    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static boolean isUnlocked(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY_UNLOCKED, false);
    }

    public static boolean checkPin(Context c, String enteredPin) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String savedHash = sp.getString(KEY_PIN, "");
        return HashUtil.sha256(enteredPin).equals(savedHash);
    }


    // ================= UNLOCK STATE =================
    public static void setUnlocked(Context context, boolean unlocked) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_UNLOCKED, unlocked)
                .apply();
    }

    // ================= AUTO LOCK =================
    public static void markBackgroundTime(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_BG_TIME, System.currentTimeMillis())
                .apply();
    }

    public static boolean shouldAutoLock(Context context) {

        long last =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .getLong(KEY_LAST_BG_TIME, 0);

        return System.currentTimeMillis() - last > AUTO_LOCK_DELAY;
    }

    // ================= RESET PIN (ONLY WHEN USER WANTS) =================
    // ⚠️ DO NOT CALL THIS ON LOGOUT
    public static void resetPin(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PIN)
                .remove(KEY_ENABLED)
                .remove(KEY_UNLOCKED)
                .apply();
    }
    public static void restoreFromServer(Context c, String pinHash) {

        SharedPreferences sp =
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        sp.edit()
                .putString(KEY_PIN, pinHash)
                .putBoolean(KEY_ENABLED, true)
                .putBoolean(KEY_UNLOCKED, false)
                .apply();
    }

}
