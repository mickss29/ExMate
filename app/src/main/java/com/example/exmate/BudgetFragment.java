package com.example.exmate;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment {

    private TextView tvTotal;
    private List<BudgetCategoryModel> categories = new ArrayList<>();

    public BudgetFragment() {
        super(R.layout.fragment_budget_add);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        tvTotal = view.findViewById(R.id.tvTotalBudget);

        RecyclerView rv = view.findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Default categories
        String[] defaults = {
                "Education","Food","Entertainment",
                "Health","Shopping","Transport","Rent","Others"
        };

        for (String c : defaults)
            categories.add(new BudgetCategoryModel(c));

        BudgetCategoryAdapter adapter =
                new BudgetCategoryAdapter(categories, this::updateTotal);

        rv.setAdapter(adapter);

        view.findViewById(R.id.btnAddCategory)
                .setOnClickListener(v -> {
                    categories.add(new BudgetCategoryModel("Custom"));
                    adapter.notifyItemInserted(categories.size() - 1);
                });

        view.findViewById(R.id.btnSaveBudget)
                .setOnClickListener(v -> saveBudget());
    }

    private void updateTotal() {
        int sum = 0;

        for (BudgetCategoryModel m : categories) {
            sum += m.getAmount(); // ✅ FIXED
        }

        tvTotal.setText("Total Budget: ₹" + sum);
    }

    private void saveBudget() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String key = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault())
                .format(new Date());

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(key);

        Map<String, Object> data = new HashMap<>();
        Map<String, Integer> map = new HashMap<>();

        int total = 0;

        for (BudgetCategoryModel m : categories) {

            if (m.getAmount() > 0) { // ✅ FIXED
                map.put(m.getName(), m.getAmount());
                total += m.getAmount();
            }
        }


        data.put("totalBudget", total);
        data.put("categories", map);

        ref.setValue(data).addOnSuccessListener(a ->
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                new BudgetAnalysisFragment())
                        .commit());
    }
}
