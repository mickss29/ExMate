package com.example.exmate;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class BudgetFragment extends Fragment {

    // ===== Period =====
    private Button btnMonthly, btnYearly;
    private TextView tvSelectedMonth;

    // ===== Budget =====
    private EditText etTotalBudget;
    private CheckBox cbCarryForward;

    // ===== Buttons =====
    private Button btnSaveBudget, btnSaveCategoryBudget;

    // ===== Summary =====
    private LinearLayout cardBudgetSummary;
    private TextView tvSummaryMonth, tvSummaryAmount, tvSummaryCarry;

    // ===== Categories =====
    private LinearLayout layoutCategoryBudgets;
    private TextView tvRemainingBudget;

    // ===== State =====
    private boolean isMonthly = true;
    private int selectedYear, selectedMonth;

    public BudgetFragment() {
        super(R.layout.budget_fragment);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        // ===== Bind Views =====
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnYearly = view.findViewById(R.id.btnYearly);
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);

        etTotalBudget = view.findViewById(R.id.etTotalBudget);
        cbCarryForward = view.findViewById(R.id.cbCarryForward);

        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
        btnSaveCategoryBudget = view.findViewById(R.id.btnSaveCategoryBudget);

        cardBudgetSummary = view.findViewById(R.id.cardBudgetSummary);
        tvSummaryMonth = view.findViewById(R.id.tvSummaryMonth);
        tvSummaryAmount = view.findViewById(R.id.tvSummaryAmount);
        tvSummaryCarry = view.findViewById(R.id.tvSummaryCarry);

        layoutCategoryBudgets = view.findViewById(R.id.layoutCategoryBudgets);
        tvRemainingBudget = view.findViewById(R.id.tvRemainingBudget);

        // ===== Init =====
        initDefaultDate();
        attachCategoryWatchers();

        // ===== Clicks =====
        btnMonthly.setOnClickListener(v -> selectMonthly());
        btnYearly.setOnClickListener(v -> selectYearly());
        tvSelectedMonth.setOnClickListener(v -> openPicker());

        btnSaveBudget.setOnClickListener(v -> saveMainBudget());

        // ⭐ FINAL ACTION
        btnSaveCategoryBudget.setOnClickListener(
                v -> openBudgetAnalysisScreen()
        );
    }

    // ================= DATE =================

    private void initDefaultDate() {
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        updateDateText();
    }

    private void updateDateText() {
        if (isMonthly) {
            String month =
                    new DateFormatSymbols().getMonths()[selectedMonth];
            tvSelectedMonth.setText(month + " " + selectedYear);
        } else {
            tvSelectedMonth.setText("Year " + selectedYear);
        }
    }

    private void selectMonthly() {
        isMonthly = true;
        btnMonthly.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1E88E5));
        btnYearly.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1F2A38));
        updateDateText();
    }

    private void selectYearly() {
        isMonthly = false;
        btnYearly.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1E88E5));
        btnMonthly.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1F2A38));
        updateDateText();
    }

    private void openPicker() {
        DatePickerDialog dialog =
                new DatePickerDialog(
                        requireContext(),
                        (view, year, month, day) -> {
                            selectedYear = year;
                            selectedMonth = month;
                            updateDateText();
                        },
                        selectedYear,
                        selectedMonth,
                        1
                );
        dialog.show();
    }

    // ================= CATEGORY WATCH =================

    private void attachCategoryWatchers() {
        for (int i = 0; i < layoutCategoryBudgets.getChildCount(); i++) {

            View row = layoutCategoryBudgets.getChildAt(i);
            if (!(row instanceof LinearLayout)) continue;

            LinearLayout line = (LinearLayout) row;
            if (line.getChildCount() < 2) continue;

            View v = line.getChildAt(1);
            if (v instanceof EditText) {
                ((EditText) v).addTextChangedListener(new CategoryWatcher());
            }
        }
    }

    private class CategoryWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
        @Override public void onTextChanged(CharSequence s,int a,int b,int c){
            updateRemaining();
        }
        @Override public void afterTextChanged(Editable s){}
    }

    private void updateRemaining() {

        String totalStr = etTotalBudget.getText().toString().trim();
        if (TextUtils.isEmpty(totalStr)) {
            tvRemainingBudget.setText("Remaining: ₹ 0");
            tvRemainingBudget.setTextColor(Color.GRAY);
            resetRowColors();
            return;
        }

        int total = Integer.parseInt(totalStr);
        int used = 0;

        for (int i = 0; i < layoutCategoryBudgets.getChildCount(); i++) {

            View row = layoutCategoryBudgets.getChildAt(i);
            if (!(row instanceof LinearLayout)) continue;

            LinearLayout line = (LinearLayout) row;
            View v = line.getChildAt(1);

            if (v instanceof EditText) {
                String val = ((EditText) v).getText().toString().trim();
                if (!val.isEmpty()) used += Integer.parseInt(val);
            }
        }

        int remaining = total - used;
        tvRemainingBudget.setText("Remaining: ₹ " + remaining);

        if (remaining < 0) {
            tvRemainingBudget.setTextColor(Color.RED);
            highlightRows();
        } else {
            tvRemainingBudget.setTextColor(Color.parseColor("#90CAF9"));
            resetRowColors();
        }
    }

    private void highlightRows() {
        for (int i = 0; i < layoutCategoryBudgets.getChildCount(); i++) {
            View row = layoutCategoryBudgets.getChildAt(i);
            if (row instanceof LinearLayout) {
                row.setBackgroundColor(Color.parseColor("#33FF5252"));
            }
        }
    }

    private void resetRowColors() {
        for (int i = 0; i < layoutCategoryBudgets.getChildCount(); i++) {
            View row = layoutCategoryBudgets.getChildAt(i);
            if (row instanceof LinearLayout) {
                row.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    // ================= SAVE MAIN =================

    private void saveMainBudget() {

        if (TextUtils.isEmpty(etTotalBudget.getText().toString())) {
            etTotalBudget.setError("Enter total budget");
            return;
        }

        cardBudgetSummary.setVisibility(View.VISIBLE);
        tvSummaryMonth.setText(tvSelectedMonth.getText().toString());
        tvSummaryAmount.setText("₹ " + etTotalBudget.getText().toString());
        tvSummaryCarry.setText(
                "Carry forward: " +
                        (cbCarryForward.isChecked() ? "Yes" : "No"));

        Toast.makeText(
                requireContext(),
                "Budget saved",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ================= NAVIGATION =================

    private void openBudgetAnalysisScreen() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        new BudgetAnalysisFragment()
                )
                .addToBackStack(null)
                .commit();
    }
}
