package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    // UI
    private BottomNavigationView bottomNav;
    private Spinner spAddAction;
    private RecyclerView rvRecentHistory;
    private View layoutEmpty;
    private TextView tvStatusMessage, tvTotalIncome, tvTotalExpense;

    // Firebase
    private DatabaseReference userRef;
    private String userId;

    // Data
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
        setupFirebase();
        loadDashboardData();
    }

    // ================= INIT =================

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNav);
        spAddAction = findViewById(R.id.spAddAction);
        rvRecentHistory = findViewById(R.id.rvRecentHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
    }

    // ================= FIREBASE =================

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);
    }

    private void loadDashboardData() {

        transactionList.clear();

        loadIncome();
        loadExpense();
    }

    private void loadIncome() {

        userRef.child("incomes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        double totalIncome = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            double amount = snap.child("amount").getValue(Double.class);
                            long time = snap.child("time").getValue(Long.class);

                            totalIncome += amount;

                            transactionList.add(
                                    new TransactionModel(
                                            "Income",
                                            amount,
                                            time
                                    )
                            );
                        }

                        tvTotalIncome.setText("₹" + totalIncome);
                        updateUI();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadExpense() {

        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        double totalExpense = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            double amount = snap.child("amount").getValue(Double.class);
                            long time = snap.child("time").getValue(Long.class);

                            totalExpense += amount;

                            transactionList.add(
                                    new TransactionModel(
                                            "Expense",
                                            amount,
                                            time
                                    )
                            );
                        }

                        tvTotalExpense.setText("₹" + totalExpense);

                        // Latest first
                        Collections.sort(transactionList,
                                (a, b) -> Long.compare(b.getTime(), a.getTime()));

                        adapter.notifyDataSetChanged();
                        updateUI();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ================= BOTTOM NAV =================

    private void setupBottomNav() {

        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) return true;

            if (id == R.id.nav_reports) {
                startActivity(new Intent(this, UserReportsActivity.class));
                return true;
            }

            if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                return true;
            }

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
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

                if (position == 0) return;

                if (position == 1) {
                    startActivity(new Intent(UserDashboardActivity.this, AddIncomeActivity.class));
                }

                if (position == 2) {
                    startActivity(new Intent(UserDashboardActivity.this, AddExpenseActivity.class));
                }

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

    // ================= UI =================

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

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(); // refresh after add income/expense
    }
}
