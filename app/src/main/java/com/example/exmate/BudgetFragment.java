package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BudgetFragment extends Fragment {

    // ─── Views ───────────────────────────────────────────────
    private TextView tvTotalBudget, tvSpent, tvAvailable, tvUnallocated;
    private TextView tvMonthTitle, tvEmptyBudget;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private ImageView btnPrevMonth, btnNextMonth;
    private View layoutChart;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton fabAddCategory;

    // ─── Data ────────────────────────────────────────────────
    private final List<BudgetCategoryModel> categoryList = new ArrayList<>();
    private BudgetCategoryAdapter adapter;
    private int totalBudget = 0;
    private int totalSpent  = 0;

    // ─── Firebase ────────────────────────────────────────────
    private DatabaseReference userRef;
    private String monthKey;
    private final Calendar calendar = Calendar.getInstance();

    // ─── Category emojis ─────────────────────────────────────
    private static final Map<String, String> EMOJI_MAP = new HashMap<String, String>() {{
        put("Food",        "🍔"); put("Transport",  "🚗");
        put("Health",      "💊"); put("Education",  "📚");
        put("Shopping",    "🛍️"); put("Travel",      "✈️");
        put("Bills",       "💡"); put("Other",       "📦");
        put("Entertainment","🎬"); put("Gym",        "💪");
        put("Rent",        "🏠"); put("Savings",    "💰");
    }};

    public BudgetFragment() {
        super(R.layout.fragment_budget);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // ── Find views ──
        tvMonthTitle    = view.findViewById(R.id.tvMonthTitle);
        tvTotalBudget   = view.findViewById(R.id.tvTotalBudget);
        tvSpent         = view.findViewById(R.id.tvSpent);
        tvAvailable     = view.findViewById(R.id.tvAvailable);
        tvUnallocated   = view.findViewById(R.id.tvUnallocated);
        tvEmptyBudget   = view.findViewById(R.id.tvEmptyBudget);
        pieChart        = view.findViewById(R.id.pieChart);
        layoutChart     = view.findViewById(R.id.layoutChart);
        layoutEmptyState= view.findViewById(R.id.layoutEmptyState);
        btnPrevMonth    = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth    = view.findViewById(R.id.btnNextMonth);
        fabAddCategory  = view.findViewById(R.id.fabAddCategory);
        recyclerView    = view.findViewById(R.id.rvCategories);

        // ── RecyclerView ──
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetCategoryAdapter(categoryList, this::openEditDialog);
        recyclerView.setAdapter(adapter);

        // ── Month nav ──
        tvMonthTitle.setText(getMonthTitle());
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonth();
        });
        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonth();
        });

        // ── FAB ──
        fabAddCategory.setOnClickListener(v -> openAddCategoryDialog());

        // ── Firebase ──
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRef  = FirebaseDatabase.getInstance().getReference("users").child(uid);
        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());

        setupPieChartAppearance();
        loadBudgets();
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD BUDGET
    // ═══════════════════════════════════════════════════════════

    private void loadBudgets() {
        userRef.child("budgets").child("monthly").child(monthKey)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        categoryList.clear();
                        totalBudget = 0;

                        DataSnapshot catSnap = snapshot.child("categories");

                        if (!catSnap.exists() || catSnap.getChildrenCount() == 0) {
                            showEmptyState();
                            createDefaultCategories();
                            return;
                        }

                        hideEmptyState();

                        for (DataSnapshot ds : catSnap.getChildren()) {
                            String  name = ds.getKey();
                            Integer amt  = ds.getValue(Integer.class);
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

    // ═══════════════════════════════════════════════════════════
    //  LOAD EXPENSES
    // ═══════════════════════════════════════════════════════════

    private void loadExpenses() {
        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        totalSpent = 0;
                        for (BudgetCategoryModel m : categoryList) m.setSpent(0);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String  category = ds.child("category").getValue(String.class);
                            Integer amount   = ds.child("amount").getValue(Integer.class);
                            Long    time     = ds.child("time").getValue(Long.class);

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

    // ═══════════════════════════════════════════════════════════
    //  SUMMARY
    // ═══════════════════════════════════════════════════════════

    private void updateSummary() {
        if (!isAdded()) return;

        int available  = Math.max(totalBudget - totalSpent, 0);
        int allocated  = 0;
        for (BudgetCategoryModel m : categoryList) allocated += m.getBudget();
        int unallocated = Math.max(totalBudget - allocated, 0);

        tvTotalBudget.setText("₹" + formatAmount(totalBudget));
        tvSpent      .setText("₹" + formatAmount(totalSpent));
        tvAvailable  .setText("₹" + formatAmount(available));
        tvUnallocated.setText("₹" + formatAmount(unallocated) + " unallocated");

        updatePieChart(totalBudget, totalSpent);
    }

    // ═══════════════════════════════════════════════════════════
    //  PIE CHART
    // ═══════════════════════════════════════════════════════════

    private void setupPieChartAppearance() {
        pieChart.setBackgroundColor(Color.parseColor("#111827"));
        pieChart.setHoleColor(Color.parseColor("#111827"));
        pieChart.setTransparentCircleColor(Color.parseColor("#111827"));
        pieChart.setHoleRadius(72f);
        pieChart.setTransparentCircleRadius(76f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);
        pieChart.setTouchEnabled(false);
    }

    private void updatePieChart(int totalBudget, int totalSpent) {
        if (pieChart == null || totalBudget == 0) return;

        int   remaining  = Math.max(totalBudget - totalSpent, 0);
        float percentLeft = (remaining * 100f) / totalBudget;

        // Color based on % remaining
        int accentColor =
                percentLeft > 40 ? Color.parseColor("#4ADE80") :
                        percentLeft > 15 ? Color.parseColor("#FBBF24") :
                                Color.parseColor("#F87171");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(remaining, "Left"));
        entries.add(new PieEntry(Math.max(totalSpent, 0), "Spent"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(accentColor, Color.parseColor("#1F2937"));
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        pieChart.setData(new PieData(dataSet));
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(Math.round(percentLeft) + "%\nLeft");
        pieChart.setCenterTextSize(20f);
        pieChart.setCenterTextColor(accentColor);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    // ═══════════════════════════════════════════════════════════
    //  DIALOGS
    // ═══════════════════════════════════════════════════════════

    private void openEditDialog(BudgetCategoryModel model) {

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_budget, null);

        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        if (model.getBudget() > 0)
            etAmount.setText(String.valueOf(model.getBudget()));

        new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                .setTitle(getEmoji(model.getName()) + "  " + model.getName())
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String input = etAmount.getText().toString().trim();
                    if (input.isEmpty()) return;
                    saveCategoryBudget(model.getName(), Integer.parseInt(input));
                })
                .setNeutralButton("Remove", (d, w) ->
                        removeCategoryBudget(model.getName()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAddCategoryDialog() {

        // Built-in suggestions
        String[] suggestions = {
                "Food","Transport","Health","Education",
                "Shopping","Travel","Bills","Entertainment",
                "Gym","Rent","Savings","Other"
        };

        // Filter out already added
        List<String> available = new ArrayList<>();
        List<String> existingNames = new ArrayList<>();
        for (BudgetCategoryModel m : categoryList) existingNames.add(m.getName());
        for (String s : suggestions) {
            if (!existingNames.contains(s)) available.add(s);
        }
        available.add("+ Custom");

        String[] options = available.toArray(new String[0]);

        new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                .setTitle("Add Category")
                .setItems(options, (d, which) -> {
                    String selected = options[which];
                    if (selected.equals("+ Custom")) {
                        openCustomCategoryDialog();
                    } else {
                        openSetAmountDialog(selected);
                    }
                })
                .show();
    }

    private void openCustomCategoryDialog() {
        EditText etName = new EditText(getContext());
        etName.setHint("Category name");
        etName.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                .setTitle("Custom Category")
                .setView(etName)
                .setPositiveButton("Next", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) openSetAmountDialog(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSetAmountDialog(String categoryName) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_budget, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);

        new AlertDialog.Builder(getContext(), R.style.DarkAlertDialog)
                .setTitle(getEmoji(categoryName) + "  Set budget for " + categoryName)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String input = etAmount.getText().toString().trim();
                    if (input.isEmpty()) return;
                    saveCategoryBudget(categoryName, Integer.parseInt(input));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════
    //  FIREBASE WRITE HELPERS
    // ═══════════════════════════════════════════════════════════

    private void saveCategoryBudget(String name, int amount) {
        userRef.child("budgets").child("monthly").child(monthKey)
                .child("categories").child(name).setValue(amount);
    }

    private void removeCategoryBudget(String name) {
        userRef.child("budgets").child("monthly").child(monthKey)
                .child("categories").child(name).removeValue();
    }

    private void createDefaultCategories() {
        String[] defaults = {"Food", "Transport", "Health", "Education", "Other"};
        DatabaseReference catRef = userRef.child("budgets").child("monthly")
                .child(monthKey).child("categories");
        for (String c : defaults) catRef.child(c).setValue(0);
    }

    // ═══════════════════════════════════════════════════════════
    //  EMPTY STATE
    // ═══════════════════════════════════════════════════════════

    private void showEmptyState() {
        layoutChart.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        layoutChart.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    // ═══════════════════════════════════════════════════════════
    //  MONTH NAV
    // ═══════════════════════════════════════════════════════════

    private void updateMonth() {
        tvMonthTitle.setText(getMonthTitle());
        monthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(calendar.getTime());
        loadBudgets();
    }

    private String getMonthTitle() {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(calendar.getTime());
    }

    // ═══════════════════════════════════════════════════════════
    //  UTIL
    // ═══════════════════════════════════════════════════════════

    public static String getEmoji(String categoryName) {
        for (Map.Entry<String, String> e : EMOJI_MAP.entrySet()) {
            if (categoryName.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return "📦";
    }

    private String formatAmount(int value) {
        if (value >= 100_000) return String.format(Locale.getDefault(), "%.1fL", value / 100_000f);
        if (value >= 1_000)   return String.format(Locale.getDefault(), "%.1fK", value / 1_000f);
        return String.valueOf(value);
    }
}