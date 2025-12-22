package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserStatisticsActivity extends AppCompatActivity {

    private RecyclerView rvCategoryStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_statistics);

        initViews();
        setupCategoryBreakdown();
    }

    // ---------------- INIT ----------------
    private void initViews() {
       // rvCategoryStats = findViewById(R.id.rvCategoryStats);

        rvCategoryStats.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );
    }

    // ---------------- UI ONLY (DUMMY DATA) ----------------
    private void setupCategoryBreakdown() {

        // Dummy categories (NO LOGIC, NO FIREBASE)
        List<String> categories = new ArrayList<>();
        categories.add("Food");
        categories.add("Transport");
        categories.add("Shopping");
        categories.add("Bills");
        categories.add("Entertainment");

        DummyAdapter adapter = new DummyAdapter(categories);
        rvCategoryStats.setAdapter(adapter);
    }
}
