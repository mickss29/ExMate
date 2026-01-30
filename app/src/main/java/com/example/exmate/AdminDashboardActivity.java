package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
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

    // ================= APP LOCK =================
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

    // ================= ON CREATE =================
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

    // ================= LOGOUT MENU =================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_xml, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.menu_logout) {
            logoutAdmin(); // ✅ FIXED HERE
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ================= LOGOUT =================
    private void logoutAdmin() {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you really want to logout?")
                .setPositiveButton("Logout", (d, which) -> {

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(AdminDashboardActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    // ================= DASHBOARD DATA =================
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
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalUsers.setText("0");
            }
        });
    }

    private void loadFinancialData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    double totalExpenses = 0.0;
                    int totalTransactions = 0;

                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {

                        if (userSnapshot.hasChild("expenses")) {
                            DataSnapshot expenses = userSnapshot.child("expenses");
                            totalTransactions += expenses.getChildrenCount();

                            for (DataSnapshot e : expenses.getChildren()) {
                                Object amt = e.child("amount").getValue();
                                if (amt != null) {
                                    totalExpenses += convertToDouble(amt);
                                }
                            }
                        }

                        if (userSnapshot.hasChild("incomes")) {
                            totalTransactions += userSnapshot.child("incomes").getChildrenCount();
                        }
                    }

                    tvTotalExpenses.setText("₹" + String.format("%.2f", totalExpenses));
                    tvTotalTransactions.setText(String.valueOf(totalTransactions));

                } catch (Exception e) {
                    tvTotalExpenses.setText("₹0.00");
                    tvTotalTransactions.setText("0");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalExpenses.setText("₹0.00");
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
        } catch (Exception ignored) {}
        return 0.0;
    }

    public void refreshDashboard(android.view.View view) {
        loadDashboardData();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }
}
