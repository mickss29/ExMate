package com.example.exmate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;



public class BudgetAnalysisFragment extends Fragment {

    private TextView tvTotalBudget, tvUnallocatedBudget,
            tvTotalSpent, tvAvailableBudget;

    private int totalBudget = 0;
    private int allocatedBudget = 0;
    private int totalSpent = 0;
    private PieChart pieChartBudget;
    private RecyclerView rvBudgetCategories;
    private BudgetCategoryAdapter adapter;
    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();




    public BudgetAnalysisFragment() {
        super(R.layout.fragment_budget_analysis);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvUnallocatedBudget = view.findViewById(R.id.tvUnallocatedBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvAvailableBudget = view.findViewById(R.id.tvAvailableBudget);
        pieChartBudget = view.findViewById(R.id.pieChartBudget);
        rvBudgetCategories = view.findViewById(R.id.rvBudgetCategories);
        rvBudgetCategories.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new BudgetCategoryAdapter(categoryList);
        rvBudgetCategories.setAdapter(adapter);
        rvBudgetCategories = view.findViewById(R.id.rvBudgetCategories);
        rvBudgetCategories.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new BudgetCategoryAdapter(categoryList);
        rvBudgetCategories.setAdapter(adapter);

        loadCategoriesAndExpenses();




        loadBudgetAndExpenses();
        loadDummyCategories();

    }

    private void loadBudgetAndExpenses() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String monthKey = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault())
                .format(new Date());

        DatabaseReference userRef =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid);

        // ===== LOAD BUDGET =====
        userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snap) {

                                if (!snap.exists()) return;

                                totalBudget =
                                        snap.child("totalBudget")
                                                .getValue(Integer.class);

                                allocatedBudget = 0;
                                for (DataSnapshot c :
                                        snap.child("categories")
                                                .getChildren()) {
                                    Integer val =
                                            c.getValue(Integer.class);
                                    if (val != null)
                                        allocatedBudget += val;
                                }


                            }

                            @Override public void onCancelled(
                                    @NonNull DatabaseError error) {}
                        });
    }

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

                        int totalSpent = 0;

                        for (DataSnapshot ds : snapshot.getChildren()) {

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
                        updateTopSummary(totalSpent);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                });
    }

    private void updateUI() {

        int unallocated = totalBudget - allocatedBudget;
        int available = totalBudget - totalSpent;

        tvTotalBudget.setText("₹ " + totalBudget);
        tvUnallocatedBudget.setText("₹ " + unallocated);
        tvTotalSpent.setText("₹ " + totalSpent);
        tvAvailableBudget.setText("₹ " + available);
        setupDonutChart(totalBudget, totalSpent);

    }

    private long getMonthStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return c.getTimeInMillis();
    }

    private long getMonthEnd() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,
                c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return c.getTimeInMillis();
    }
    private void updateOverallProgress(
            int totalBudget,
            int totalSpent,
            ProgressBar progressOverall,
            TextView tvCenterText
    ) {

        if (totalBudget <= 0) {
            progressOverall.setProgress(0);
            tvCenterText.setText("0%");
            return;
        }

        int left = totalBudget - totalSpent;
        int percentLeft = (left * 100) / totalBudget;

        progressOverall.setProgress(percentLeft);
        tvCenterText.setText(percentLeft + "% Left");

        if (percentLeft < 20) {
            progressOverall.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#E53935"))
            );
        } else if (percentLeft < 50) {
            progressOverall.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#FB8C00"))
            );
        } else {
            progressOverall.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            );
        }
    }
    private void setupDonutChart(int totalBudget, int totalSpent) {

        pieChartBudget.setUsePercentValues(true);
        pieChartBudget.getDescription().setEnabled(false);
        pieChartBudget.setDrawHoleEnabled(true);
        pieChartBudget.setHoleRadius(75f);
        pieChartBudget.setTransparentCircleRadius(80f);
        pieChartBudget.setRotationEnabled(false);

        int remaining = Math.max(totalBudget - totalSpent, 0);

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(remaining, "Left"));
        entries.add(new PieEntry(totalSpent, "Spent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // ===== COLORS BASED ON USAGE =====
        int percentUsed =
                totalBudget == 0 ? 0 : (totalSpent * 100) / totalBudget;

        if (percentUsed < 80) {
            dataSet.setColors(
                    Color.parseColor("#16A34A"), // green
                    Color.parseColor("#E5E7EB")  // light gray
            );
        } else if (percentUsed < 100) {
            dataSet.setColors(
                    Color.parseColor("#F59E0B"), // orange
                    Color.parseColor("#E5E7EB")
            );
        } else {
            dataSet.setColors(
                    Color.parseColor("#EF4444"), // red
                    Color.parseColor("#E5E7EB")
            );
        }

        PieData data = new PieData(dataSet);
        data.setDrawValues(false);

        pieChartBudget.setData(data);

        // ===== CENTER TEXT =====
        int percentLeft =
                totalBudget == 0 ? 0 : (remaining * 100) / totalBudget;

        pieChartBudget.setCenterText(
                percentLeft + "%\nLeft"
        );
        pieChartBudget.setCenterTextSize(16f);
        pieChartBudget.setCenterTextColor(Color.parseColor("#111827"));

        pieChartBudget.invalidate(); // refresh
    }
    private void loadDummyCategories() {
        categoryList.clear();

        categoryList.add(new BudgetCategoryModel("Food", 3000));
        categoryList.add(new BudgetCategoryModel("Transport", 1500));
        categoryList.add(new BudgetCategoryModel("Entertainment", 2000));
        categoryList.add(new BudgetCategoryModel("Health", 1000));

        adapter.notifyDataSetChanged();
    }
    private void loadCategoriesAndExpenses() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // 1️⃣ Load budget categories first (from budget data)
        DatabaseReference budgetRef =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getCurrentMonthKey())
                        .child("categories");

        budgetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                categoryList.clear();
                Map<String, BudgetCategoryModel> map = new HashMap<>();

                // Load categories + budget
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.getKey();
                    int budget = ds.getValue(Integer.class);

                    BudgetCategoryModel model =
                            new BudgetCategoryModel(name, budget);
                    map.put(name, model);
                    categoryList.add(model);
                }

                // 2️⃣ Load expenses and map spent
                loadExpenses(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void updateTopSummary(int totalSpent) {

        int totalBudget = 0;
        for (BudgetCategoryModel m : categoryList) {
            totalBudget += m.getAmount();
        }

        int available = totalBudget - totalSpent;

        tvTotalBudget.setText("₹ " + totalBudget);
        tvTotalSpent.setText("₹ " + totalSpent);
        tvAvailableBudget.setText("₹ " + available);

        setupDonutChart(totalBudget, totalSpent);
    }
    private String getCurrentMonthKey() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM",
                        java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }


}
