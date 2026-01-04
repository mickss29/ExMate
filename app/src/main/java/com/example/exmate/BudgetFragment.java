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
import com.github.mikephil.charting.data.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BudgetFragment extends Fragment {

    private TextView tvTotal, tvSpent, tvAvailable;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private DatabaseReference rootRef;


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
    private ImageView btnPrevMonth, btnNextMonth;

    private Calendar calendar = Calendar.getInstance();




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
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);

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
        rootRef = FirebaseDatabase
                .getInstance("https://exmate-users-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();



        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonth();
        });


// ✅ ONLY load budgets here
// expenses will be loaded AFTER budgets finish loading
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

                        for (DataSnapshot ds : catSnap.getChildren()) {
                            String name = ds.getKey();
                            Integer amt = ds.getValue(Integer.class);
                            if (name == null || amt == null) continue;

                            categoryList.add(
                                    new BudgetCategoryModel(name, amt));
                            totalBudget += amt;
                        }

                        adapter.notifyDataSetChanged();
                        loadExpenses(); // 👈 IMPORTANT
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

                        java.text.SimpleDateFormat sdf =
                                new java.text.SimpleDateFormat("yyyy-MM",
                                        java.util.Locale.getDefault());

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            String category =
                                    ds.child("category").getValue(String.class);
                            Integer amount =
                                    ds.child("amount").getValue(Integer.class);
                            Long time =
                                    ds.child("time").getValue(Long.class);

                            if (category == null || amount == null || time == null)
                                continue;

                            if (!sdf.format(new java.util.Date(time))
                                    .equals(monthKey)) continue;

                            for (BudgetCategoryModel m : categoryList) {
                                if (m.getName()
                                        .equalsIgnoreCase(category)) {
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
        animateAmount(tvTotalBudget, 0, totalBudget);
        animateAmount(tvSpent, 0, totalSpent);
        animateAmount(tvAvailable, 0, Math.max(totalBudget - totalSpent, 0));


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

        if (pieChart == null) return;

        int remaining = Math.max(totalBudget - totalSpent, 0);

        float percentLeft = totalBudget == 0 ? 0f :
                (remaining * 100f) / totalBudget;

        // ===== ENTRIES =====
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

        // ===== COLOR LOGIC =====
        int leftColor;
        int spentColor = android.graphics.Color.parseColor("#E5E7EB");

        if (percentLeft > 40) {
            leftColor = android.graphics.Color.parseColor("#16A34A"); // green
        } else if (percentLeft > 15) {
            leftColor = android.graphics.Color.parseColor("#F59E0B"); // orange
        } else {
            leftColor = android.graphics.Color.parseColor("#DC2626"); // red
        }

        dataSet.setColors(leftColor, spentColor);

        com.github.mikephil.charting.data.PieData data =
                new com.github.mikephil.charting.data.PieData(dataSet);

        pieChart.setData(data);

        // ===== DONUT STYLE =====
        pieChart.setDrawCenterText(true);
        pieChart.setHoleRadius(72f);
        pieChart.setTransparentCircleRadius(76f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);

        // ===== CENTER TEXT BASE =====
        pieChart.setCenterText("0%\nLeft");
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(leftColor);

        // ===== CENTER TEXT ANIMATION =====
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0f, percentLeft);

        animator.setDuration(900);
        animator.setInterpolator(
                new android.view.animation.DecelerateInterpolator());

        animator.addUpdateListener(a -> {
            int value = Math.round((float) a.getAnimatedValue());
            pieChart.setCenterText(value + "%\nLeft");
        });

        animator.start();

        // ===== CHART ANIMATION =====
        pieChart.animateY(
                900,
                com.github.mikephil.charting.animation.Easing.EaseInOutCubic
        );

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
    private void animateAmount(TextView tv, int from, int to) {

        if (tv == null) return;   // 🔥 IMPORTANT FIX

        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofInt(from, to);

        animator.setDuration(700);
        animator.setInterpolator(
                new android.view.animation.DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            if (tv != null) { // extra safety during lifecycle
                int value = (int) animation.getAnimatedValue();
                tv.setText("₹" + value);
            }
        });

        animator.start();
    }



    private void updateMonth() {
        SimpleDateFormat titleFormat =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText(titleFormat.format(calendar.getTime()));

        monthKey = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault())
                .format(calendar.getTime());

        loadBudgets(); // reload data for selected month
    }



}
