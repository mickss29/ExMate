package com.example.exmate;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class HomeFragment extends Fragment {

    // =====================================================================
    // NAVIGATION
    // =====================================================================
    public interface HomeNavigationListener {
        void openReports();
        void openBudget();
    }

    private HomeNavigationListener navigationListener;

    // =====================================================================
    // UI VIEWS
    // =====================================================================
    private RecyclerView rvRecent;
    private TextView tvIncome, tvExpense;
    private TextView tvCurrentBalance;
    private TextView tvUserName, btnViewAll;
    private MaterialCardView cardAddIncome, cardAddExpense, cardBudgetSummary;
    private View fabVoiceDashboard;
    private RecyclerView rvDiscover;

    // =====================================================================
    // VOICE DIALOG VIEWS
    // =====================================================================
    private Dialog voiceDialog;
    private View pulse1, pulse2, pulse3, micCard;
    private TextView tvDots;

    // =====================================================================
    // VOICE DIALOG ANIMATIONS
    // =====================================================================
    private Animation pulseAnim1, pulseAnim2, pulseAnim3; // BUG FIX: pulseAnim3 is now a proper field
    private Animation micBounceAnim, dotsAnim;

    // =====================================================================
    // PERMISSION
    // =====================================================================
    private static final int REQ_AUDIO_PERMISSION = 9001;

    // =====================================================================
    // FIREBASE
    // =====================================================================
    private DatabaseReference userRef;
    private DatabaseReference incomeRef;
    private DatabaseReference expenseRef;
    private String userId;

    // =====================================================================
    // DATA
    // =====================================================================
    private final List<TransactionModel> transactionList = new ArrayList<>();
    private RecentTransactionAdapter adapter;

    private double totalIncome  = 0;
    private double totalExpense = 0;
    private double lastBalance  = 0;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");

    // =====================================================================
    // VOICE (SPEECH RECOGNIZER)
    // =====================================================================
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    // =====================================================================
    // SINGLE REALTIME LISTENER
    // =====================================================================
    private final ValueEventListener dashboardListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {

            transactionList.clear();
            totalIncome  = 0;
            totalExpense = 0;

            // ── Income ──
            for (DataSnapshot snap : snapshot.child("incomes").getChildren()) {
                Double amount = snap.child("amount").getValue(Double.class);
                Long   time   = snap.child("time").getValue(Long.class);
                String source = snap.child("source").getValue(String.class);

                if (amount == null || time == null) continue;

                totalIncome += amount;
                transactionList.add(new TransactionModel(
                        "Income", amount, time,
                        source != null ? source : "Income"
                ));
            }

            // ── Expense ──
            for (DataSnapshot snap : snapshot.child("expenses").getChildren()) {
                Double amount   = snap.child("amount").getValue(Double.class);
                Long   time     = snap.child("time").getValue(Long.class);
                String category = snap.child("category").getValue(String.class);

                if (amount == null || time == null) continue;

                totalExpense += amount;
                transactionList.add(new TransactionModel(
                        "Expense", amount, time,
                        category != null ? category : "Expense",
                        true
                ));
            }

            // BUG FIX: tvIncome / tvExpense now show clean amounts to match new UI labels
            if (tvIncome  != null) tvIncome.setText("₹" + moneyFormat.format(totalIncome));
            if (tvExpense != null) tvExpense.setText("₹" + moneyFormat.format(totalExpense));

            updateBalances();
            finalizeList();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) { /* no-op */ }
    };

    // =====================================================================
    // FRAGMENT LIFECYCLE
    // =====================================================================

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
        setupDiscoverSection();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop voice first (stops listening + hides dialog)
        stopDashboardVoice();

        // BUG FIX: destroy recognizer and null both references so they
        //          are fully recreated next time the fragment resumes
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        speechIntent = null;

        // BUG FIX: null-out voiceDialog so it is rebuilt fresh on next show;
        //          prevents window leak when the fragment is detached
        voiceDialog = null;
    }

    // =====================================================================
    // INIT
    // =====================================================================

    private void initViews(View view) {
        rvRecent         = view.findViewById(R.id.rvRecent);
        tvIncome         = view.findViewById(R.id.tvIncome);
        tvExpense        = view.findViewById(R.id.tvExpense);
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
        tvUserName       = view.findViewById(R.id.tvUserName);
        btnViewAll       = view.findViewById(R.id.btnViewAll);
        cardAddIncome    = view.findViewById(R.id.cardAddIncome);
        cardAddExpense   = view.findViewById(R.id.cardAddExpense);
        cardBudgetSummary= view.findViewById(R.id.cardBudgetSummary);
        rvDiscover       = view.findViewById(R.id.rvDiscover);
        fabVoiceDashboard= view.findViewById(R.id.fabVoiceDashboard);
    }

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        userRef    = FirebaseDatabase.getInstance().getReference("users").child(userId);
        incomeRef  = userRef.child("incomes");
        expenseRef = userRef.child("expenses");
    }

    // =====================================================================
    // USER PROFILE
    // =====================================================================

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
                    tvUserName.setText("Friend 👋");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private String capitalize(String text) {
        if (TextUtils.isEmpty(text)) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void fadeIn(View view) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(400);
        view.startAnimation(anim);
    }

    // =====================================================================
    // BALANCE
    // =====================================================================

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
        // Newest first
        Collections.sort(transactionList, (a, b) -> Long.compare(b.getTime(), a.getTime()));

        // Show only last 6
        if (transactionList.size() > 6) {
            transactionList.subList(6, transactionList.size()).clear();
        }

        adapter.notifyDataSetChanged();
    }

    // =====================================================================
    // RECENT LIST
    // =====================================================================

    private void setupRecentList() {
        adapter = new RecentTransactionAdapter(transactionList);
        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecent.setAdapter(adapter);
    }

    // =====================================================================
    // DISCOVER SECTION
    // =====================================================================

    private void setupDiscoverSection() {
        List<DiscoverCategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new DiscoverCategoryModel("Shopping",    R.drawable.ic_offer));
        categoryList.add(new DiscoverCategoryModel("Food",        R.drawable.ic_food));
        categoryList.add(new DiscoverCategoryModel("Travel",      R.drawable.ic_travel));
        categoryList.add(new DiscoverCategoryModel("Movies",      R.drawable.ic_movie));
        categoryList.add(new DiscoverCategoryModel("Electronics", R.drawable.ic_stocks));
        categoryList.add(new DiscoverCategoryModel("Health",      R.drawable.ic_gym));

        DiscoverCategoryAdapter discoverAdapter =
                new DiscoverCategoryAdapter(requireContext(), categoryList, category -> {
                    Intent intent = new Intent(requireContext(), DiscoverActivity.class);
                    intent.putExtra("selectedCategory", category);
                    startActivity(intent);
                });

        rvDiscover.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDiscover.setAdapter(discoverAdapter);
    }

    // =====================================================================
    // CARD CLICKS
    // =====================================================================

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
            if (navigationListener != null) navigationListener.openReports();
        });

        if (cardBudgetSummary != null) {
            cardBudgetSummary.setOnClickListener(v -> {
                if (navigationListener != null) navigationListener.openBudget();
            });
        }
    }

    // =====================================================================
    // ENTRY ANIMATION
    // =====================================================================

    private void playEntryAnimation() {
        if (getContext() == null) return;

        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.card_fade_slide);

        if (cardAddIncome    != null) cardAddIncome.startAnimation(anim);
        if (cardAddExpense   != null) cardAddExpense.startAnimation(anim);
        if (rvRecent         != null) rvRecent.startAnimation(anim);
        if (fabVoiceDashboard!= null) fabVoiceDashboard.startAnimation(anim);
    }

    // =====================================================================
    // VOICE – DASHBOARD MIC SETUP
    // =====================================================================

    private void setupDashboardVoice() {
        if (fabVoiceDashboard == null) return;

        fabVoiceDashboard.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startDashboardVoice();
        });
    }

    private void startDashboardVoice() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_AUDIO_PERMISSION
            );
            Toast.makeText(requireContext(), "Allow mic permission first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Voice input not supported on this device",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // BUG FIX: always create a fresh recognizer (old one was destroyed in onDestroyView)
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
                speechRecognizer.setRecognitionListener(buildRecognitionListener());
            }

            // BUG FIX: always build intent fresh if null
            if (speechIntent == null) {
                speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
                speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            }

            speechRecognizer.startListening(speechIntent);

        } catch (Exception e) {
            hideVoiceDialog();
            Toast.makeText(requireContext(), "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    /** Extracted to a helper method to keep startDashboardVoice() clean. */
    private RecognitionListener buildRecognitionListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                showVoiceDialog();
            }

            @Override
            public void onResults(Bundle results) {
                hideVoiceDialog();
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handleDashboardVoice(matches.get(0));
                }
            }

            @Override
            public void onError(int error) {
                hideVoiceDialog();
                Toast.makeText(requireContext(), "Couldn't hear you, try again 😅",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (micCard == null) return;
                float normalized = Math.min(10f, Math.max(0f, rmsdB));
                float scale = 1.0f + (normalized / 50f); // 1.0 → ~1.2
                micCard.animate()
                        .scaleX(scale).scaleY(scale)
                        .setDuration(120)
                        .start();
            }

            // Required but unused callbacks
            @Override public void onBeginningOfSpeech()  { }
            @Override public void onEndOfSpeech()        { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        };
    }

    private void stopDashboardVoice() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
            }
        } catch (Exception ignored) { }

        hideVoiceDialog();
    }

    // Permission callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDashboardVoice();
            } else {
                Toast.makeText(requireContext(), "Mic permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =====================================================================
    // VOICE DIALOG  –  SHOW / HIDE
    // =====================================================================

    private void showVoiceDialog() {
        if (getContext() == null) return;

        // BUG FIX: voiceDialog is nulled in onDestroyView, so it is always
        //          rebuilt when the fragment is re-attached, preventing a
        //          window-already-added crash.
        if (voiceDialog == null) {
            voiceDialog = new Dialog(requireContext());
            voiceDialog.setContentView(R.layout.dialog_voice_listening);
            voiceDialog.setCancelable(false);

            if (voiceDialog.getWindow() != null) {
                voiceDialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                voiceDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            pulse1  = voiceDialog.findViewById(R.id.pulse1);
            pulse2  = voiceDialog.findViewById(R.id.pulse2);
            pulse3  = voiceDialog.findViewById(R.id.pulse3);
            micCard = voiceDialog.findViewById(R.id.micCard);
            tvDots  = voiceDialog.findViewById(R.id.tvDots);

            TextView btnCancel = voiceDialog.findViewById(R.id.btnCancelVoice);
            if (btnCancel != null) btnCancel.setOnClickListener(v -> stopDashboardVoice());

            // ── Pulse animations ──
            pulseAnim1 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim1.setRepeatCount(Animation.INFINITE);

            pulseAnim2 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim2.setRepeatCount(Animation.INFINITE);
            pulseAnim2.setStartOffset(220);

            // BUG FIX: pulseAnim3 is now stored in its own field (was incorrectly
            //          assigned via a tag, and pulseAnim2 was self-assigned).
            pulseAnim3 = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);
            pulseAnim3.setRepeatCount(Animation.INFINITE);
            pulseAnim3.setStartOffset(420);

            micBounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.mic_bounce);
            dotsAnim      = AnimationUtils.loadAnimation(requireContext(), R.anim.dots_fade);
        }

        try {
            if (!voiceDialog.isShowing()) voiceDialog.show();
        } catch (Exception ignored) { }

        if (pulse1  != null) pulse1.startAnimation(pulseAnim1);
        if (pulse2  != null) pulse2.startAnimation(pulseAnim2);
        if (pulse3  != null) pulse3.startAnimation(pulseAnim3); // BUG FIX: uses field directly
        if (micCard != null) micCard.startAnimation(micBounceAnim);
        if (tvDots  != null) tvDots.startAnimation(dotsAnim);
    }

    private void hideVoiceDialog() {
        try {
            if (pulse1  != null) pulse1.clearAnimation();
            if (pulse2  != null) pulse2.clearAnimation();
            if (pulse3  != null) pulse3.clearAnimation();
            if (micCard != null) {
                micCard.clearAnimation();
                micCard.setScaleX(1f);
                micCard.setScaleY(1f);
            }
            if (tvDots  != null) tvDots.clearAnimation();

            if (voiceDialog != null && voiceDialog.isShowing()) {
                voiceDialog.dismiss();
            }
        } catch (Exception ignored) { }
    }

    // =====================================================================
    // VOICE COMMAND HANDLER
    // =====================================================================

    private void handleDashboardVoice(String rawText) {
        if (userId == null || userRef == null) return;

        String text = rawText.toLowerCase(Locale.ROOT);

        double amount = extractAmount(text);
        if (amount <= 0) {
            Toast.makeText(requireContext(),
                    "Couldn't detect amount 😅", Toast.LENGTH_SHORT).show();
            return;
        }

        long   time     = detectDateMillis(text);
        boolean income  = isIncomeText(text);

        if (income) {
            saveIncome(amount, time,
                    detectIncomeSource(text),
                    detectPaymentMode(text),
                    cleanNote(text));
        } else {
            saveExpense(amount, time,
                    detectExpenseCategory(text),
                    detectPaymentMode(text),
                    cleanNote(text));
        }
    }

    // =====================================================================
    // FIREBASE SAVE
    // =====================================================================

    private void saveIncome(double amount, long time,
                            String source, String paymentMode, String note) {
        if (incomeRef == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("amount",      amount);
        map.put("time",        time);
        map.put("source",      source);
        map.put("paymentMode", paymentMode);
        map.put("note",        note);

        incomeRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    NotificationHelper.showIncomeSummaryNotification(
                            requireContext(), amount, source);
                    Toast.makeText(requireContext(),
                            "Income added: ₹" + moneyFormat.format(amount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void saveExpense(double amount, long time,
                             String category, String paymentMode, String note) {
        if (expenseRef == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("amount",      amount);
        map.put("time",        time);
        map.put("category",    category);
        map.put("paymentMode", paymentMode);
        map.put("note",        note);

        expenseRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    NotificationHelper.showExpenseSummaryNotification(
                            requireContext(), amount, category);
                    Toast.makeText(requireContext(),
                            "Expense added: ₹" + moneyFormat.format(amount),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // =====================================================================
    // SMART NLP HELPERS
    // =====================================================================

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
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception ignored) { }
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
        if (text.contains("upi")   || text.contains("gpay")
                || text.contains("phonepe") || text.contains("paytm")) return "UPI";
        if (text.contains("cash"))                                       return "Cash";
        if (text.contains("card")  || text.contains("credit")
                || text.contains("debit"))                               return "Card";
        if (text.contains("bank")  || text.contains("transfer"))         return "Bank Transfer";
        if (text.contains("cheque"))                                      return "Cheque";
        return "Cash";
    }

    private String detectIncomeSource(String text) {
        if (text.contains("salary"))                                             return "Salary";
        if (text.contains("business"))                                           return "Business";
        if (text.contains("freelance"))                                          return "Freelance";
        if (text.contains("investment") || text.contains("stock")
                || text.contains("profit"))                                      return "Investment";
        if (text.contains("rent") || text.contains("rental"))                   return "Rental Income";
        if (text.contains("bonus"))                                              return "Bonus";
        if (text.contains("gift"))                                               return "Gift";
        return "Other";
    }

    private String detectExpenseCategory(String text) {
        if (text.contains("uber")  || text.contains("ola")    || text.contains("metro")
                || text.contains("bus")  || text.contains("train")  || text.contains("petrol")
                || text.contains("fuel"))                                        return "Transport";

        if (text.contains("dominos") || text.contains("pizza") || text.contains("cafe")
                || text.contains("coffee") || text.contains("restaurant")
                || text.contains("chai") || text.contains("tea")
                || text.contains("mcd") || text.contains("kfc"))                return "Food";

        if (text.contains("amazon") || text.contains("flipkart")
                || text.contains("shopping") || text.contains("mall")
                || text.contains("store"))                                       return "Shopping";

        if (text.contains("netflix") || text.contains("movie")
                || text.contains("pvr") || text.contains("cinema"))             return "Entertainment";

        if (text.contains("doctor") || text.contains("hospital")
                || text.contains("medicine") || text.contains("clinic")
                || text.contains("pharmacy"))                                    return "Health";

        if (text.contains("fees") || text.contains("college")
                || text.contains("school") || text.contains("course")
                || text.contains("gym"))                                         return "Education";

        if (text.contains("bill") || text.contains("electricity")
                || text.contains("recharge") || text.contains("wifi")
                || text.contains("rent"))                                        return "Bills";

        return "Other";
    }

    private String cleanNote(String text) {
        String clean = text
                .replace("day before yesterday", "").replace("yesterday", "")
                .replace("today", "").replace("aaj", "")
                .replace("kal",   "").replace("parso", "")
                .replace("upi",   "").replace("gpay",  "")
                .replace("phonepe","").replace("paytm","")
                .replace("cash",   "").replace("card",  "")
                .replace("debit",  "").replace("credit","")
                .replace("bank transfer","").replace("transfer","")
                .replace("bank",  "").replace("cheque","")
                .replace("rupees","").replace("rupaye","")
                .replace("rs",    "").replace("₹",     "")
                .replaceAll("(\\d+(?:\\.\\d{1,2})?)", "")
                .replaceAll("\\s+", " ").trim();

        if (clean.isEmpty()) return "Transaction";
        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }
}