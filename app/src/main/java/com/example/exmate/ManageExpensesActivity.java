package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageExpensesActivity extends AppCompatActivity {

    RecyclerView categoryRecyclerView;
    CategoryAdapter categoryAdapter;
    List<String> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_expenses);

        Toolbar toolbar = findViewById(R.id.expenseToolbar);
        setSupportActionBar(toolbar);

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Categories (Admin controlled)
        categoryList = new ArrayList<>();
        categoryList.add("Food");
        categoryList.add("Travel");
        categoryList.add("Shopping");
        categoryList.add("Bills");
        categoryList.add("Medical");
        categoryList.add("Entertainment");

        categoryAdapter = new CategoryAdapter(this, categoryList);
        categoryRecyclerView.setAdapter(categoryAdapter);
    }
}
