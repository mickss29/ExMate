package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsers, cardCategories, cardReports;
    private Toolbar adminToolbar;

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

        // ➜ Manage Users
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageUsersActivity.class
            );
            startActivity(intent);
        });

        // ➜ Manage Categories
        cardCategories.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageCategoriesActivity.class
            );
            startActivity(intent);
        });

        // ➜ Reports (later)
        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ReportsActivity.class
            );
            startActivity(intent);
        });
    }
}
