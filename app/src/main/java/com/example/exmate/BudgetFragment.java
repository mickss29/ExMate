package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class BudgetFragment extends Fragment {

    // -------- UI --------
    private TextView tvTotalBudget, tvSpentBudget, tvRemainingBudget, tvPercentage;
    private MaterialButton btnAddCategory;
    private RecyclerView rvCategories;

    // -------- Local Data (temporary until Firebase comes) --------
    private final ArrayList<CategoryModel> categoryList = new ArrayList<>();
    private BudgetCategoryAdapter categoryAdapter;

    private long totalBudget = 0;
    private long totalSpent = 0;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.budget_fragment, container, false);

        initViews(view);
        setupRecycler();
        setupClicks();

        // load dummy/local data
        loadSampleData();
        updateSummary();

        return view;
    }

    // -------- Init UI --------
    private void initViews(View view) {
        tvTotalBudget     = view.findViewById(R.id.tvTotalBudget);
        tvSpentBudget     = view.findViewById(R.id.tvSpentBudget);
        tvRemainingBudget = view.findViewById(R.id.tvRemainingBudget);
        tvPercentage      = view.findViewById(R.id.tvPercentage);
        btnAddCategory    = view.findViewById(R.id.btnAddCategory);
        rvCategories      = view.findViewById(R.id.rvCategories);
    }

    // -------- Recycler setup --------
    private void setupRecycler() {
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new BudgetCategoryAdapter(requireContext(), categoryList);
        rvCategories.setAdapter(categoryAdapter);
    }

    // -------- Add button --------
    private void setupClicks() {
        btnAddCategory.setOnClickListener(v -> {
            Intent i = new Intent(requireActivity(), AddCategoryActivity.class);
            startActivity(i);
        });
    }

    // -------- TEMP DATA until Firebase connected --------
    private void loadSampleData() {

        categoryList.clear();

        categoryList.add(new CategoryModel("1", "Rent",       10000, 6000, "ic_budget_home"));
        categoryList.add(new CategoryModel("2", "Food",        5000,  4200, "ic_budget_food"));
        categoryList.add(new CategoryModel("3", "Entertainment",3000,  1000, "ic_budget_entertainment"));
        categoryList.add(new CategoryModel("4", "Bills",       4000,  3500, "ic_budget_bills"));

        // refresh recycler
        categoryAdapter.notifyDataSetChanged();
    }

    // -------- Calculate summary from local data --------
    private void updateSummary() {

        totalBudget = 0;
        totalSpent  = 0;

        for (CategoryModel model : categoryList) {
            totalBudget += model.limit;
            totalSpent  += model.spent;
        }

        long remaining = totalBudget - totalSpent;
        int percent = totalBudget > 0 ? (int) ((totalSpent * 100) / totalBudget) : 0;

        tvTotalBudget.setText("₹" + totalBudget);
        tvSpentBudget.setText("₹" + totalSpent);
        tvRemainingBudget.setText("₹" + remaining);
        tvPercentage.setText(percent + "% used");
    }

    @Override
    public void onResume() {
        super.onResume();
        // if AddCategoryActivity modifies local list later, update summary again
        updateSummary();
    }
}
