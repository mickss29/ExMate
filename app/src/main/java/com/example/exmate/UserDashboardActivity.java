package com.example.exmate;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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

        // 🔹 Default screen = Dashboard
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

            // 🏠 DASHBOARD
            if (id == R.id.nav_dashboard) {
                loadFragment(new HomeFragment());
                return true;
            }

            // 📊 STATISTICS
            if (id == R.id.nav_budget) {
                loadFragment(new BudgetFragment());
                return true;
            }



            // ➕ CENTER ADD BUTTON (BOTTOM SHEET)
            if (id == R.id.nav_add) {
                showAddBottomSheet();
                return false; // IMPORTANT: tab change nahi hoga
            }

            // 📑 REPORTS
            if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
                return true;
            }

            // 👤 PROFILE
            if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    // ================= BOTTOM SHEET =================

    private void showAddBottomSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_add_button, null);

        dialog.setContentView(view);

        view.findViewById(R.id.optionAddIncome).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddIncomeActivity.class));
        });

        view.findViewById(R.id.optionAddExpense).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddExpenseActivity.class));
        });

        dialog.show();
    }

    // ================= FRAGMENT LOADER =================

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

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
                (android.app.AlarmManager)
                        getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DailyReminderReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 19); // 7 PM
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);

        // If time already passed today → schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                android.app.AlarmManager.INTERVAL_DAY,
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
                (android.app.AlarmManager)
                        getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, DailySummaryReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this,
                        4001,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21); // 9 PM
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
                (android.app.AlarmManager)
                        getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, MorningGreetingReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this,
                        7001,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 7); // 7 AM
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);

        // If already past 7 AM → next day
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }





}
