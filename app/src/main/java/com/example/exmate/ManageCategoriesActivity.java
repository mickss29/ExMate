package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CategoryAdapter adapter;
    List<String> categoryList;
    Button btnAddCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        recyclerView = findViewById(R.id.recyclerCategories);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initial admin categories
        categoryList = new ArrayList<>();
        categoryList.add("Food");
        categoryList.add("Transport");
        categoryList.add("Shopping");
        categoryList.add("Bills");
        categoryList.add("Entertainment");
        categoryList.add("Health");
        categoryList.add("Education");

        adapter = new CategoryAdapter(this, categoryList);
        recyclerView.setAdapter(adapter);

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    // ================= ADD CATEGORY =================

    private void showAddCategoryDialog() {

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_category, null);

        EditText etCategoryName = view.findViewById(R.id.etCategoryName);

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {

                    String name = etCategoryName.getText()
                            .toString()
                            .trim();

                    if (name.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Category name required",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    categoryList.add(name);
                    adapter.notifyItemInserted(categoryList.size() - 1);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= DELETE CATEGORY =================

    public void deleteCategory(int position) {

        if (position < 0 || position >= categoryList.size()) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    categoryList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
