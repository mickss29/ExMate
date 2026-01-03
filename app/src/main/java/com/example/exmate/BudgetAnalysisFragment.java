package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BudgetAnalysisFragment extends Fragment {

    // Top summary values
    private TextView tvTotalBudget;
    private TextView tvUnallocatedBudget;
    private TextView tvTotalSpent;
    private TextView tvAvailableBudget;

    public BudgetAnalysisFragment() {
        super(R.layout.fragment_budget_analysis);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // ===== Bind views =====
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvUnallocatedBudget = view.findViewById(R.id.tvUnallocatedBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvAvailableBudget = view.findViewById(R.id.tvAvailableBudget);

        // ===== TEMP STATIC DATA =====
        showStaticData();
    }

    private void showStaticData() {

        // These values are dummy for now
        int totalBudget = 50000;
        int allocatedBudget = 5000;
        int spent = 0;

        int unallocated = totalBudget - allocatedBudget;
        int available = totalBudget - spent;

        tvTotalBudget.setText("₹ " + totalBudget);
        tvUnallocatedBudget.setText("₹ " + unallocated);
        tvTotalSpent.setText("₹ " + spent + " (From Budget)");
        tvAvailableBudget.setText("₹ " + available);
    }
}
