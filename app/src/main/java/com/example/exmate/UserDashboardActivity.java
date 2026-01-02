package com.example.exmate;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class UserDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onPause() {
        super.onPause();
        AppLockManager.markBackgroundTime(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AppLockManager.isEnabled(this)
                && AppLockManager.shouldAutoLock(this)) {

            AppLockManager.setUnlocked(this, false);
            startActivity(new Intent(this, AppLockActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        bottomNav = findViewById(R.id.bottomNav);

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        setupBottomNav();
        createDailyReminderChannel();
        scheduleDailyReminder();
        createDailySummaryChannel();
        scheduleDailySummary();
        createMorningGreetingChannel();
        scheduleMorningGreeting();
    }

    // ================= BOTTOM NAV =================

    private void setupBottomNav() {

        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                loadFragment(new HomeFragment());
                return true;
            }

            if (id == R.id.nav_budget) {
                loadFragment(new BudgetFragment());
                return true;
            }

            if (id == R.id.nav_add) {
                startActivity(
                        new Intent(this, AddTransactionActivity.class)
                );
                return true;

        }

            if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
                return true;
            }

            if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    // ================= FRAGMENT LOADER =================

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // ================== SCHEDULERS / NOTIFS — UNTOUCHED ==================

    private void createDailyReminderChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "daily_reminder",
                            "Daily Reminders",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription("Daily income & expense reminder");

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleDailyReminder() {
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DailyReminderReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this, 0, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void createDailySummaryChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "daily_summary",
                            "Daily Summary",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription("Daily income & expense summary");

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleDailySummary() {
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DailySummaryReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this, 4001, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void createMorningGreetingChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "morning_greeting",
                            "Morning Greetings",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription("Daily morning greeting");

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleMorningGreeting() {
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, MorningGreetingReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this, 7001, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }
}
