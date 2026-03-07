package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    // ── Stats cards (MaterialCardView in XML) ──────────────────────────────
    private MaterialCardView cardTotalUsers, cardTotalExpenses, cardTotalTransactions;
    private TextView tvTotalUsers, tvTotalExpenses, tvTotalTransactions;

    // ── Management rows (LinearLayout with IDs in XML) ─────────────────────
    private View cardUsers, cardCategories, cardReports, cardFeedback, cardOffers;

    // ── Quick action chips (MaterialCardView in XML) ───────────────────────
    private MaterialCardView chipAddUser, chipExportReport, chipAddOffer, chipViewFeedback;

    // ── Header ─────────────────────────────────────────────────────────────
    private MaterialCardView btnSignOut;

    // ── Firebase ───────────────────────────────────────────────────────────
    private DatabaseReference usersRef;
    private static final String TAG = "AdminDashboard";

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initViews();
        setClickListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AppLockManager.isEnabled(this) && AppLockManager.shouldAutoLock(this)) {
            AppLockManager.setUnlocked(this, false);
            startActivity(new Intent(this, AppLockActivity.class));
            return;
        }

        loadDashboardData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLockManager.markBackgroundTime(this);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════

    private void initViews() {
        // Stats
        cardTotalUsers        = findViewById(R.id.cardTotalUsers);
        cardTotalExpenses     = findViewById(R.id.cardTotalExpenses);
        cardTotalTransactions = findViewById(R.id.cardTotalTransactions);

        tvTotalUsers        = findViewById(R.id.tvTotalUsers);
        tvTotalExpenses     = findViewById(R.id.tvTotalExpenses);
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions);

        // Management rows (LinearLayouts in XML)
        cardUsers      = findViewById(R.id.cardUsers);
        cardCategories = findViewById(R.id.cardCategories);
        cardReports    = findViewById(R.id.cardReports);
        cardFeedback   = findViewById(R.id.cardFeedback);
        cardOffers     = findViewById(R.id.cardOffers);

        // Quick action chips
        chipAddUser      = findViewById(R.id.chipAddUser);
        chipExportReport = findViewById(R.id.chipExportReport);
        chipAddOffer     = findViewById(R.id.chipAddOffer);
        chipViewFeedback = findViewById(R.id.chipViewFeedback);

        // Sign out button
        btnSignOut = findViewById(R.id.btnSignOut);
    }

    private void setClickListeners() {

        // ── Sign out ───────────────────────────────────────────────────────
        btnSignOut.setOnClickListener(v -> showLogoutDialog());

        // ── Stat cards ─────────────────────────────────────────────────────
        cardTotalUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        cardTotalExpenses.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserFinanceReportActivity.class)));

        cardTotalTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        // ── Management rows ────────────────────────────────────────────────
        cardUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        cardCategories.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserFinanceReportActivity.class)));

        cardReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        cardFeedback.setOnClickListener(v ->
                startActivity(new Intent(this, AdminFeedbackActivity.class)));

        cardOffers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManageOffersActivity.class)));

        // ── Quick action chips ─────────────────────────────────────────────
        chipAddUser.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        chipExportReport.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));

        chipAddOffer.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManageOffersActivity.class)));

        chipViewFeedback.setOnClickListener(v ->
                startActivity(new Intent(this, AdminFeedbackActivity.class)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Logout
    // ══════════════════════════════════════════════════════════════════════

    private void showLogoutDialog() {
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

    // ══════════════════════════════════════════════════════════════════════
    // Firebase data loading
    // ══════════════════════════════════════════════════════════════════════

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
                    Log.e(TAG, "loadTotalUsers error", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalUsers.setText("0");
                Log.e(TAG, "loadTotalUsers cancelled: " + databaseError.getMessage());
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
                    Log.e(TAG, "loadFinancialData error", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvTotalExpenses.setText("₹0.00");
                tvTotalTransactions.setText("0");
                Log.e(TAG, "loadFinancialData cancelled: " + databaseError.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private double convertToDouble(Object obj) {
        try {
            if (obj instanceof Double)  return (Double) obj;
            if (obj instanceof Long)    return ((Long) obj).doubleValue();
            if (obj instanceof Integer) return ((Integer) obj).doubleValue();
            if (obj instanceof String)  return Double.parseDouble((String) obj);
        } catch (Exception ignored) {}
        return 0.0;
    }

    /** Called from XML via android:onClick if needed */
    public void refreshDashboard(View view) {
        loadDashboardData();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }
}