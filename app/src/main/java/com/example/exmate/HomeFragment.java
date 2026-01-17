package com.example.exmate;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    // ================= NAVIGATION =================
    public interface HomeNavigationListener {
        void openReports();
        void openBudget();
    }

    private HomeNavigationListener navigationListener;

    // ================= UI =================
    private RecyclerView rvRecent;
    private TextView tvIncome, tvExpense;
    private TextView tvCurrentBalance, tvTotalBalance;
    private TextView tvUserName, btnViewAll;
    private MaterialCardView cardAddIncome, cardAddExpense, cardBudgetSummary;

    // ================= FIREBASE =================
    private DatabaseReference userRef;
    private String userId;

    // ================= DATA =================
    private final List<TransactionModel> transactionList = new ArrayList<>();
    private RecentTransactionAdapter adapter;

    private double totalIncome = 0;
    private double totalExpense = 0;
    private double lastBalance = 0;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");

    // ================= SINGLE REALTIME LISTENER =================
    private final ValueEventListener dashboardListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {

            transactionList.clear();
            totalIncome = 0;
            totalExpense = 0;

            DataSnapshot incomeSnap = snapshot.child("incomes");
            DataSnapshot expenseSnap = snapshot.child("expenses");

            // INCOME
            for (DataSnapshot snap : incomeSnap.getChildren()) {
                Double amount = snap.child("amount").getValue(Double.class);
                Long time = snap.child("time").getValue(Long.class);
                String source = snap.child("source").getValue(String.class);

                if (amount == null || time == null) continue;

                totalIncome += amount;

                transactionList.add(new TransactionModel(
                        "Income",
                        amount,
                        time,
                        source != null ? source : "Income"
                ));
            }

            // EXPENSE
            for (DataSnapshot snap : expenseSnap.getChildren()) {
                Double amount = snap.child("amount").getValue(Double.class);
                Long time = snap.child("time").getValue(Long.class);
                String category = snap.child("category").getValue(String.class);

                if (amount == null || time == null) continue;

                totalExpense += amount;

                transactionList.add(new TransactionModel(
                        "Expense",
                        amount,
                        time,
                        category != null ? category : "Expense",
                        true
                ));
            }

            tvIncome.setText("Income  ₹" + moneyFormat.format(totalIncome));
            tvExpense.setText("Expenses  ₹" + moneyFormat.format(totalExpense));

            updateBalances();
            finalizeList();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {}
    };

    // =========================================================================================

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeNavigationListener) {
            navigationListener = (HomeNavigationListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecentList();
        setupFirebase();
        setupCardClicks();
        loadUserProfile();
        playEntryAnimation();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (userRef != null) {
            userRef.addValueEventListener(dashboardListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userRef != null) {
            userRef.removeEventListener(dashboardListener);
        }
    }

    // =========================================================================================

    private void initViews(View view) {
        rvRecent = view.findViewById(R.id.rvRecent);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvUserName = view.findViewById(R.id.tvUserName);
        btnViewAll = view.findViewById(R.id.btnViewAll);
        cardAddIncome = view.findViewById(R.id.cardAddIncome);
        cardAddExpense = view.findViewById(R.id.cardAddExpense);
        cardBudgetSummary = view.findViewById(R.id.cardBudgetSummary);
    }

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);
    }

    // =========================================================================================

    private void loadUserProfile() {
        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                if (TextUtils.isEmpty(name)) {
                    name = snapshot.child("username").getValue(String.class);
                }

                if (!TextUtils.isEmpty(name)) {
                    tvUserName.setText(capitalize(name));
                    fadeIn(tvUserName);
                } else {
                    tvUserName.setText("Welcome 👋");
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String capitalize(String text) {
        if (text.length() == 0) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void fadeIn(View view) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(400);
        view.startAnimation(anim);
    }

    // =========================================================================================

    private void updateBalances() {
        double newBalance = totalIncome - totalExpense;

        int color = newBalance >= 0
                ? Color.parseColor("#22C55E")
                : Color.parseColor("#F87171");

        tvCurrentBalance.setTextColor(color);
        tvTotalBalance.setTextColor(color);

        animateBalance(tvCurrentBalance, lastBalance, newBalance);
        animateBalance(tvTotalBalance, lastBalance, newBalance);

        lastBalance = newBalance;
    }

    private void animateBalance(TextView tv, double from, double to) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) to);
        animator.setDuration(600);
        animator.addUpdateListener(a ->
                tv.setText("₹ " + moneyFormat.format(a.getAnimatedValue()))
        );
        animator.start();
    }

    private void finalizeList() {
        Collections.sort(transactionList, (a, b) -> Long.compare(b.getTime(), a.getTime()));
        adapter.notifyDataSetChanged();
    }

    private void setupRecentList() {
        adapter = new RecentTransactionAdapter(transactionList);
        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecent.setAdapter(adapter);
    }

    // =========================================================================================

    private void setupCardClicks() {

        cardAddIncome.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddTransactionActivity.class);
            intent.putExtra("openTab", 1);
            startActivity(intent);
        });

        cardAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddTransactionActivity.class);
            intent.putExtra("openTab", 0);
            startActivity(intent);
        });

        btnViewAll.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.openReports();
            }
        });

        if (cardBudgetSummary != null) {
            cardBudgetSummary.setOnClickListener(v -> {
                if (navigationListener != null) {
                    navigationListener.openBudget();
                }
            });
        }
    }

    private void playEntryAnimation() {
        if (getContext() == null) return;

        Animation anim = AnimationUtils.loadAnimation(
                getContext(),
                R.anim.card_fade_slide
        );

        cardAddIncome.startAnimation(anim);
        cardAddExpense.startAnimation(anim);
        rvRecent.startAnimation(anim);
    }
}
