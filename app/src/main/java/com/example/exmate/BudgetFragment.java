package com.example.exmate;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment {

    // ===== UI =====
    private EditText etTotalBudget;
    private TextView tvTotal;
    private Button btnUpdateBudget;
    private RecyclerView recyclerView;

    // ===== DATA =====
    private BudgetCategoryAdapter adapter;
    private final List<BudgetCategoryModel> categories = new ArrayList<>();
    private boolean isEdit = false;

    public BudgetFragment() {
        super(R.layout.fragment_budget_add);
    }

    // ===== MASTER CATEGORY LIST =====
    private List<String> getAllCategories() {
        return Arrays.asList(
                "Food",
                "Education",
                "Entertainment",
                "Health",
                "Shopping",
                "Transport",
                "Rent",
                "Others"
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        // Bind views
        etTotalBudget = view.findViewById(R.id.etTotalBudget);
        tvTotal = view.findViewById(R.id.tvTotalBudget);
        btnUpdateBudget = view.findViewById(R.id.btnUpdateBudget);

        recyclerView = view.findViewById(R.id.recyclerBudgetCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new BudgetCategoryAdapter(categories, this::updateTotal);
        recyclerView.setAdapter(adapter);

        // Always build all categories first
        buildDefaultCategories();

        // Check edit mode
        if (getArguments() != null) {
            isEdit = getArguments().getBoolean("isEdit", false);
        }

        if (isEdit) {
            loadExistingBudget();
        }

        btnUpdateBudget.setOnClickListener(v -> saveOrUpdateBudget());
    }

    // ===== TOTAL CALC =====
    private void updateTotal() {
        int sum = 0;
        for (BudgetCategoryModel m : categories) {
            sum += m.getAmount();
        }
        tvTotal.setText("Total Budget: ₹" + sum);
    }

    // ===== SAVE / UPDATE =====
    private void saveOrUpdateBudget() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (TextUtils.isEmpty(etTotalBudget.getText().toString())) {
            etTotalBudget.setError("Enter total budget");
            return;
        }

        int totalBudget =
                Integer.parseInt(etTotalBudget.getText().toString().trim());

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getCurrentMonthKey());

        Map<String, Object> data = new HashMap<>();
        Map<String, Integer> catMap = new HashMap<>();

        // ✅ SAVE ALL CATEGORIES (IMPORTANT FIX)
        for (BudgetCategoryModel m : categories) {
            catMap.put(m.getName(), m.getAmount());
        }

        data.put("totalBudget", totalBudget);
        data.put("categories", catMap);

        ref.setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            requireContext(),
                            isEdit ? "Budget updated" : "Budget saved",
                            Toast.LENGTH_SHORT
                    ).show();

                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack();
                });
    }

    // ===== LOAD EXISTING (EDIT MODE) =====
    private void loadExistingBudget() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getCurrentMonthKey());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                Integer total =
                        snapshot.child("totalBudget")
                                .getValue(Integer.class);

                if (total != null) {
                    etTotalBudget.setText(String.valueOf(total));
                }

                DataSnapshot catSnap = snapshot.child("categories");

                for (BudgetCategoryModel m : categories) {
                    Integer val =
                            catSnap.child(m.getName())
                                    .getValue(Integer.class);
                    if (val != null) {
                        m.setAmount(val);
                    }
                }

                adapter.notifyDataSetChanged();
                updateTotal();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ===== HELPERS =====
    private void buildDefaultCategories() {
        categories.clear();
        for (String name : getAllCategories()) {
            categories.add(new BudgetCategoryModel(name, 0));
        }
    }

    private String getCurrentMonthKey() {
        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(new Date());
    }
}
