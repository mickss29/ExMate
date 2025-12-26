package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsers, cardCategories, cardReports, cardFeedback;
    private Toolbar adminToolbar;
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
        setContentView(R.layout.activity_admin_dashboard);

        // Toolbar
        adminToolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(adminToolbar);

        // Cards
        cardUsers = findViewById(R.id.cardUsers);
        cardCategories = findViewById(R.id.cardCategories);
        cardReports = findViewById(R.id.cardReports);
        cardFeedback = findViewById(R.id.cardFeedback); // 🔥 NEW

        // ➜ Manage Users
        cardUsers.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        ManageUsersActivity.class
                )));

        // ➜ Expense Management
        cardCategories.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        AdminUserFinanceReportActivity.class
                )));

        // ➜ Reports & Analytics
        cardReports.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        ReportsActivity.class
                )));

        // ➜ 🔥 USER FEEDBACK & SUPPORT
        cardFeedback.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminDashboardActivity.this,
                        AdminFeedbackActivity.class
                )));
    }
}
