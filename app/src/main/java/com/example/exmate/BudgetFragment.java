package com.example.exmate;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment {

    // ===== UI =====
    private EditText etTotalBudget;
    private TextView tvRemaining;
    private CheckBox cbCarryForward;
    private Button btnSaveTotalBudget, btnSaveCategoryBudget;
    private Button btnMonthly, btnYearly;

    // ===== LIST =====
    private RecyclerView rvCategory;
    private BudgetCategoryAdapter adapter;
    private final List<BudgetCategoryModel> categories = new ArrayList<>();

    // ===== STATE =====
    private boolean isMonthly = true;
    private boolean isEdit = false;

    public BudgetFragment() {
        super(R.layout.fragment_budget_add);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        // ===== Bind Views =====
        etTotalBudget = view.findViewById(R.id.etTotalBudget);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        cbCarryForward = view.findViewById(R.id.cbCarryForward);

        btnSaveTotalBudget = view.findViewById(R.id.btnSaveTotalBudget);
        btnSaveCategoryBudget = view.findViewById(R.id.btnSaveCategoryBudget);
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnYearly = view.findViewById(R.id.btnYearly);

        rvCategory = view.findViewById(R.id.rvCategoryBudget);
        rvCategory.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        buildDefaultCategories();
        adapter = new BudgetCategoryAdapter(categories, this::updateRemaining);
        rvCategory.setAdapter(adapter);

        // ===== Arguments (EDIT MODE) =====
        if (getArguments() != null) {
            isEdit = getArguments().getBoolean("isEdit", false);
        }

        if (isEdit) {
            loadExistingBudget();
        }

        // ===== Toggle =====
        btnMonthly.setOnClickListener(v -> isMonthly = true);
        btnYearly.setOnClickListener(v -> isMonthly = false);

        // ===== Save =====
        btnSaveTotalBudget.setOnClickListener(v -> saveTotalBudget());
        btnSaveCategoryBudget.setOnClickListener(v -> saveCategoryBudget());
    }

    // ================= Remaining =================

    private void updateRemaining() {

        if (TextUtils.isEmpty(etTotalBudget.getText())) {
            tvRemaining.setText("Remaining: ₹0");
            return;
        }

        int total = Integer.parseInt(
                etTotalBudget.getText().toString()
        );

        int used = 0;
        for (BudgetCategoryModel m : categories) {
            used += m.getAmount();
        }

        tvRemaining.setText(
                "Remaining: ₹" + (total - used)
        );
    }

    // ================= Load Existing Budget =================

    private void loadExistingBudget() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly") // 🔥 EDIT always monthly
                        .child(getMonthKey());

        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) return;

                        Integer total =
                                snapshot.child("totalBudget")
                                        .getValue(Integer.class);

                        if (total != null) {
                            etTotalBudget.setText(
                                    String.valueOf(total)
                            );
                        }

                        DataSnapshot catSnap =
                                snapshot.child("categories");

                        for (BudgetCategoryModel m : categories) {
                            Integer val =
                                    catSnap.child(m.getName())
                                            .getValue(Integer.class);
                            if (val != null) {
                                m.setAmount(val);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateRemaining();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                });
    }

    // ================= Save Total =================

    private void saveTotalBudget() {

        if (TextUtils.isEmpty(etTotalBudget.getText())) {
            etTotalBudget.setError("Enter amount");
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        int total =
                Integer.parseInt(
                        etTotalBudget.getText().toString()
                );

        boolean carryForward =
                cbCarryForward.isChecked();

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getMonthKey());

        Map<String, Object> data = new HashMap<>();
        data.put("totalBudget", total);
        data.put("carryForward", carryForward);

        ref.updateChildren(data)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Budget saved",
                            Toast.LENGTH_SHORT
                    ).show();

                    openAnalysis();
                });
    }

    // ================= Save Categories =================

    private void saveCategoryBudget() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Integer> map = new HashMap<>();
        for (BudgetCategoryModel m : categories) {
            map.put(m.getName(), m.getAmount());
        }

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("budgets")
                        .child("monthly")
                        .child(getMonthKey())
                        .child("categories");

        ref.setValue(map)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Category budget saved",
                            Toast.LENGTH_SHORT
                    ).show();

                    openAnalysis();
                });
    }

    // ================= Redirect =================

    private void openAnalysis() {

        if (!isAdded()) return;

        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        new BudgetAnalysisFragment()
                )
                .commit();
    }

    // ================= Helpers =================

    private void buildDefaultCategories() {
        categories.clear();
        categories.add(new BudgetCategoryModel("Food", 0));
        categories.add(new BudgetCategoryModel("Transport", 0));
        categories.add(new BudgetCategoryModel("Education", 0));
        categories.add(new BudgetCategoryModel("Shopping", 0));
        categories.add(new BudgetCategoryModel("Health", 0));
        categories.add(new BudgetCategoryModel("Others", 0));
    }

    private String getMonthKey() {
        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(new Date());
    }
}
