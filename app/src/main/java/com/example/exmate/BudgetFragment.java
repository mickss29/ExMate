package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BudgetFragment extends Fragment {

    private TextView tvTotal, tvSpent, tvAvailable;
    private PieChart pieChart;
    private RecyclerView recyclerView;

    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();
    private BudgetCategoryAdapter adapter;

    private int totalBudget = 0;
    private int totalSpent = 0;

    private DatabaseReference userRef;
    private String monthKey;
    // ===== SUMMARY VIEWS =====
    private TextView tvMonthTitle;
    private TextView tvTotalBudget;

    private TextView tvUnallocated;


    private RecyclerView rvCategories;


    public BudgetFragment() {
        super(R.layout.fragment_budget);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        tvTotal = view.findViewById(R.id.tvTotalBudget);
        tvSpent = view.findViewById(R.id.tvSpent);
        tvAvailable = view.findViewById(R.id.tvAvailable);
        pieChart = view.findViewById(R.id.pieChart);

        recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new BudgetCategoryAdapter(categoryList, this::openBudgetDialog);
        recyclerView.setAdapter(adapter);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvSpent = view.findViewById(R.id.tvSpent);
        tvAvailable = view.findViewById(R.id.tvAvailable);
        tvUnallocated = view.findViewById(R.id.tvUnallocated);

        pieChart = view.findViewById(R.id.pieChart);

// Month title
        tvMonthTitle.setText(getMonthTitle());


        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        loadBudgets();
        loadExpenses();
    }

    // ================= LOAD BUDGET =================

    private void loadBudgets() {

        userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        categoryList.clear();
                        totalBudget = 0;

                        if (snapshot.exists()) {

                            for (DataSnapshot ds :
                                    snapshot.child("categories").getChildren()) {

                                String name = ds.getKey();
                                Integer amt = ds.getValue(Integer.class);

                                if (name == null || amt == null) continue;

                                BudgetCategoryModel model =
                                        new BudgetCategoryModel(name);
                                model.setBudget(amt);

                                categoryList.add(model);
                                totalBudget += amt;
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateSummary();
                        updatePieChart(totalBudget, totalSpent);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ================= LOAD EXPENSES =================

    private void loadExpenses() {

        userRef.child("expenses")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        android.util.Log.d("BUDGET_DEBUG",
                                "Expenses count = " + snapshot.getChildrenCount());

                        totalSpent = 0;

                        // Reset spent
                        for (BudgetCategoryModel m : categoryList) {
                            m.setSpent(0);
                        }

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            android.util.Log.d("BUDGET_DEBUG",
                                    "Expense raw = " + ds.getValue());

                            String category =
                                    ds.child("category").getValue(String.class);

                            Integer amount =
                                    ds.child("amount").getValue(Integer.class);

                            android.util.Log.d("BUDGET_DEBUG",
                                    "category=" + category + " amount=" + amount);

                            if (category == null || amount == null) continue;

                            for (BudgetCategoryModel m : categoryList) {
                                if (m.getName().equalsIgnoreCase(category.trim())) {
                                    m.setSpent(m.getSpent() + amount);
                                    totalSpent += amount;
                                    android.util.Log.d("BUDGET_DEBUG",
                                            "MATCHED -> " + m.getName());
                                    break;
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateSummary();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("BUDGET_DEBUG",
                                "Error: " + error.getMessage());
                    }
                });
    }



    // ================= SUMMARY =================

    private void updateSummary() {

        if (!isAdded()) return;   // Fragment safety

        if (tvTotalBudget != null)
            tvTotalBudget.setText("Total Budget\n₹" + totalBudget);

        if (tvSpent != null)
            tvSpent.setText("Spent\n₹" + totalSpent);

        if (tvAvailable != null)
            tvAvailable.setText("Available\n₹" + (totalBudget - totalSpent));

        if (tvUnallocated != null) {
            int allocated = 0;
            for (BudgetCategoryModel m : categoryList) {
                allocated += m.getBudget();
            }
            tvUnallocated.setText(
                    "Unallocated\n₹" + (totalBudget - allocated));
        }

        // Update chart ONLY if view exists
        if (pieChart != null) {
            updatePieChart(totalBudget, totalSpent);
        }
    }



    // ================= ADD / UPDATE =================

    private void openBudgetDialog(BudgetCategoryModel model) {

        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_budget, null);

        EditText etAmount = v.findViewById(R.id.etAmount);

        new AlertDialog.Builder(getContext())
                .setTitle(model.getBudget() == 0 ?
                        "Add Budget" : "Update Budget")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {

                    int amt = Integer.parseInt(
                            etAmount.getText().toString());

                    userRef.child("budgets")
                            .child("monthly")
                            .child(monthKey)
                            .child("categories")
                            .child(model.getName())
                            .setValue(amt);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void updatePieChart(int totalBudget, int totalSpent) {

        int remaining = Math.max(totalBudget - totalSpent, 0);

        float percentLeft = totalBudget == 0 ? 0 :
                (remaining * 100f) / totalBudget;

        List<com.github.mikephil.charting.data.PieEntry> entries =
                new ArrayList<>();

        entries.add(new com.github.mikephil.charting.data.PieEntry(
                remaining, "Left"));
        entries.add(new com.github.mikephil.charting.data.PieEntry(
                totalSpent, "Spent"));

        com.github.mikephil.charting.data.PieDataSet dataSet =
                new com.github.mikephil.charting.data.PieDataSet(entries, "");

        dataSet.setSliceSpace(3f);
        dataSet.setDrawValues(false);

        // 🎨 COLORS
        dataSet.setColors(
                android.graphics.Color.parseColor("#16A34A"), // Green
                android.graphics.Color.parseColor("#E5E7EB")  // Light grey
        );

        com.github.mikephil.charting.data.PieData data =
                new com.github.mikephil.charting.data.PieData(dataSet);

        pieChart.setData(data);

        // ===== CENTER TEXT =====
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(
                Math.round(percentLeft) + "%\nLeft");
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(
                android.graphics.Color.parseColor("#16A34A"));

        // ===== DONUT STYLE =====
        pieChart.setHoleRadius(70f);
        pieChart.setTransparentCircleRadius(74f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);

        // ===== SMOOTH ANIMATION =====
        pieChart.animateY(900,
                com.github.mikephil.charting.animation.Easing.EaseInOutQuad);

        pieChart.invalidate();
    }
    private String getMonthTitle() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault());
        return "Spent in " + sdf.format(new java.util.Date());
    }
    private String extractMonthKey(String date) {
        try {
            // Case 1: yyyy-MM-dd
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return date.substring(0, 7);
            }

            // Case 2: dd/MM/yyyy
            if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                String[] parts = date.split("/");
                return parts[2] + "-" + parts[1];
            }

            // Case 3: timestamp stored as string
            long time = Long.parseLong(date);
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(time));

        } catch (Exception e) {
            return "";
        }
    }


}
