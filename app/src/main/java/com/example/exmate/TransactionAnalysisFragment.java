package com.example.exmate;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TransactionAnalysisFragment extends Fragment {

    // ─── Charts ─────────────────────────────────────────────
    private PieChart  pieChart;
    private LineChart lineChart;
    private BarChart  barChart;
    private LineChart forecastChart;

    // ─── Summary cards ───────────────────────────────────────
    private TextView tvIncome, tvExpense, tvSavings;

    // ─── Insight text ────────────────────────────────────────
    private TextView txtInsight;

    // ─── Filter chips ────────────────────────────────────────
    private ChipGroup chipGroupFilter;

    // ─── Budget progress ─────────────────────────────────────
    private LinearLayout budgetProgressContainer;

    // ─── Forecast views ──────────────────────────────────────
    private TextView tvSpentSoFar, tvProjected, tvDailyAvg, tvForecastMessage;

    // ─── AI Insight views ────────────────────────────────────
    private TextView     tvAiInsight;
    private LinearLayout aiLoadingLayout;
    private com.google.android.material.button.MaterialButton btnGetInsights;

    // ─── Firebase ────────────────────────────────────────────
    private FirebaseAuth      auth;
    private DatabaseReference userRef;

    // ─── State ───────────────────────────────────────────────
    private float totalIncome  = 0;
    private float totalExpense = 0;
    private HashMap<String, Float> latestCategoryMap = new HashMap<>();

    // ─── OkHttp + Gemini ─────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();

    // !! Replace with your key from aistudio.google.com !!
    private static final String GEMINI_API_KEY = "AIzaSyAXdnsrwIKEblkW0jjzFyBR6n0ZdoTnXO0";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash-lite:generateContent?key=" + GEMINI_API_KEY;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_transaction_analysis, container, false);

        // Charts
        pieChart      = view.findViewById(R.id.pieChart);
        lineChart     = view.findViewById(R.id.lineChart);
        barChart      = view.findViewById(R.id.barChart);
        forecastChart = view.findViewById(R.id.forecastChart);

        // Summary
        tvIncome  = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvSavings = view.findViewById(R.id.tvSavings);

        // Insight
        txtInsight = view.findViewById(R.id.txtInsight);

        // Budget progress
        budgetProgressContainer = view.findViewById(R.id.budgetProgressContainer);

        // Forecast
        tvSpentSoFar      = view.findViewById(R.id.tvSpentSoFar);
        tvProjected       = view.findViewById(R.id.tvProjected);
        tvDailyAvg        = view.findViewById(R.id.tvDailyAvg);
        tvForecastMessage = view.findViewById(R.id.tvForecastMessage);

        // AI
        tvAiInsight     = view.findViewById(R.id.tvAiInsight);
        aiLoadingLayout = view.findViewById(R.id.aiLoadingLayout);
        btnGetInsights  = view.findViewById(R.id.btnGetInsights);
        btnGetInsights.setOnClickListener(v -> callGeminiInsights());

        // Filter
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        // Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return view;

        String uid = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        setupChartAppearance();
        setupForecastChart();
        setupFilters();
        loadMonthAnalytics();

        return view;
    }

    // ═══════════════════════════════════════════════════════════
    //  CHART APPEARANCE
    // ═══════════════════════════════════════════════════════════

    private void setupChartAppearance() {

        final int BG = Color.parseColor("#111827");

        // ── Pie Chart ──
        pieChart.setBackgroundColor(BG);
        pieChart.setHoleColor(BG);
        pieChart.setTransparentCircleColor(BG);
        pieChart.setHoleRadius(62f);
        pieChart.setTransparentCircleRadius(67f);
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(15f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        styleLegend(pieChart.getLegend());

        // ── Line Chart ──
        lineChart.setBackgroundColor(BG);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        styleAxis(lineChart.getXAxis(), XAxis.XAxisPosition.BOTTOM);
        styleYAxis(lineChart.getAxisLeft());
        styleLegend(lineChart.getLegend());
        lineChart.setGridBackgroundColor(BG);
        lineChart.setDrawGridBackground(false);

        // ── Bar Chart ──
        barChart.setBackgroundColor(BG);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        styleAxis(barChart.getXAxis(), XAxis.XAxisPosition.BOTTOM);
        styleYAxis(barChart.getAxisLeft());

        barChart.setRenderer(new RoundedBarChartRenderer(
                barChart,
                barChart.getAnimator(),
                barChart.getViewPortHandler(),
                12f,
                requireContext()
        ));

        barChart.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(new String[]{"Income", "Expense"}));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setLabelCount(2);
        barChart.getAxisLeft().setValueFormatter(new LargeValueFormatter());
    }

    private void setupForecastChart() {
        final int BG = Color.parseColor("#111827");
        forecastChart.setBackgroundColor(BG);
        forecastChart.getDescription().setEnabled(false);
        forecastChart.getAxisRight().setEnabled(false);
        forecastChart.setDrawGridBackground(false);
        forecastChart.setTouchEnabled(true);
        forecastChart.setDragEnabled(true);
        forecastChart.setScaleEnabled(false);
        forecastChart.setExtraBottomOffset(12f);
        forecastChart.setExtraLeftOffset(8f);

        XAxis xAxis = forecastChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#9CA3AF"));
        xAxis.setGridColor(Color.parseColor("#1F2937"));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        YAxis yAxis = forecastChart.getAxisLeft();
        yAxis.setTextColor(Color.parseColor("#9CA3AF"));
        yAxis.setGridColor(Color.parseColor("#1F2937"));
        yAxis.setAxisMinimum(0f);
        yAxis.setValueFormatter(new LargeValueFormatter());
        yAxis.setTextSize(10f);

        Legend legend = forecastChart.getLegend();
        styleLegend(legend);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
    }

    private void styleAxis(XAxis axis, XAxis.XAxisPosition pos) {
        axis.setPosition(pos);
        axis.setTextColor(Color.parseColor("#9CA3AF"));
        axis.setGridColor(Color.parseColor("#1F2937"));
        axis.setAxisLineColor(Color.parseColor("#374151"));
        axis.setDrawGridLines(true);
        axis.setTextSize(11f);
    }

    private void styleYAxis(YAxis axis) {
        axis.setTextColor(Color.parseColor("#9CA3AF"));
        axis.setGridColor(Color.parseColor("#1F2937"));
        axis.setAxisLineColor(Color.parseColor("#374151"));
        axis.setTextSize(11f);
    }

    private void styleLegend(Legend legend) {
        legend.setTextColor(Color.parseColor("#D1D5DB"));
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(10f);
    }

    // ═══════════════════════════════════════════════════════════
    //  FILTER CHIPS
    // ═══════════════════════════════════════════════════════════

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if      (checkedId == R.id.chipToday) loadTodayAnalytics();
            else if (checkedId == R.id.chipMonth) loadMonthAnalytics();
            else if (checkedId == R.id.chipYear)  loadYearAnalytics();
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  TIME-RANGE HELPERS
    // ═══════════════════════════════════════════════════════════

    private void loadTodayAnalytics() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE,      0);
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);
        loadFilteredData(cal.getTimeInMillis(), System.currentTimeMillis());
    }

    private void loadMonthAnalytics() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY,  0);
        cal.set(Calendar.MINUTE,       0);
        cal.set(Calendar.SECOND,       0);
        cal.set(Calendar.MILLISECOND,  0);
        loadFilteredData(cal.getTimeInMillis(), System.currentTimeMillis());
    }

    private void loadYearAnalytics() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR,  1);
        cal.set(Calendar.HOUR_OF_DAY,  0);
        cal.set(Calendar.MINUTE,       0);
        cal.set(Calendar.SECOND,       0);
        cal.set(Calendar.MILLISECOND,  0);
        loadFilteredData(cal.getTimeInMillis(), System.currentTimeMillis());
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD FILTERED EXPENSES
    // ═══════════════════════════════════════════════════════════

    private void loadFilteredData(long start, long end) {

        HashMap<String, Float>  categoryMap = new HashMap<>();
        TreeMap<Integer, Float> dailyTotals = new TreeMap<>();
        HashMap<Integer, Float> dailyMap    = new HashMap<>();

        totalExpense = 0;
        totalIncome  = 0;

        userRef.child("expenses")
                .orderByChild("time")
                .startAt(start)
                .endAt(end)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot data : snapshot.getChildren()) {

                            String category  = data.child("category").getValue(String.class);
                            Double amountObj = data.child("amount").getValue(Double.class);
                            Long   timeObj   = data.child("time").getValue(Long.class);

                            if (amountObj == null) continue;

                            float amount = amountObj.floatValue();
                            totalExpense += amount;

                            if (category == null) category = "Other";
                            categoryMap.merge(category, amount, Float::sum);

                            if (timeObj != null) {
                                Calendar c = Calendar.getInstance();
                                c.setTimeInMillis(timeObj);
                                int day = c.get(Calendar.DAY_OF_MONTH);
                                dailyTotals.merge(day, amount, Float::sum);
                                dailyMap.merge(day, amount, Float::sum);
                            }
                        }

                        loadIncomeFiltered(categoryMap, dailyTotals, dailyMap, start, end);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD FILTERED INCOME
    // ═══════════════════════════════════════════════════════════

    private void loadIncomeFiltered(
            HashMap<String, Float>  categoryMap,
            TreeMap<Integer, Float> dailyTotals,
            HashMap<Integer, Float> dailyMap,
            long start,
            long end
    ) {
        userRef.child("incomes")
                .orderByChild("time")
                .startAt(start)
                .endAt(end)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Double amountObj = data.child("amount").getValue(Double.class);
                            if (amountObj != null) totalIncome += amountObj.floatValue();
                        }

                        latestCategoryMap = categoryMap;

                        updateCards();
                        loadPieChart(categoryMap);
                        loadLineChart(dailyTotals);
                        loadBarChart(totalIncome, totalExpense);
                        loadHeatmap(dailyMap);
                        loadSpendingForecast(dailyTotals);
                        loadBudgetInsight();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  SUMMARY CARDS
    // ═══════════════════════════════════════════════════════════

    private void updateCards() {
        float savings = totalIncome - totalExpense;
        tvIncome .setText("₹" + formatAmount(totalIncome));
        tvExpense.setText("₹" + formatAmount(totalExpense));
        tvSavings.setText("₹" + formatAmount(savings));
        tvSavings.setTextColor(savings >= 0
                ? Color.parseColor("#4ADE80")
                : Color.parseColor("#F87171"));
    }

    private String formatAmount(float value) {
        if (Math.abs(value) >= 100_000)
            return String.format(Locale.getDefault(), "%.1fL", value / 100_000);
        if (Math.abs(value) >= 1_000)
            return String.format(Locale.getDefault(), "%.1fK", value / 1_000);
        return String.valueOf((int) value);
    }

    // ═══════════════════════════════════════════════════════════
    //  PIE CHART
    // ═══════════════════════════════════════════════════════════

    private void loadPieChart(HashMap<String, Float> categoryMap) {

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> e : categoryMap.entrySet())
            entries.add(new PieEntry(e.getValue(), e.getKey()));

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setHoleColor(Color.parseColor("#111827"));
            pieChart.setCenterText("No expenses\nthis period");
            pieChart.setCenterTextColor(Color.parseColor("#9CA3AF"));
            pieChart.setCenterTextSize(14f);
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(
                Color.parseColor("#4ADE80"), Color.parseColor("#60A5FA"),
                Color.parseColor("#FBBF24"), Color.parseColor("#F87171"),
                Color.parseColor("#C084FC"), Color.parseColor("#22D3EE"),
                Color.parseColor("#FB923C"), Color.parseColor("#A78BFA")
        );

        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(12f);
        data.setValueFormatter(new LargeValueFormatter());

        pieChart.setData(data);
        pieChart.setCenterText("Expenses\n₹" + formatAmount(totalExpense));
        pieChart.setCenterTextColor(Color.WHITE);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setTextColor(Color.parseColor("#D1D5DB"));
        pieLegend.setTextSize(11f);
        pieLegend.setForm(Legend.LegendForm.CIRCLE);
        pieLegend.setFormSize(9f);
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setWordWrapEnabled(true);
        pieLegend.setDrawInside(false);

        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    // ═══════════════════════════════════════════════════════════
    //  LINE CHART
    // ═══════════════════════════════════════════════════════════

    private void loadLineChart(TreeMap<Integer, Float> dailyTotals) {

        if (dailyTotals == null || dailyTotals.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("No spending data for this period");
            lineChart.setNoDataTextColor(Color.parseColor("#9CA3AF"));
            lineChart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO)
                    .setTextSize(dpToPx(5));
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry>  entries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Integer, Float> entry : dailyTotals.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            xLabels.add("D" + entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spend");
        dataSet.setColor(Color.parseColor("#22D3EE"));
        dataSet.setCircleColor(Color.parseColor("#22D3EE"));
        dataSet.setCircleHoleColor(Color.parseColor("#111827"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#9CA3AF"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new LargeValueFormatter());
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#22D3EE"));
        dataSet.setFillAlpha(40);
        dataSet.setHighLightColor(Color.parseColor("#FBBF24"));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.enableDashedHighlightLine(10f, 5f, 0f);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(Math.min(xLabels.size(), 8), false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#9CA3AF"));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisLeft().setValueFormatter(new LargeValueFormatter());
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#1F2937"));
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);

        Legend lineLegend = lineChart.getLegend();
        lineLegend.setTextColor(Color.parseColor("#D1D5DB"));
        lineLegend.setTextSize(12f);
        lineLegend.setForm(Legend.LegendForm.CIRCLE);
        lineLegend.setFormSize(10f);
        lineLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        lineLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        lineLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        lineLegend.setDrawInside(false);

        lineChart.setExtraBottomOffset(16f);
        lineChart.setExtraLeftOffset(8f);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    // ═══════════════════════════════════════════════════════════
    //  BAR CHART
    // ═══════════════════════════════════════════════════════════

    private void loadBarChart(float income, float expense) {

        if (income == 0 && expense == 0) {
            barChart.clear();
            barChart.setNoDataText("No income or expense data");
            barChart.setNoDataTextColor(Color.parseColor("#9CA3AF"));
            barChart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO)
                    .setTextSize(dpToPx(5));
            barChart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, income));
        entries.add(new BarEntry(1f, expense));

        BarDataSet dataSet = new BarDataSet(entries, "Income vs Expense");
        dataSet.setColors(Color.parseColor("#4ADE80"), Color.parseColor("#F87171"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new LargeValueFormatter());

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        float maxVal = Math.max(income, expense);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(maxVal * 1.2f);
        barChart.getXAxis().setAxisMinimum(-0.5f);
        barChart.getXAxis().setAxisMaximum(1.5f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(new String[]{"Income", "Expense"}));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setLabelCount(2);

        Legend barLegend = barChart.getLegend();
        barLegend.setTextColor(Color.parseColor("#D1D5DB"));
        barLegend.setTextSize(12f);
        barLegend.setForm(Legend.LegendForm.CIRCLE);
        barLegend.setFormSize(10f);
        barLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        barLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        barLegend.setDrawInside(false);

        barChart.setData(data);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    // ═══════════════════════════════════════════════════════════
    //  HEATMAP
    // ═══════════════════════════════════════════════════════════

    private void loadHeatmap(HashMap<Integer, Float> dailyMap) {

        View root = getView();
        if (root == null) return;

        GridLayout grid = root.findViewById(R.id.heatmapGrid);
        grid.removeAllViews();

        float maxSpend = 1f;
        for (float v : dailyMap.values()) if (v > maxSpend) maxSpend = v;

        Calendar cal       = Calendar.getInstance();
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int startOffset    = (firstDayOfWeek == 1) ? 6 : firstDayOfWeek - 2;

        int cellSize   = dpToPx(34);
        int cellMargin = dpToPx(3);

        String[] dayHeaders = {"M", "T", "W", "T", "F", "S", "S"};
        for (String h : dayHeaders) {
            TextView header = new TextView(getContext());
            header.setText(h);
            header.setTextColor(Color.parseColor("#6B7280"));
            header.setTextSize(11f);
            header.setGravity(android.view.Gravity.CENTER);
            GridLayout.LayoutParams hp = new GridLayout.LayoutParams();
            hp.width  = cellSize;
            hp.height = cellSize;
            hp.setMargins(cellMargin, 0, cellMargin, dpToPx(4));
            header.setLayoutParams(hp);
            grid.addView(header);
        }

        for (int i = 0; i < startOffset; i++) {
            View empty = new View(getContext());
            GridLayout.LayoutParams ep = new GridLayout.LayoutParams();
            ep.width  = cellSize;
            ep.height = cellSize;
            ep.setMargins(cellMargin, cellMargin, cellMargin, cellMargin);
            empty.setLayoutParams(ep);
            grid.addView(empty);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            float spend = dailyMap.containsKey(day) ? dailyMap.get(day) : 0f;
            float ratio = spend / maxSpend;
            int   color = getHeatColor(ratio, spend);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(6));
            shape.setColor(color);

            TextView cell = new TextView(getContext());
            cell.setText(String.valueOf(day));
            cell.setTextColor(spend > 0
                    ? Color.parseColor("#FFFFFF")
                    : Color.parseColor("#4B5563"));
            cell.setTextSize(9f);
            cell.setGravity(android.view.Gravity.CENTER);
            cell.setBackground(shape);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = cellSize;
            params.height = cellSize;
            params.setMargins(cellMargin, cellMargin, cellMargin, cellMargin);
            cell.setLayoutParams(params);

            final int   d = day;
            final float s = spend;
            cell.setOnClickListener(v ->
                    android.widget.Toast.makeText(getContext(),
                            s > 0 ? "Day " + d + "  →  ₹" + (int) s
                                    : "Day " + d + "  →  No spending",
                            android.widget.Toast.LENGTH_SHORT).show()
            );

            grid.addView(cell);
        }
    }

    private int getHeatColor(float ratio, float spend) {
        if (spend == 0f)   return Color.parseColor("#1F2937");
        if (ratio < 0.25f) return Color.parseColor("#4ADE80");
        if (ratio < 0.60f) return Color.parseColor("#FACC15");
        if (ratio < 0.85f) return Color.parseColor("#FB923C");
        return                    Color.parseColor("#EF4444");
    }

    // ═══════════════════════════════════════════════════════════
    //  BUDGET INSIGHT + PROGRESS BARS
    // ═══════════════════════════════════════════════════════════

    private void loadBudgetInsight() {

        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        userRef.child("budgets").child("monthly").child(monthKey).child("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        float totalBudget = 0;
                        HashMap<String, Float> budgetMap = new HashMap<>();

                        for (DataSnapshot cat : snapshot.getChildren()) {
                            Double v = cat.getValue(Double.class);
                            if (v != null) {
                                totalBudget += v;
                                budgetMap.put(cat.getKey(), v.floatValue());
                            }
                        }

                        float  savings = totalIncome - totalExpense;
                        String emoji   = savings >= 0 ? "✅" : "⚠️";
                        String text    =
                                "📊 Monthly Budget: ₹" + formatAmount(totalBudget) + "\n" +
                                        "💰 Income:  ₹"        + formatAmount(totalIncome)  + "\n" +
                                        "💸 Expense: ₹"        + formatAmount(totalExpense) + "\n" +
                                        emoji + " Savings: ₹"  + formatAmount(savings);

                        if (txtInsight != null) txtInsight.setText(text);

                        loadCategorySpendForProgress(budgetMap, monthKey);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadCategorySpendForProgress(
            HashMap<String, Float> budgetMap, String monthKey) {

        String[] monthParts = monthKey.split("-");
        int year  = Integer.parseInt(monthParts[0]);
        int month = Integer.parseInt(monthParts[1]) - 1;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startMs = cal.getTimeInMillis();
        long endMs   = System.currentTimeMillis();

        HashMap<String, Float> categorySpend = new HashMap<>();

        userRef.child("expenses")
                .orderByChild("time")
                .startAt(startMs)
                .endAt(endMs)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String category  = data.child("category").getValue(String.class);
                            Double amountObj = data.child("amount").getValue(Double.class);
                            if (amountObj == null) continue;
                            if (category == null) category = "Other";
                            categorySpend.merge(category, amountObj.floatValue(), Float::sum);
                        }
                        renderBudgetProgressBars(budgetMap, categorySpend);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void renderBudgetProgressBars(
            HashMap<String, Float> budgetMap,
            HashMap<String, Float> categorySpend
    ) {
        if (budgetProgressContainer == null) return;
        budgetProgressContainer.removeAllViews();

        if (budgetMap.isEmpty()) {
            TextView hint = new TextView(getContext());
            hint.setText("No budgets set for this month.\nGo to Budget tab to set limits.");
            hint.setTextColor(Color.parseColor("#6B7280"));
            hint.setTextSize(13f);
            hint.setGravity(android.view.Gravity.CENTER);
            budgetProgressContainer.addView(hint);
            return;
        }

        for (Map.Entry<String, Float> entry : budgetMap.entrySet()) {

            String category = entry.getKey();
            float  budget   = entry.getValue();
            float  spent    = categorySpend.containsKey(category)
                    ? categorySpend.get(category) : 0f;
            float  percent  = (budget > 0) ? (spent / budget) * 100f : 0f;
            int    progress = (int) Math.min(percent, 100);

            int    barColor;
            String statusEmoji;
            if      (percent < 60f) { barColor = Color.parseColor("#4ADE80"); statusEmoji = "✅"; }
            else if (percent < 85f) { barColor = Color.parseColor("#FACC15"); statusEmoji = "⚠️"; }
            else                    { barColor = Color.parseColor("#F87171"); statusEmoji = "🔴"; }

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(14));
            row.setLayoutParams(rowParams);

            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvCategory = new TextView(getContext());
            tvCategory.setText(statusEmoji + " " + category);
            tvCategory.setTextColor(Color.parseColor("#E5E7EB"));
            tvCategory.setTextSize(13f);
            tvCategory.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvAmounts = new TextView(getContext());
            tvAmounts.setText("₹" + formatAmount(spent) + " / ₹" + formatAmount(budget));
            tvAmounts.setTextColor(Color.parseColor("#9CA3AF"));
            tvAmounts.setTextSize(12f);
            tvAmounts.setGravity(android.view.Gravity.END);

            topRow.addView(tvCategory);
            topRow.addView(tvAmounts);

            GradientDrawable track = new GradientDrawable();
            track.setShape(GradientDrawable.RECTANGLE);
            track.setCornerRadius(dpToPx(10));
            track.setColor(Color.parseColor("#1F2937"));

            GradientDrawable fill = new GradientDrawable();
            fill.setShape(GradientDrawable.RECTANGLE);
            fill.setCornerRadius(dpToPx(10));
            fill.setColor(barColor);

            android.widget.FrameLayout frameBar = new android.widget.FrameLayout(getContext());
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(10));
            frameParams.setMargins(0, dpToPx(6), 0, 0);
            frameBar.setLayoutParams(frameParams);

            View trackView = new View(getContext());
            trackView.setBackground(track);
            trackView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            View fillView = new View(getContext());
            fillView.setBackground(fill);
            android.widget.FrameLayout.LayoutParams fillParams =
                    new android.widget.FrameLayout.LayoutParams(
                            0, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
            fillView.setLayoutParams(fillParams);

            frameBar.addView(trackView);
            frameBar.addView(fillView);

            TextView tvPercent = new TextView(getContext());
            tvPercent.setText((int) percent + "% used");
            tvPercent.setTextColor(barColor);
            tvPercent.setTextSize(11f);
            tvPercent.setGravity(android.view.Gravity.END);
            LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            percentParams.setMargins(0, dpToPx(3), 0, 0);
            tvPercent.setLayoutParams(percentParams);

            row.addView(topRow);
            row.addView(frameBar);
            row.addView(tvPercent);
            budgetProgressContainer.addView(row);

            final int  targetProgress = progress;
            final View fv = fillView;
            frameBar.post(() -> {
                int totalWidth  = frameBar.getWidth();
                int targetWidth = (int) (totalWidth * (targetProgress / 100f));
                android.animation.ValueAnimator animator =
                        android.animation.ValueAnimator.ofInt(0, targetWidth);
                animator.setDuration(900);
                animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
                animator.addUpdateListener(anim -> {
                    android.widget.FrameLayout.LayoutParams lp =
                            (android.widget.FrameLayout.LayoutParams) fv.getLayoutParams();
                    lp.width = (int) anim.getAnimatedValue();
                    fv.setLayoutParams(lp);
                });
                animator.start();
            });
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SPENDING FORECAST
    // ═══════════════════════════════════════════════════════════

    private void loadSpendingForecast(TreeMap<Integer, Float> dailyTotals) {

        Calendar cal      = Calendar.getInstance();
        int today         = cal.get(Calendar.DAY_OF_MONTH);
        int daysInMonth   = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysRemaining = daysInMonth - today;

        ArrayList<Entry>  actualEntries  = new ArrayList<>();
        ArrayList<String> xLabels        = new ArrayList<>();

        float runningTotal = 0f;
        int   idx          = 0;

        for (int day = 1; day <= today; day++) {
            float spend = dailyTotals.containsKey(day) ? dailyTotals.get(day) : 0f;
            runningTotal += spend;
            actualEntries.add(new Entry(idx, runningTotal));
            xLabels.add("D" + day);
            idx++;
        }

        float dailyAvg = today > 0 ? totalExpense / today : 0f;

        ArrayList<Entry> forecastEntries = new ArrayList<>();
        if (!actualEntries.isEmpty())
            forecastEntries.add(new Entry(idx - 1, runningTotal));

        float projected = runningTotal;
        for (int day = today + 1; day <= daysInMonth; day++) {
            projected += dailyAvg;
            forecastEntries.add(new Entry(idx, projected));
            xLabels.add("D" + day);
            idx++;
        }

        float projectedTotal = runningTotal + (dailyAvg * daysRemaining);

        tvSpentSoFar.setText("₹" + formatAmount(totalExpense));
        tvProjected .setText("₹" + formatAmount(projectedTotal));
        tvDailyAvg  .setText("₹" + formatAmount(dailyAvg));
        tvForecastMessage.setText(buildForecastMessage(projectedTotal, dailyAvg, daysRemaining));

        LineDataSet actualSet = new LineDataSet(actualEntries, "Actual Spend");
        actualSet.setColor(Color.parseColor("#22D3EE"));
        actualSet.setCircleColor(Color.parseColor("#22D3EE"));
        actualSet.setCircleHoleColor(Color.parseColor("#111827"));
        actualSet.setLineWidth(2.5f);
        actualSet.setCircleRadius(4f);
        actualSet.setCircleHoleRadius(2f);
        actualSet.setDrawValues(false);
        actualSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        actualSet.setDrawFilled(true);
        actualSet.setFillColor(Color.parseColor("#22D3EE"));
        actualSet.setFillAlpha(30);

        LineDataSet forecastSet = new LineDataSet(forecastEntries, "Projected");
        forecastSet.setColor(Color.parseColor("#FBBF24"));
        forecastSet.setLineWidth(2f);
        forecastSet.enableDashedLine(12f, 6f, 0f);
        forecastSet.setDrawCircles(false);
        forecastSet.setDrawValues(false);
        forecastSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(actualSet, forecastSet);
        forecastChart.setData(lineData);

        XAxis xAxis = forecastChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelCount(Math.min(xLabels.size(), 10), false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        forecastChart.setExtraBottomOffset(16f);
        forecastChart.animateX(1000);
        forecastChart.invalidate();
    }

    private String buildForecastMessage(float projected, float dailyAvg, int daysLeft) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 ").append(daysLeft).append(" days left this month.\n");
        sb.append("📊 Daily average: ₹").append(formatAmount(dailyAvg)).append("\n");
        if (totalIncome > 0) {
            float remaining = totalIncome - projected;
            if (remaining > 0)
                sb.append("✅ At this pace you'll have ₹")
                        .append(formatAmount(remaining)).append(" left from income.");
            else
                sb.append("⚠️ At this pace you may exceed income by ₹")
                        .append(formatAmount(Math.abs(remaining)))
                        .append(". Consider cutting back.");
        } else {
            sb.append("💡 Projected month-end spend: ₹").append(formatAmount(projected));
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  GEMINI AI INSIGHTS
    // ═══════════════════════════════════════════════════════════

    private void callGeminiInsights() {

        btnGetInsights.setEnabled(false);
        btnGetInsights.setText("Analyzing...");
        aiLoadingLayout.setVisibility(View.VISIBLE);
        tvAiInsight.setText("");

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray  contents    = new JSONArray();
            JSONObject content     = new JSONObject();
            JSONArray  parts       = new JSONArray();
            JSONObject part        = new JSONObject();

            part.put("text", buildGeminiPrompt());
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 400);
            requestBody.put("generationConfig", genConfig);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_URL)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        resetAiButton();
                        tvAiInsight.setText("⚠️ Network error. Check your connection and try again.");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    String responseBody = response.body() != null
                            ? response.body().string() : "";
                    requireActivity().runOnUiThread(() -> {
                        resetAiButton();
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String text = json
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                            tvAiInsight.setText(text.trim());
                        } catch (Exception e) {
                            try {
                                // Try to show the actual API error
                                JSONObject errJson = new JSONObject(responseBody);
                                String errMsg = errJson
                                        .getJSONObject("error")
                                        .getString("message");
                                tvAiInsight.setText("⚠️ API Error: " + errMsg);
                            } catch (Exception e2) {
                                // Show raw response so you can debug
                                tvAiInsight.setText("⚠️ Raw response:\n" + responseBody);
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            resetAiButton();
            tvAiInsight.setText("⚠️ Error: " + e.getMessage());
        }
    }

    private void resetAiButton() {
        aiLoadingLayout.setVisibility(View.GONE);
        btnGetInsights.setEnabled(true);
        btnGetInsights.setText("✨ Refresh Insights");
    }

    private String buildGeminiPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal finance advisor. Analyze this user's spending data and give ");
        sb.append("3-4 short, specific, actionable insights. Be friendly and concise. Use emojis.\n\n");
        sb.append("SPENDING DATA:\n");
        sb.append("- Total Income this period: ₹").append(formatAmount(totalIncome)).append("\n");
        sb.append("- Total Expenses this period: ₹").append(formatAmount(totalExpense)).append("\n");
        sb.append("- Savings: ₹").append(formatAmount(totalIncome - totalExpense)).append("\n\n");
        sb.append("SPENDING BY CATEGORY:\n");
        for (Map.Entry<String, Float> entry : latestCategoryMap.entrySet()) {
            float pct = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100f : 0f;
            sb.append("- ").append(entry.getKey())
                    .append(": ₹").append(formatAmount(entry.getValue()))
                    .append(" (").append((int) pct).append("% of total)\n");
        }
        sb.append("\nKeep your response under 150 words. Focus on what stands out most.");
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  UTIL
    // ═══════════════════════════════════════════════════════════

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}