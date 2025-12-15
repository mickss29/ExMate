package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    // UI
    private BottomNavigationView bottomNav;
    private Spinner spAddAction;
    private RecyclerView rvRecentHistory;
    private View layoutEmpty;
    private TextView tvStatusMessage;

    // Data (temporary – no Firebase yet)
    private List<TransactionModel> transactionList;
    private RecentTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        initViews();
        setupBottomNav();
        setupAddActionSpinner();
        setupRecentHistory();
        updateUI();
    }

    // ================= INIT =================

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNav);
        spAddAction = findViewById(R.id.spAddAction);
        rvRecentHistory = findViewById(R.id.rvRecentHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
    }

    // ================= BOTTOM NAV =================

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                return true;
            }

            if (id == R.id.nav_reports) {
                startActivity(new Intent(this, UserReportsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    // ================= ADD ACTION SPINNER =================

    private void setupAddActionSpinner() {

        ArrayAdapter<CharSequence> spinnerAdapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.dashboard_add_action,
                        android.R.layout.simple_spinner_item
                );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spAddAction.setAdapter(spinnerAdapter);

        spAddAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) return; // Select Action

                if (position == 1) {
                    startActivity(new Intent(UserDashboardActivity.this, AddIncomeActivity.class));
                }

                if (position == 2) {
                    startActivity(new Intent(UserDashboardActivity.this, AddExpenseActivity.class));
                }

                // Reset spinner
                spAddAction.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ================= RECENT HISTORY =================

    private void setupRecentHistory() {
        transactionList = new ArrayList<>();

        adapter = new RecentTransactionAdapter(transactionList);
        rvRecentHistory.setLayoutManager(new LinearLayoutManager(this));
        rvRecentHistory.setAdapter(adapter);
    }

    // ================= UI STATE =================

    private void updateUI() {

        if (transactionList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvRecentHistory.setVisibility(View.GONE);
            tvStatusMessage.setText("No activity yet. Start adding income or expense.");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvRecentHistory.setVisibility(View.VISIBLE);
            tvStatusMessage.setText("Here’s your recent activity");
        }
    }

    // ================= FUTURE READY =================
    // Call this method after Firebase data update later
    public void refreshDashboard(List<TransactionModel> newList) {
        transactionList.clear();
        transactionList.addAll(newList);
        adapter.notifyDataSetChanged();
        updateUI();
    }
}
