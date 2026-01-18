package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BudgetFragment extends Fragment {

    private TextView tvTotalBudget, tvSpent, tvAvailable, tvUnallocated;
    private TextView tvMonthTitle, tvEmptyBudget;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private ImageView btnPrevMonth, btnNextMonth;
    private View layoutChart;

    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();
    private BudgetCategoryAdapter adapter;

    private int totalBudget = 0;
    private int totalSpent = 0;

    private DatabaseReference userRef;
    private String monthKey;
    private Calendar calendar = Calendar.getInstance();

    public BudgetFragment() {
        super(R.layout.fragment_budget);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // ===== UI =====
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvSpent = view.findViewById(R.id.tvSpent);
        tvAvailable = view.findViewById(R.id.tvAvailable);
        tvUnallocated = view.findViewById(R.id.tvUnallocated);
        tvEmptyBudget = view.findViewById(R.id.tvEmptyBudget);

        pieChart = view.findViewById(R.id.pieChart);
        layoutChart = view.findViewById(R.id.layoutChart);

        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);

        recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetCategoryAdapter(categoryList, this::openBudgetDialog);
        recyclerView.setAdapter(adapter);

        // ===== MONTH =====
        tvMonthTitle.setText(getMonthTitle());

        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonth();
        });

        // ===== FIREBASE =====
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(calendar.getTime());

        loadBudgets();
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

                        DataSnapshot catSnap = snapshot.child("categories");

                        // ===== EMPTY STATE =====
                        if (!catSnap.exists() || catSnap.getChildrenCount() == 0) {
                            showEmptyState();
                            createDefaultCategoriesIfEmpty(); // 👈 THIS LINE
                            return;
                        }


                        hideEmptyState();

                        for (DataSnapshot ds : catSnap.getChildren()) {
                            String name = ds.getKey();
                            Integer amt = ds.getValue(Integer.class);
                            if (name == null || amt == null) continue;

                            categoryList.add(new BudgetCategoryModel(name, amt));
                            totalBudget += amt;
                        }

                        adapter.notifyDataSetChanged();
                        loadExpenses();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ================= LOAD EXPENSES =================

    private void loadExpenses() {
        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        totalSpent = 0;

                        for (BudgetCategoryModel m : categoryList) {
                            m.setSpent(0);
                        }

                        SimpleDateFormat sdf =
                                new SimpleDateFormat("yyyy-MM", Locale.getDefault());

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            String category = ds.child("category").getValue(String.class);
                            Integer amount = ds.child("amount").getValue(Integer.class);
                            Long time = ds.child("time").getValue(Long.class);

                            if (category == null || amount == null || time == null) continue;
                            if (!sdf.format(new Date(time)).equals(monthKey)) continue;

                            for (BudgetCategoryModel m : categoryList) {
                                if (m.getName().equalsIgnoreCase(category)) {
                                    m.setSpent(m.getSpent() + amount);
                                    totalSpent += amount;
                                    break;
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateSummary();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ================= SUMMARY =================

    private void updateSummary() {
        if (!isAdded()) return;

        tvTotalBudget.setText("Total Budget\n₹" + totalBudget);
        tvSpent.setText("Spent\n₹" + totalSpent);
        tvAvailable.setText("Available\n₹" + Math.max(totalBudget - totalSpent, 0));

        int allocated = 0;
        for (BudgetCategoryModel m : categoryList) {
            allocated += m.getBudget();
        }
        tvUnallocated.setText("Unallocated\n₹" + (totalBudget - allocated));

        updatePieChart(totalBudget, totalSpent);
    }

    // ================= EMPTY STATE =================

    private void showEmptyState() {
        layoutChart.setVisibility(View.GONE);
        tvEmptyBudget.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        layoutChart.setVisibility(View.VISIBLE);
        tvEmptyBudget.setVisibility(View.GONE);
    }

    // ================= ADD / UPDATE =================

    private void openBudgetDialog(BudgetCategoryModel model) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_budget, null);

        EditText etAmount = v.findViewById(R.id.etAmount);

        new AlertDialog.Builder(getContext())
                .setTitle(model.getBudget() == 0 ? "Add Budget" : "Update Budget")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String input = etAmount.getText().toString().trim();
                    if (input.isEmpty()) return;

                    int amt = Integer.parseInt(input);

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

    // ================= PIE CHART =================

    private void updatePieChart(int totalBudget, int totalSpent) {
        if (pieChart == null || totalBudget == 0) return;

        int remaining = Math.max(totalBudget - totalSpent, 0);
        float percentLeft = (remaining * 100f) / totalBudget;

        List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();
        entries.add(new com.github.mikephil.charting.data.PieEntry(remaining, "Left"));
        entries.add(new com.github.mikephil.charting.data.PieEntry(totalSpent, "Spent"));

        com.github.mikephil.charting.data.PieDataSet dataSet =
                new com.github.mikephil.charting.data.PieDataSet(entries, "");

        int leftColor =
                percentLeft > 40 ? android.graphics.Color.parseColor("#16A34A") :
                        percentLeft > 15 ? android.graphics.Color.parseColor("#F59E0B") :
                                android.graphics.Color.parseColor("#DC2626");

        dataSet.setColors(leftColor, android.graphics.Color.parseColor("#E5E7EB"));
        dataSet.setDrawValues(false);

        pieChart.setData(new com.github.mikephil.charting.data.PieData(dataSet));
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(Math.round(percentLeft) + "%\nLeft");
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(leftColor);
        pieChart.setHoleRadius(72f);
        pieChart.setTransparentCircleRadius(76f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);
        pieChart.animateY(900);
        pieChart.invalidate();
    }

    private String getMonthTitle() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return "Spent in " + sdf.format(calendar.getTime());
    }

    private void updateMonth() {
        tvMonthTitle.setText(getMonthTitle());
        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(calendar.getTime());
        loadBudgets();
    }
    private void createDefaultCategoriesIfEmpty() {
        String[] defaults = {"Food", "Education", "Health", "Travel", "Other"};

        DatabaseReference catRef = userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .child("categories");

        for (String c : defaults) {
            catRef.child(c).setValue(0); // 0 means not set yet
        }
    }

}
