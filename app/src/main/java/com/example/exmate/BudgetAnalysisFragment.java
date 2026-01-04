package com.example.exmate;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAnalysisFragment extends Fragment {

    // ===== UI =====
    private TextView tvTotalBudget;
    private TextView tvTotalSpent;
    private TextView tvAvailableBudget;
    private TextView tvUnallocatedBudget;
    private PieChart pieChartBudget;
    private RecyclerView rvBudgetCategories;

    // ===== DATA =====
    private BudgetCategoryAdapter adapter;
    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();

    private int totalBudget = 0;
    private int totalSpent = 0;

    public BudgetAnalysisFragment() {
        super(R.layout.fragment_budget_analysis);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        // ===== Bind views =====
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvAvailableBudget = view.findViewById(R.id.tvAvailableBudget);
        tvUnallocatedBudget = view.findViewById(R.id.tvUnallocatedBudget);
        pieChartBudget = view.findViewById(R.id.pieChartBudget);

        rvBudgetCategories = view.findViewById(R.id.rvBudgetCategories);
        rvBudgetCategories.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new BudgetCategoryAdapter(categoryList);
        rvBudgetCategories.setAdapter(adapter);

        // ===== FAB (EDIT BUDGET) =====
        FloatingActionButton fab =
                view.findViewById(R.id.fabEditBudget);

        fab.setOnClickListener(v -> {

            // 🔒 lifecycle safety
            if (!isAdded()) return;

            Bundle b = new Bundle();
            b.putBoolean("isEdit", true);

            BudgetFragment fragment = new BudgetFragment();
            fragment.setArguments(b);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack("edit_budget")
                    .commit();
        });

        loadBudgetAndCategories();
    }

    // ================= LOAD BUDGET + CATEGORIES =================

    private void loadBudgetAndCategories() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getCurrentMonthKey());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                Integer tb =
                        snapshot.child("totalBudget")
                                .getValue(Integer.class);

                totalBudget = tb != null ? tb : 0;

                categoryList.clear();
                Map<String, BudgetCategoryModel> map = new HashMap<>();

                for (DataSnapshot ds :
                        snapshot.child("categories").getChildren()) {

                    String name = ds.getKey();
                    Integer amt = ds.getValue(Integer.class);

                    if (name == null || amt == null) continue;

                    BudgetCategoryModel model =
                            new BudgetCategoryModel(name, amt);

                    map.put(name, model);
                    categoryList.add(model);
                }

                loadExpenses(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ================= LOAD EXPENSES =================

    private void loadExpenses(
            Map<String, BudgetCategoryModel> categoryMap) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference expenseRef =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("expenses");

        expenseRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        totalSpent = 0;

                        for (DataSnapshot ds :
                                snapshot.getChildren()) {

                            String category =
                                    ds.child("category")
                                            .getValue(String.class);

                            Integer amount =
                                    ds.child("amount")
                                            .getValue(Integer.class);

                            if (category == null || amount == null)
                                continue;

                            BudgetCategoryModel model =
                                    categoryMap.get(category);

                            if (model != null) {
                                model.setSpent(
                                        model.getSpent() + amount
                                );
                                totalSpent += amount;
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateTopSummary();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                });
    }

    // ================= UPDATE TOP SUMMARY =================

    private void updateTopSummary() {

        int allocated = 0;
        for (BudgetCategoryModel m : categoryList) {
            allocated += m.getAmount();
        }

        int unallocated = totalBudget - allocated;
        int available = totalBudget - totalSpent;

        tvTotalBudget.setText("₹ " + totalBudget);
        tvTotalSpent.setText("₹ " + totalSpent);
        tvAvailableBudget.setText("₹ " + available);
        tvUnallocatedBudget.setText(
                "Unallocated\n₹" + unallocated
        );

        setupDonutChart(totalBudget, totalSpent);
    }

    // ================= PIE CHART =================

    private void setupDonutChart(int totalBudget, int totalSpent) {

        pieChartBudget.setUsePercentValues(true);
        pieChartBudget.getDescription().setEnabled(false);
        pieChartBudget.setDrawHoleEnabled(true);
        pieChartBudget.setHoleRadius(70f);
        pieChartBudget.setRotationEnabled(false);

        int remaining = Math.max(totalBudget - totalSpent, 0);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(remaining, "Left"));
        entries.add(new PieEntry(totalSpent, "Spent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);

        int percentUsed =
                totalBudget == 0 ? 0 :
                        (totalSpent * 100) / totalBudget;

        if (percentUsed < 80) {
            dataSet.setColors(
                    Color.parseColor("#16A34A"),
                    Color.parseColor("#E5E7EB")
            );
        } else if (percentUsed < 100) {
            dataSet.setColors(
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#E5E7EB")
            );
        } else {
            dataSet.setColors(
                    Color.parseColor("#EF4444"),
                    Color.parseColor("#E5E7EB")
            );
        }

        PieData data = new PieData(dataSet);
        data.setDrawValues(false);

        pieChartBudget.setData(data);
        pieChartBudget.invalidate();
    }

    private String getCurrentMonthKey() {
        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(new Date());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter.getItemCount() == 0) {
            loadBudgetAndCategories();
        }
    }
}
