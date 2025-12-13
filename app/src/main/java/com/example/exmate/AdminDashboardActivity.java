package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsers, cardExpenses, cardReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);

        // Dashboard Cards
        cardUsers = findViewById(R.id.cardUsers);
        cardExpenses = findViewById(R.id.cardExpenses);
        cardReports = findViewById(R.id.cardReports);

        // Click → Manage Users
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageUsersActivity.class
            );
            startActivity(intent);
        });

        // Click → Expense Management
        cardExpenses.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ManageExpensesActivity.class
            );
            startActivity(intent);
        });

        // Click → Reports & Analytics
        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDashboardActivity.this,
                    ReportsActivity.class
            );
            startActivity(intent);
        });
    }
}
