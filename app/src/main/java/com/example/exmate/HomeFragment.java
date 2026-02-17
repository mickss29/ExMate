package com.example.exmate;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // 🎙️ Dashboard Mic (NEW)
    private View fabVoiceDashboard;

    private static final int REQ_CODE_SPEECH_DASH = 3001;

    // ================= FIREBASE =================
    private DatabaseReference userRef;
    private DatabaseReference incomeRef;
    private DatabaseReference expenseRef;
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

        // 🎙️ Setup dashboard mic
        setupDashboardVoice();

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
        tvUserName = view.findViewById(R.id.tvUserName);
        btnViewAll = view.findViewById(R.id.btnViewAll);
        cardAddIncome = view.findViewById(R.id.cardAddIncome);
        cardAddExpense = view.findViewById(R.id.cardAddExpense);
        cardBudgetSummary = view.findViewById(R.id.cardBudgetSummary);

        // 🎙️ NEW
        fabVoiceDashboard = view.findViewById(R.id.fabVoiceDashboard);
    }

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        // 🎙️ direct save refs (NEW)
        incomeRef = userRef.child("incomes");
        expenseRef = userRef.child("expenses");
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

        animateBalance(tvCurrentBalance, lastBalance, newBalance);

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

        // 🎙️ little pop animation
        if (fabVoiceDashboard != null) {
            fabVoiceDashboard.startAnimation(anim);
        }
    }

    // =========================================================================================
    // 🎙️ DASHBOARD VOICE (NEW)
    // =========================================================================================

    private void setupDashboardVoice() {
        if (fabVoiceDashboard == null) return;

        fabVoiceDashboard.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startDashboardVoice();
        });
    }

    private void startDashboardVoice() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak income or expense");

            Toast.makeText(requireContext(), "Listening...", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, REQ_CODE_SPEECH_DASH);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Voice input not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_DASH &&
                resultCode == Activity.RESULT_OK &&
                data != null) {

            ArrayList<String> result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (result != null && !result.isEmpty()) {
                String rawText = result.get(0);

                handleDashboardVoice(rawText);
            }
        }
    }

    private void handleDashboardVoice(String rawText) {

        if (userId == null || userRef == null) return;

        String text = rawText.toLowerCase(Locale.ROOT);

        // Amount
        double amount = extractAmount(text);
        if (amount <= 0) {
            Toast.makeText(requireContext(),
                    "Couldn't detect amount 😅",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Decide Income vs Expense
        boolean isIncome = isIncomeText(text);

        long time = detectDateMillis(text);

        if (isIncome) {
            String source = detectIncomeSource(text);
            String pay = detectPaymentMode(text);
            String note = cleanNote(text);

            saveIncome(amount, time, source, pay, note);
        } else {
            String category = detectExpenseCategory(text);
            String pay = detectPaymentMode(text);
            String note = cleanNote(text);

            saveExpense(amount, time, category, pay, note);
        }
    }

    // ================= SAVE FROM DASHBOARD =================

    private void saveIncome(double amount, long time, String source, String paymentMode, String note) {

        if (incomeRef == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("time", time);
        map.put("source", source);
        map.put("paymentMode", paymentMode);
        map.put("note", note);

        incomeRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    NotificationHelper.showIncomeSummaryNotification(
                            requireContext(),
                            amount,
                            source
                    );
                    Toast.makeText(requireContext(),
                            "Income added: ₹" + moneyFormat.format(amount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void saveExpense(double amount, long time, String category, String paymentMode, String note) {

        if (expenseRef == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("time", time);
        map.put("category", category);
        map.put("paymentMode", paymentMode);
        map.put("note", note);

        expenseRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    NotificationHelper.showExpenseSummaryNotification(
                            requireContext(),
                            amount,
                            category
                    );
                    Toast.makeText(requireContext(),
                            "Expense added: ₹" + moneyFormat.format(amount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    // ================= SMART HELPERS =================

    private boolean isIncomeText(String text) {
        return text.contains("salary")
                || text.contains("freelance")
                || text.contains("income")
                || text.contains("business")
                || text.contains("rent")
                || text.contains("bonus")
                || text.contains("received")
                || text.contains("credit");
    }

    private double extractAmount(String text) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception ignored) {}
        return 0;
    }

    private long detectDateMillis(String text) {
        Calendar cal = Calendar.getInstance();

        if (text.contains("parso") || text.contains("day before yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -2);
        } else if (text.contains("kal") || text.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    private String detectPaymentMode(String text) {

        if (text.contains("upi") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm")) {
            return "UPI";
        }
        if (text.contains("cash")) return "Cash";
        if (text.contains("card") || text.contains("credit") || text.contains("debit")) return "Card";
        if (text.contains("bank") || text.contains("transfer")) return "Bank Transfer";
        if (text.contains("cheque")) return "Cheque";

        // default
        return "Cash";
    }

    private String detectIncomeSource(String text) {

        if (text.contains("salary")) return "Salary";
        if (text.contains("business")) return "Business";
        if (text.contains("freelance")) return "Freelance";
        if (text.contains("investment") || text.contains("stock") || text.contains("profit")) return "Investment";
        if (text.contains("rent") || text.contains("rental")) return "Rental Income";
        if (text.contains("bonus")) return "Bonus";
        if (text.contains("gift")) return "Gift";

        return "Other";
    }

    private String detectExpenseCategory(String text) {

        if (text.contains("uber") || text.contains("ola") || text.contains("metro") || text.contains("bus")
                || text.contains("train") || text.contains("petrol") || text.contains("fuel")) {
            return "Transport";
        }

        if (text.contains("dominos") || text.contains("pizza") || text.contains("cafe") || text.contains("coffee")
                || text.contains("restaurant") || text.contains("chai") || text.contains("tea")
                || text.contains("mcd") || text.contains("kfc")) {
            return "Food";
        }

        if (text.contains("amazon") || text.contains("flipkart") || text.contains("shopping")
                || text.contains("mall") || text.contains("store")) {
            return "Shopping";
        }

        if (text.contains("netflix") || text.contains("movie") || text.contains("pvr") || text.contains("cinema")) {
            return "Entertainment";
        }

        if (text.contains("doctor") || text.contains("hospital") || text.contains("medicine")
                || text.contains("clinic") || text.contains("pharmacy")) {
            return "Health";
        }

        if (text.contains("fees") || text.contains("college") || text.contains("school")
                || text.contains("course") || text.contains("gym")) {
            return "Education";
        }

        if (text.contains("bill") || text.contains("electricity") || text.contains("recharge")
                || text.contains("wifi") || text.contains("rent")) {
            return "Bills";
        }

        return "Other";
    }

    private String cleanNote(String text) {

        String clean = text;

        clean = clean.replace("today", "")
                .replace("aaj", "")
                .replace("yesterday", "")
                .replace("kal", "")
                .replace("parso", "")
                .replace("day before yesterday", "");

        clean = clean.replace("upi", "")
                .replace("gpay", "")
                .replace("phonepe", "")
                .replace("paytm", "")
                .replace("cash", "")
                .replace("card", "")
                .replace("debit", "")
                .replace("credit", "")
                .replace("bank transfer", "")
                .replace("transfer", "")
                .replace("bank", "")
                .replace("cheque", "");

        clean = clean.replace("rupees", "")
                .replace("rupaye", "")
                .replace("rs", "")
                .replace("₹", "");

        clean = clean.replaceAll("(\\d+(?:\\.\\d{1,2})?)", "");
        clean = clean.replaceAll("\\s+", " ").trim();

        if (clean.isEmpty()) return "Transaction";

        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }
}
