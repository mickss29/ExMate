package com.example.exmate;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity
        implements HomeFragment.HomeNavigationListener {

    private BottomNavigationView bottomNav;

    // 🔒 AppLock guard
    private boolean isLockScreenOpened = false;

    @Override
    protected void onPause() {
        super.onPause();
        AppLockManager.markBackgroundTime(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isLockScreenOpened
                && AppLockManager.isEnabled(this)
                && AppLockManager.shouldAutoLock(this)) {

            isLockScreenOpened = true;
            AppLockManager.setUnlocked(this, false);
            startActivity(new Intent(this, AppLockActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        bottomNav = findViewById(R.id.bottomNav);

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

    // ================= HOME CALLBACKS =================

    @Override
    public void openReports() {
        bottomNav.setSelectedItemId(R.id.nav_reports);
    }

    @Override
    public void openBudget() {
        bottomNav.setSelectedItemId(R.id.nav_budget);
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
                openBudgetFlow();
                return true;
            }

            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddTransactionActivity.class));
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

    // ================= BUDGET FLOW =================

    private void openBudgetFlow() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("budgets")
                .child("monthly")
                .child(getCurrentMonthKey());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Fragment target = new BudgetFragment();
                loadFragment(target);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getCurrentMonthKey() {
        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(new Date());
    }

    // ================= FRAGMENT LOADER =================

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // ================= NOTIFICATIONS =================

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
        scheduleAlarm(0, 19);
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
        scheduleAlarm(4001, 21);
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
        scheduleAlarm(7001, 7);
    }

    private void scheduleAlarm(int requestCode, int hour) {

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this,
                requestCode == 0 ? DailyReminderReceiver.class :
                        requestCode == 4001 ? DailySummaryReceiver.class :
                                MorningGreetingReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this, requestCode, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
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
