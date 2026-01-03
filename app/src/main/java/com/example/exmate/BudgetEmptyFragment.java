package com.example.exmate;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BudgetEmptyFragment extends Fragment {

    public BudgetEmptyFragment() {
        super(R.layout.fragment_budget_empty);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        view.findViewById(R.id.btnAddBudget)
                .setOnClickListener(v ->
                        requireActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(
                                        R.id.fragmentContainer,
                                        new BudgetFragment()
                                )
                                .addToBackStack(null)
                                .commit()
                );
    }
}
