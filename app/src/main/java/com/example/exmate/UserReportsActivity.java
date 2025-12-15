package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class UserReportsActivity extends AppCompatActivity {

    // UI
    private TextView tvIncome, tvExpense, tvSavings, tvEmpty;
    private MaterialButton btnDaily, btnWeekly, btnMonthly;
    private RecyclerView rvCategory;

    // Firebase
    private DatabaseReference userRef;

    // Data
    private CategoryReportAdapter adapter;
    private final List<CategoryReportModel> categoryList = new ArrayList<>();

    private double currentIncome = 0;
    private double currentExpense = 0;

    private static final int DAILY = 1;
    private static final int WEEKLY = 2;
    private static final int MONTHLY = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reports);

        initViews();
        setupRecycler();
        setupFirebase();
        setupButtons();

        loadReport(DAILY); // default
    }

    // ================= INIT =================

    private void initViews() {

        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvSavings = findViewById(R.id.tvSavings); // ⭐ FIXED
        tvEmpty = findViewById(R.id.tvEmpty);

        btnDaily = findViewById(R.id.btnDaily);
        btnWeekly = findViewById(R.id.btnWeekly);
        btnMonthly = findViewById(R.id.btnMonthly);

        rvCategory = findViewById(R.id.rvCategory);

        // DEFAULT VALUES (CRASH FIX)
        if (tvIncome != null) tvIncome.setText("₹0");
        if (tvExpense != null) tvExpense.setText("₹0");
        if (tvSavings != null) tvSavings.setText("₹0");
    }

    private void setupRecycler() {
        adapter = new CategoryReportAdapter(categoryList);
        rvCategory.setLayoutManager(new LinearLayoutManager(this));
        rvCategory.setAdapter(adapter);
    }

    private void setupFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);
    }

    private void setupButtons() {
        btnDaily.setOnClickListener(v -> loadReport(DAILY));
        btnWeekly.setOnClickListener(v -> loadReport(WEEKLY));
        btnMonthly.setOnClickListener(v -> loadReport(MONTHLY));
    }

    // ================= CORE =================

    private void loadReport(int type) {
        long start = getStartTime(type);
        long end = System.currentTimeMillis();

        loadIncome(start, end);
        loadExpense(start, end);
    }

    private void loadIncome(long start, long end) {

        userRef.child("incomes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        double total = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Long time = snap.child("time").getValue(Long.class);
                            Double amount = snap.child("amount").getValue(Double.class);

                            if (time == null || amount == null) continue;
                            if (time < start || time > end) continue;

                            total += amount;
                        }

                        currentIncome = total;
                        if (tvIncome != null) tvIncome.setText(format(total));
                        updateSavings();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadExpense(long start, long end) {

        categoryList.clear();

        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        double total = 0;
                        Map<String, Double> map = new HashMap<>();

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            Long time = snap.child("time").getValue(Long.class);
                            Double amount = snap.child("amount").getValue(Double.class);
                            String category = snap.child("category").getValue(String.class);

                            if (time == null || amount == null) continue;
                            if (time < start || time > end) continue;

                            total += amount;

                            if (category == null) category = "Other";
                            map.put(category, map.getOrDefault(category, 0.0) + amount);
                        }

                        currentExpense = total;
                        if (tvExpense != null) tvExpense.setText(format(total));

                        for (String key : map.keySet()) {
                            categoryList.add(new CategoryReportModel(key, map.get(key)));
                        }

                        adapter.notifyDataSetChanged();
                        updateSavings();
                        updateEmpty();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void updateSavings() {
        if (tvSavings == null) return;

        double savings = currentIncome - currentExpense;
        tvSavings.setText(format(savings));
    }

    // ================= UTIL =================

    private long getStartTime(int type) {
        Calendar cal = Calendar.getInstance();

        if (type == DAILY) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else if (type == WEEKLY) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        } else if (type == MONTHLY) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }

        return cal.getTimeInMillis();
    }

    private String format(double value) {
        return "₹" + String.format(Locale.getDefault(), "%,.0f", value);
    }

    private void updateEmpty() {
        if (categoryList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvCategory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvCategory.setVisibility(View.VISIBLE);
        }
    }
}
