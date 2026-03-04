package com.example.exmate;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.Description;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class TransactionAnalysisFragment extends Fragment {

    private PieChart pieChart;
    private LineChart lineChart;
    private BarChart barChart;

    private TextView txtInsight;
    private TextView tvIncome, tvExpense, tvSavings;

    private ChipGroup chipGroupFilter;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    private float totalIncome = 0;
    private float totalExpense = 0;

    @Nullable
    @Override


    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_transaction_analysis, container, false);

        // ================= CHARTS =================
        pieChart = view.findViewById(R.id.pieChart);
        lineChart = view.findViewById(R.id.lineChart);
        barChart = view.findViewById(R.id.barChart);

        // ================= TEXT =================
        txtInsight = view.findViewById(R.id.txtInsight);

        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvSavings = view.findViewById(R.id.tvSavings);

        // ================= FILTER =================
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        // ================= FIREBASE =================
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            return view; // safety check
        }

        String uid = auth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        // ================= INIT =================
        setupFilters();

        // Default load (Month)
        loadMonthAnalytics();

        // Heatmap
        loadHeatmap(view);

        return view;
    }
    // ================= FILTER =================

    private void setupFilter() {

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.chipToday) {
                loadAnalytics();
            }

            if (checkedId == R.id.chipMonth) {
                loadAnalytics();
            }

            if (checkedId == R.id.chipYear) {
                loadAnalytics();
            }
        });
    }

    // ================= LOAD ANALYTICS =================

    private void loadAnalytics() {

        HashMap<String, Float> categoryMap = new HashMap<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();

        totalIncome = 0;
        totalExpense = 0;

        userRef.child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int index = 0;

                for (DataSnapshot data : snapshot.getChildren()) {

                    String category = data.child("category").getValue(String.class);
                    Double amountObj = data.child("amount").getValue(Double.class);

                    if (amountObj == null) continue;

                    float amount = amountObj.floatValue();

                    totalExpense += amount;

                    if (category == null) category = "Other";

                    if (!categoryMap.containsKey(category))
                        categoryMap.put(category, amount);
                    else
                        categoryMap.put(category, categoryMap.get(category) + amount);

                    lineEntries.add(new Entry(index++, amount));
                }

                loadIncome(categoryMap, lineEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= LOAD INCOME =================

    private void loadIncome(HashMap<String, Float> categoryMap, ArrayList<Entry> lineEntries) {

        userRef.child("incomes").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot data : snapshot.getChildren()) {

                    Double amountObj = data.child("amount").getValue(Double.class);

                    if (amountObj != null)
                        totalIncome += amountObj.floatValue();
                }

                updateCards();

                loadPieChart(categoryMap);
                loadLineChart(lineEntries);
                loadBarChart(totalIncome, totalExpense);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= UPDATE SUMMARY CARDS =================

    private void updateCards() {

        float savings = totalIncome - totalExpense;

        tvIncome.setText("₹" + (int) totalIncome);
        tvExpense.setText("₹" + (int) totalExpense);
        tvSavings.setText("₹" + (int) savings);
    }

    // ================= PIE CHART =================

    private void loadPieChart(HashMap<String, Float> categoryMap) {

        ArrayList<PieEntry> entries = new ArrayList<>();

        for (String category : categoryMap.keySet()) {
            entries.add(new PieEntry(categoryMap.get(category), category));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);

        dataSet.setColors(
                Color.parseColor("#4ADE80"),
                Color.parseColor("#60A5FA"),
                Color.parseColor("#FBBF24"),
                Color.parseColor("#F87171"),
                Color.parseColor("#C084FC"),
                Color.parseColor("#22D3EE")
        );

        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(13f);

        pieChart.setData(data);

        pieChart.setHoleRadius(65f);
        pieChart.setTransparentCircleRadius(70f);

        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(16f);

        pieChart.getLegend().setTextColor(Color.WHITE);

        pieChart.getDescription().setEnabled(false);

        pieChart.animateY(1400);

        pieChart.invalidate();
    }
    // ================= LINE CHART =================

    private void loadLineChart(ArrayList<Entry> entries) {

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");

        dataSet.setColor(Color.parseColor("#22D3EE"));
        dataSet.setCircleColor(Color.parseColor("#22D3EE"));

        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);

        dataSet.setDrawValues(false);

        LineData data = new LineData(dataSet);

        lineChart.setData(data);

        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisRight().setEnabled(false);

        lineChart.getLegend().setTextColor(Color.WHITE);

        lineChart.getDescription().setEnabled(false);

        lineChart.animateX(1200);

        lineChart.invalidate();
    }

    // ================= BAR CHART =================

    private void loadBarChart(float income, float expense) {

        ArrayList<BarEntry> entries = new ArrayList<>();

        entries.add(new BarEntry(0, income));
        entries.add(new BarEntry(1, expense));

        BarDataSet dataSet = new BarDataSet(entries, "Income vs Expense");

        dataSet.setColors(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#F44336")
        );

        BarData data = new BarData(dataSet);

        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);

        barChart.setData(data);
        barChart.animateY(1200);

        Description description = new Description();
        description.setText("");
        barChart.setDescription(description);

        barChart.invalidate();
    }

    // ================= BUDGET INSIGHT =================

    private void loadBudgetInsight() {

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        userRef.child("budgets")
                .child("monthly")
                .child(monthKey)
                .child("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        float totalBudget = 0;

                        for (DataSnapshot cat : snapshot.getChildren()) {

                            Double amountObj = cat.getValue(Double.class);

                            if (amountObj != null)
                                totalBudget += amountObj;
                        }

                        txtInsight.setText(
                                "📊 Monthly Budget: ₹" + (int) totalBudget +
                                        "\nIncome: ₹" + (int) totalIncome +
                                        "\nExpense: ₹" + (int) totalExpense +
                                        "\nSavings: ₹" + (int) (totalIncome - totalExpense)
                        );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void setupFilters() {

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.chipToday) {
                loadTodayAnalytics();
            }

            else if (checkedId == R.id.chipMonth) {
                loadMonthAnalytics();
            }

            else if (checkedId == R.id.chipYear) {
                loadYearAnalytics();
            }
        });
    }
    private void loadHeatmap(View view) {

        GridLayout grid = view.findViewById(R.id.heatmapGrid);

        grid.removeAllViews();

        Random random = new Random();

        for(int i=0;i<30;i++){

            View cell = new View(getContext());

            int size = dpToPx(22);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            params.width = size;
            params.height = size;
            params.setMargins(4,4,4,4);

            cell.setLayoutParams(params);

            int level = random.nextInt(4);

            int color;

            switch(level){

                case 0: color = Color.parseColor("#1F2937"); break;
                case 1: color = Color.parseColor("#4ADE80"); break;
                case 2: color = Color.parseColor("#FACC15"); break;
                default: color = Color.parseColor("#EF4444");
            }

            cell.setBackgroundColor(color);

            grid.addView(cell);
        }
    }
    private int dpToPx(int dp){
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void loadTodayAnalytics() {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();

        loadFilteredExpenses(start, end);
    }
    private void loadMonthAnalytics() {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();

        loadFilteredExpenses(start, end);
    }
    private void loadYearAnalytics() {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();

        loadFilteredExpenses(start, end);
    }
    private void loadFilteredExpenses(long start, long end) {

        HashMap<String, Float> categoryMap = new HashMap<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();

        totalExpense = 0;
        totalIncome = 0;

        userRef.child("expenses")
                .orderByChild("time")
                .startAt(start)
                .endAt(end)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        int index = 0;

                        for (DataSnapshot data : snapshot.getChildren()) {

                            String category = data.child("category").getValue(String.class);
                            Double amountObj = data.child("amount").getValue(Double.class);

                            if (amountObj == null) continue;

                            float amount = amountObj.floatValue();

                            totalExpense += amount;

                            if (category == null) category = "Other";

                            if (!categoryMap.containsKey(category))
                                categoryMap.put(category, amount);
                            else
                                categoryMap.put(category, categoryMap.get(category) + amount);

                            lineEntries.add(new Entry(index++, amount));
                        }

                        loadIncomeFiltered(categoryMap, lineEntries);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void loadIncomeFiltered(HashMap<String, Float> categoryMap, ArrayList<Entry> lineEntries) {

        userRef.child("incomes")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot data : snapshot.getChildren()) {

                            Double amountObj = data.child("amount").getValue(Double.class);

                            if (amountObj != null)
                                totalIncome += amountObj.floatValue();
                        }

                        updateCards();

                        loadPieChart(categoryMap);
                        loadLineChart(lineEntries);
                        loadBarChart(totalIncome, totalExpense);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

}