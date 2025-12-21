package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
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

    // UI (ONLY IDs THAT EXIST IN XML)
    private BottomNavigationView bottomNav;
    private RecyclerView rvRecent;
    private TextView tvIncome, tvExpense;
    private MaterialCardView cardAddIncome, cardAddExpense;

    // Firebase
    private DatabaseReference userRef;
    private String userId;

    // Data
    private final List<TransactionModel> transactionList = new ArrayList<>();
    private RecentTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        initViews();
        setupRecentList();
        setupCardClicks();
        setupBottomNav();
        setupFirebase();
    }

    // ================= INIT =================

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNav);
        rvRecent = findViewById(R.id.rvRecent);

        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);

        cardAddIncome = findViewById(R.id.cardAddIncome);
        cardAddExpense = findViewById(R.id.cardAddExpense);
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

    // ================= LOAD DATA =================

    private void loadDashboardData() {
        transactionList.clear();
        adapter.notifyDataSetChanged();

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
                            Double amount = snap.child("amount").getValue(Double.class);
                            Long time = snap.child("time").getValue(Long.class);

                            if (amount == null || time == null) continue;

                            totalIncome += amount;
                            transactionList.add(
                                    new TransactionModel("Income", amount, time)
                            );
                        }

                        tvIncome.setText("Income  ₹" + totalIncome);
                        finalizeList();
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
                            Double amount = snap.child("amount").getValue(Double.class);
                            Long time = snap.child("time").getValue(Long.class);

                            if (amount == null || time == null) continue;

                            totalExpense += amount;
                            transactionList.add(
                                    new TransactionModel("Expense", amount, time)
                            );
                        }

                        tvExpense.setText("Expenses  ₹" + totalExpense);
                        finalizeList();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void finalizeList() {
        Collections.sort(
                transactionList,
                (a, b) -> Long.compare(b.getTime(), a.getTime())
        );
        adapter.notifyDataSetChanged();
    }

    // ================= RECENT LIST =================

    private void setupRecentList() {
        adapter = new RecentTransactionAdapter(transactionList);
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        rvRecent.setAdapter(adapter);
    }

    // ================= CARD CLICKS =================

    private void setupCardClicks() {

        cardAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class))
        );

        cardAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class))
        );
    }

    // ================= CENTER + BOTTOM SHEET =================

    private void showAddTransactionSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_add_button, null);

        dialog.setContentView(view);

        view.findViewById(R.id.optionAddIncome)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, AddIncomeActivity.class));
                });

        view.findViewById(R.id.optionAddExpense)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, AddExpenseActivity.class));
                });

        dialog.show();
    }

    // ================= BOTTOM NAV =================

    private void setupBottomNav() {

        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_dashboard) return true;

            // ⭐ CENTER + BUTTON
            if (id == R.id.nav_add) {
                showAddTransactionSheet();
                return false; // DO NOT switch tab
            }

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

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }
}
