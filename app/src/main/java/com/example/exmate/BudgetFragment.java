package com.example.exmate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment {

    private RecyclerView recyclerCategories;
    private BudgetCategoryAdapter categoryAdapter;
    private List<BudgetCategoryModel> categoryList;

    private EditText etTotalBudget;
    private Button btnSaveBudget;
    private TextView tabMonthly, tabYearly, txtBudgetFor;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupCategoryGrid();
        setupToggle();
        setupSave();
    }

    // ================= INIT =================
    private void initViews(View view) {
        recyclerCategories = view.findViewById(R.id.recyclerBudgetCategories);
        etTotalBudget = view.findViewById(R.id.etTotalBudget);
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);

        tabMonthly = view.findViewById(R.id.tabMonthly);
        tabYearly = view.findViewById(R.id.tabYearly);
        txtBudgetFor = view.findViewById(R.id.txtBudgetFor);
    }

    // ================= CATEGORY GRID =================
    private void setupCategoryGrid() {

        categoryList = new ArrayList<>();

        categoryList.add(new BudgetCategoryModel("Food", R.drawable.ic_food));
        categoryList.add(new BudgetCategoryModel("Transport", R.drawable.ic_transport));
        categoryList.add(new BudgetCategoryModel("Shopping", R.drawable.ic_shopping));
        categoryList.add(new BudgetCategoryModel("Bills", R.drawable.ic_bills));
        categoryList.add(new BudgetCategoryModel("Entertainment", R.drawable.ic_entertainment));
        categoryList.add(new BudgetCategoryModel("Health", R.drawable.ic_health));
        categoryList.add(new BudgetCategoryModel("Education", R.drawable.ic_education));
        categoryList.add(new BudgetCategoryModel("Travel", R.drawable.ic_transport));
        categoryList.add(new BudgetCategoryModel("Other", R.drawable.ic_category_default));

        categoryAdapter = new BudgetCategoryAdapter(categoryList);

        recyclerCategories.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerCategories.setAdapter(categoryAdapter);
        recyclerCategories.setNestedScrollingEnabled(false);
    }

    // ================= MONTHLY / YEARLY =================
    private void setupToggle() {

        tabMonthly.setOnClickListener(v -> {
            tabMonthly.setBackgroundResource(R.drawable.bg_tab_active);
            tabYearly.setBackground(null);

            tabMonthly.setTextColor(getResources().getColor(R.color.black));
            tabYearly.setTextColor(getResources().getColor(R.color.white));

            txtBudgetFor.setText("January 2026");
        });

        tabYearly.setOnClickListener(v -> {
            tabYearly.setBackgroundResource(R.drawable.bg_tab_active);
            tabMonthly.setBackground(null);

            tabYearly.setTextColor(getResources().getColor(R.color.black));
            tabMonthly.setTextColor(getResources().getColor(R.color.white));

            txtBudgetFor.setText("Year 2026");
        });
    }

    // ================= SAVE =================
    private void setupSave() {

        btnSaveBudget.setOnClickListener(v -> {

            String budgetStr = etTotalBudget.getText().toString().trim();
            if (budgetStr.isEmpty()) {
                etTotalBudget.setError("Enter budget amount");
                return;
            }

            List<BudgetCategoryModel> selected =
                    categoryAdapter.getSelectedCategories();

            if (selected.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Select at least one category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 For now just confirm (Firebase later)
            Toast.makeText(requireContext(),
                    "Budget saved (" + selected.size() + " categories)",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
