package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class UserDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        bottomNav = findViewById(R.id.bottomNav);

        // 🔹 Default screen = Dashboard
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        setupBottomNav();
    }

    // ================= BOTTOM NAV =================

    private void setupBottomNav() {

        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            // 🏠 DASHBOARD
            if (id == R.id.nav_dashboard) {
                loadFragment(new HomeFragment());
                return true;
            }

            // 📊 STATISTICS
            if (id == R.id.nav_statistics) {
                loadFragment(new StatisticFragment());
                return true;
            }

            // ➕ CENTER ADD BUTTON (BOTTOM SHEET)
            if (id == R.id.nav_add) {
                showAddBottomSheet();
                return false; // IMPORTANT: tab change nahi hoga
            }

            // 📑 REPORTS
            if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
                return true;
            }

            // 👤 PROFILE
            if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }

    // ================= BOTTOM SHEET =================

    private void showAddBottomSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_add_button, null);

        dialog.setContentView(view);

        view.findViewById(R.id.optionAddIncome).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddIncomeActivity.class));
        });

        view.findViewById(R.id.optionAddExpense).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AddExpenseActivity.class));
        });

        dialog.show();
    }

    // ================= FRAGMENT LOADER =================

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
