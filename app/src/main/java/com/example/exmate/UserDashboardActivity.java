package com.example.exmate;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
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
    private MaterialCardView bottomNavCard;

    // Track current selected ID to avoid redundant reloads
    private int currentNavId = -1;

    // AppLock guard
    private boolean isLockScreenOpened = false;

    // ══════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bottom_nav_baar);

        setupEdgeInsets();
        bindViews();
        requestNotificationPermission();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), R.id.nav_dashboard);
        }

        setupBottomNav();
        runNavEntrance();
        syncSmsQueue();

        // Schedule all notifications
        createDailyReminderChannel();
        scheduleDailyReminder();
        createDailySummaryChannel();
        scheduleDailySummary();
        createMorningGreetingChannel();
        scheduleMorningGreeting();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLockManager.markBackgroundTime(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSmsQueue();

        if (!isLockScreenOpened
                && AppLockManager.isEnabled(this)
                && AppLockManager.shouldAutoLock(this)) {
            isLockScreenOpened = true;
            AppLockManager.setUnlocked(this, false);
            startActivity(new Intent(this, AppLockActivity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════════════

    private void bindViews() {
        bottomNav     = findViewById(R.id.bottomNav);
        bottomNavCard = findViewById(R.id.bottomNavCard);
    }

    /** Edge-to-edge — nav card floats above system nav bar */
    private void setupEdgeInsets() {
        View root = findViewById(R.id.fragmentContainer);
        if (root == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // Push nav card up by system nav bar height
            if (bottomNavCard != null) {
                bottomNavCard.setTranslationY(-bottom);
            }
            return insets;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BOTTOM NAV
    // ══════════════════════════════════════════════════════════════════════

    private void setupBottomNav() {

        // Set initial selection without triggering listener
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        currentNavId = R.id.nav_dashboard;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Don't reload if already on this tab
            if (id == currentNavId) return true;

            // "Add" tab — launches Activity, doesn't change fragment
            if (id == R.id.nav_add) {
                animateNavItem(item.getItemId());
                startActivity(new Intent(this, AddTransactionActivity.class));
                // Keep previous tab visually selected
                bottomNav.setSelectedItemId(currentNavId);
                return false;
            }

            animateNavItem(id);

            if (id == R.id.nav_dashboard) {
                loadFragment(new HomeFragment(), id);
                return true;
            }
            if (id == R.id.nav_budget) {
                openBudgetFlow();
                return true;
            }
            if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment(), id);
                return true;
            }
            if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), id);
                return true;
            }

            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════

    /** Slide-up entrance for the floating nav card */
    private void runNavEntrance() {
        if (bottomNavCard == null) return;
        bottomNavCard.setAlpha(0f);
        bottomNavCard.setTranslationY(80f);
        bottomNavCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    /**
     * Bounce-scale the tapped nav item icon.
     * Works by finding the icon view inside BottomNavigationView.
     */
    private void animateNavItem(int itemId) {
        View itemView = bottomNav.findViewById(itemId);
        if (itemView == null) return;
        itemView.animate()
                .scaleX(0.82f).scaleY(0.82f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() ->
                        itemView.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(220)
                                .setInterpolator(new OvershootInterpolator(2.5f))
                                .start()
                ).start();
    }

    /** Hide nav bar (for full-screen fragments if needed) */
    public void hideBottomNav() {
        if (bottomNavCard == null) return;
        bottomNavCard.animate()
                .alpha(0f).translationY(120f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> bottomNavCard.setVisibility(View.GONE))
                .start();
    }

    /** Show nav bar again */
    public void showBottomNav() {
        if (bottomNavCard == null) return;
        bottomNavCard.setVisibility(View.VISIBLE);
        bottomNavCard.animate()
                .alpha(1f).translationY(0f)
                .setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HOME NAVIGATION CALLBACKS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void openReports() {
        bottomNav.setSelectedItemId(R.id.nav_reports);
        loadFragment(new ReportsFragment(), R.id.nav_reports);
    }

    @Override
    public void openBudget() {
        bottomNav.setSelectedItemId(R.id.nav_budget);
        openBudgetFlow();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FRAGMENT LOADER
    // ══════════════════════════════════════════════════════════════════════

    private void loadFragment(Fragment fragment, int navId) {
        currentNavId = navId;

        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        // Skip if same fragment type already loaded
        if (current != null && current.getClass().equals(fragment.getClass())) return;

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUDGET FLOW
    // ══════════════════════════════════════════════════════════════════════

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
                loadFragment(new BudgetFragment(), R.id.nav_budget);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getCurrentMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SMS QUEUE
    // ══════════════════════════════════════════════════════════════════════

    private void syncSmsQueue() {
        String uid = getSharedPreferences("USER_PREF", MODE_PRIVATE)
                .getString("UID", null);
        if (uid != null) SmsQueueSync.sync(this, uid);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NOTIFICATION PERMISSION
    // ══════════════════════════════════════════════════════════════════════

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NOTIFICATION CHANNELS + ALARMS
    // ══════════════════════════════════════════════════════════════════════

    private void createNotificationChannel(String id, String name, String desc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            id, name,
                            android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(desc);
            getSystemService(android.app.NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }

    private void createDailyReminderChannel() {
        createNotificationChannel(
                "daily_reminder", "Daily Reminders",
                "Daily income & expense reminder");
    }

    private void createDailySummaryChannel() {
        createNotificationChannel(
                "daily_summary", "Daily Summary",
                "Daily income & expense summary");
    }

    private void createMorningGreetingChannel() {
        createNotificationChannel(
                "morning_greeting", "Morning Greetings",
                "Daily morning greeting");
    }

    private void scheduleDailyReminder()  { scheduleAlarm(0,    19); }
    private void scheduleDailySummary()   { scheduleAlarm(4001, 21); }
    private void scheduleMorningGreeting(){ scheduleAlarm(7001,  7); }

    private void scheduleAlarm(int requestCode, int hour) {
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this,
                requestCode == 0    ? DailyReminderReceiver.class :
                        requestCode == 4001 ? DailySummaryReceiver.class  :
                                MorningGreetingReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        this, requestCode, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE);

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
                pendingIntent);
    }
}