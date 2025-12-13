package com.example.exmate;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ❌ NO try-catch here
        setContentView(R.layout.activity_main);

        // 🔹 Test UI load (this MUST appear)
        Toast.makeText(this, "UI LOADED", Toast.LENGTH_SHORT).show();

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            Toast.makeText(this, item.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
            return true;
        });

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DummyAdapter());

        // Buttons
        LinearLayout btnIncome = findViewById(R.id.btnAddIncome);
        LinearLayout btnExpense = findViewById(R.id.btnAddExpense);

        btnIncome.setOnClickListener(v ->
                Toast.makeText(this, "Add Income", Toast.LENGTH_SHORT).show());

        btnExpense.setOnClickListener(v ->
                Toast.makeText(this, "Add Expense", Toast.LENGTH_SHORT).show());
    }
}
