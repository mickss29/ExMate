package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    // ================= UI =================
    private RecyclerView rvRecent;
    private TextView tvIncome, tvExpense;
    private MaterialCardView cardAddIncome, cardAddExpense;

    // ================= FIREBASE =================
    private DatabaseReference userRef;
    private String userId;

    // ================= DATA =================
    private final List<TransactionModel> transactionList = new ArrayList<>();
    private RecentTransactionAdapter adapter;

    // ================= LIFECYCLE =================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecentList();
        setupCardClicks();
        setupFirebase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    // ================= INIT =================

    private void initViews(View view) {
        rvRecent = view.findViewById(R.id.rvRecent);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        cardAddIncome = view.findViewById(R.id.cardAddIncome);
        cardAddExpense = view.findViewById(R.id.cardAddExpense);
    }

    // ================= FIREBASE =================

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            // 🔒 SAFETY: fragment opened before login
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);
    }

    // ================= LOAD DATA =================

    private void loadDashboardData() {
        if (adapter == null || userRef == null) return;

        transactionList.clear();
        adapter.notifyDataSetChanged();

        loadIncome();
        loadExpense();
    }

    private void loadIncome() {
        userRef.child("incomes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        double totalIncome = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Double amount = snap.child("amount").getValue(Double.class);
                            Long time = snap.child("time").getValue(Long.class);

                            if (amount == null || time == null) continue;

                            totalIncome += amount;
                            transactionList.add(
                                    new TransactionModel("Income", amount, time)
                            );
                        }

                        tvIncome.setText("Income  ₹" + totalIncome);
                        finalizeList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadExpense() {
        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        double totalExpense = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Double amount = snap.child("amount").getValue(Double.class);
                            Long time = snap.child("time").getValue(Long.class);

                            if (amount == null || time == null) continue;

                            totalExpense += amount;
                            transactionList.add(
                                    new TransactionModel("Expense", amount, time)
                            );
                        }

                        tvExpense.setText("Expenses  ₹" + totalExpense);
                        finalizeList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void finalizeList() {
        Collections.sort(
                transactionList,
                (a, b) -> Long.compare(b.getTime(), a.getTime())
        );
        adapter.notifyDataSetChanged();
    }

    // ================= RECENT LIST =================

    private void setupRecentList() {
        adapter = new RecentTransactionAdapter(transactionList);
        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecent.setAdapter(adapter);
    }

    // ================= CARD CLICKS =================

    private void setupCardClicks() {

        cardAddIncome.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddIncomeActivity.class))
        );

        cardAddExpense.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddExpenseActivity.class))
        );
    }

    // ================= OPTIONAL BOTTOM SHEET =================

    private void showAddTransactionSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_add_button, null);

        dialog.setContentView(view);

        view.findViewById(R.id.optionAddIncome)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(getContext(), AddIncomeActivity.class));
                });

        view.findViewById(R.id.optionAddExpense)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(getContext(), AddExpenseActivity.class));
                });

        dialog.show();
    }
}
