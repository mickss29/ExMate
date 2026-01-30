package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardUsers, cardCategories, cardReports, cardFeedback;
    private CardView cardTotalUsers, cardTotalExpenses, cardTotalTransactions;
    private TextView tvTotalUsers, tvTotalExpenses, tvTotalTransactions;
    private Toolbar adminToolbar;

    private DatabaseReference usersRef;
    private static final String TAG = "AdminDashboard";

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

        loadDashboardData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        adminToolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(adminToolbar);

        cardTotalUsers = findViewById(R.id.cardTotalUsers);
        cardTotalExpenses = findViewById(R.id.cardTotalExpenses);
        cardTotalTransactions = findViewById(R.id.cardTotalTransactions);

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions);

        cardUsers = findViewById(R.id.cardUsers);
        cardCategories = findViewById(R.id.cardCategories);
        cardReports = findViewById(R.id.cardReports);
        cardFeedback = findViewById(R.id.cardFeedback);

        cardTotalUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        cardTotalExpenses.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserFinanceReportActivity.class)));

        cardTotalTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        cardUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        cardCategories.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserFinanceReportActivity.class)));

        cardReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        cardFeedback.setOnClickListener(v ->
                startActivity(new Intent(this, AdminFeedbackActivity.class)));

        loadDashboardData();
    }

    private void loadDashboardData() {
        loadTotalUsers();
        loadFinancialData();
    }

    private void loadTotalUsers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    long userCount = dataSnapshot.getChildrenCount();
                    tvTotalUsers.setText(String.valueOf(userCount));
                    Log.d(TAG, "Total users: " + userCount);
                } catch (Exception e) {
                    tvTotalUsers.setText("0");
                    Log.e(TAG, "Error users: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalUsers.setText("0");
                Log.e(TAG, "Failed users: " + databaseError.getMessage());
            }
        });
    }

    // 🔥 FIXED: This method handles BOTH expenses and incomes
    private void loadFinancialData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    double totalExpenses = 0.0;
                    int totalTransactions = 0;

                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            Log.d(TAG, "Processing user: " + userId);

                            // 🔥 Check for EXPENSES
                            if (userSnapshot.hasChild("expenses")) {
                                DataSnapshot expensesSnapshot = userSnapshot.child("expenses");
                                totalTransactions += expensesSnapshot.getChildrenCount();

                                for (DataSnapshot expenseSnapshot : expensesSnapshot.getChildren()) {
                                    Object amountObj = expenseSnapshot.child("amount").getValue();
                                    if (amountObj != null) {
                                        double amount = convertToDouble(amountObj);
                                        totalExpenses += amount;
                                        Log.d(TAG, "  Expense: ₹" + amount);
                                    }
                                }
                            }

                            // 🔥 Check for INCOMES
                            if (userSnapshot.hasChild("incomes")) {
                                DataSnapshot incomesSnapshot = userSnapshot.child("incomes");
                                totalTransactions += incomesSnapshot.getChildrenCount();

                                // If you want to show total income too, add it here
                                // double userIncome = 0;
                                // for (DataSnapshot incomeSnapshot : incomesSnapshot.getChildren()) {
                                //     Object amountObj = incomeSnapshot.child("amount").getValue();
                                //     if (amountObj != null) {
                                //         userIncome += convertToDouble(amountObj);
                                //     }
                                // }
                                // Log.d(TAG, "  User " + userId + " income: ₹" + userIncome);
                            }
                        }
                    }

                    // Update UI
                    tvTotalExpenses.setText("₹" + String.format("%.2f", totalExpenses));
                    tvTotalTransactions.setText(String.valueOf(totalTransactions));

                    Log.d(TAG, "=== FINAL RESULTS ===");
                    Log.d(TAG, "Total Expenses: ₹" + totalExpenses);
                    Log.d(TAG, "Total Transactions: " + totalTransactions);

                } catch (Exception e) {
                    tvTotalExpenses.setText("₹0.00");
                    tvTotalTransactions.setText("0");
                    Log.e(TAG, "Error in loadFinancialData: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalExpenses.setText("₹0.00");
                tvTotalTransactions.setText("0");
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());
            }
        });
    }

    // Alternative: Load expenses and transactions separately (more efficient)
    private void loadSeparateData() {
        // This method is for if you want to show loading separately
        loadExpenses();
        loadTransactions();
    }

    private void loadExpenses() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalExpenses = 0.0;

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    if (userSnapshot.hasChild("expenses")) {
                        DataSnapshot expenses = userSnapshot.child("expenses");
                        for (DataSnapshot expense : expenses.getChildren()) {
                            Object amount = expense.child("amount").getValue();
                            if (amount != null) {
                                totalExpenses += convertToDouble(amount);
                            }
                        }
                    }
                }

                tvTotalExpenses.setText("₹" + String.format("%.2f", totalExpenses));
                Log.d(TAG, "Expenses loaded: ₹" + totalExpenses);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalExpenses.setText("₹0.00");
            }
        });
    }

    private void loadTransactions() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int totalTransactions = 0;

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    // Count expenses
                    if (userSnapshot.hasChild("expenses")) {
                        totalTransactions += userSnapshot.child("expenses").getChildrenCount();
                    }
                    // Count incomes
                    if (userSnapshot.hasChild("incomes")) {
                        totalTransactions += userSnapshot.child("incomes").getChildrenCount();
                    }
                }

                tvTotalTransactions.setText(String.valueOf(totalTransactions));
                Log.d(TAG, "Transactions loaded: " + totalTransactions);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalTransactions.setText("0");
            }
        });
    }

    private double convertToDouble(Object obj) {
        try {
            if (obj instanceof Double) return (Double) obj;
            if (obj instanceof Long) return ((Long) obj).doubleValue();
            if (obj instanceof Integer) return ((Integer) obj).doubleValue();
            if (obj instanceof String) return Double.parseDouble((String) obj);
        } catch (Exception e) {
            Log.e(TAG, "Convert error: " + e.getMessage());
        }
        return 0.0;
    }

    public void refreshDashboard(android.view.View view) {
        loadDashboardData();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }
}