package com.example.exmate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class IncomeFragment extends Fragment {

    private static final int REQ_CODE_SPEECH = 1101;

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;
    private ProgressBar progressSaveIncome;

    // 🎙️ Voice
    private com.google.android.material.card.MaterialCardView btnVoiceIncome;

    private android.widget.TextView tvVoiceDetectedIncome;

    private long selectedDateMillis = -1;
    private DatabaseReference incomeRef;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.income_fragment, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupSourceSpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        setupDefaultDate();
        setupAmountWatcher();
        focusAmountField();

        btnSaveIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndSave();
        });

        // 🎙️ Voice button
        btnVoiceIncome.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startVoiceInput();
        });
    }

    // ================= INIT =================
    private void initViews(View view) {
        etIncomeAmount     = view.findViewById(R.id.etIncomeAmount);
        etIncomeDate       = view.findViewById(R.id.etIncomeDate);
        etIncomeNote       = view.findViewById(R.id.etIncomeNote);
        spIncomeSource     = view.findViewById(R.id.spIncomeSource);
        spPaymentMode      = view.findViewById(R.id.spPaymentMode);
        btnSaveIncome      = view.findViewById(R.id.btnSaveIncome);
        progressSaveIncome = view.findViewById(R.id.progressSaveIncome);

        // 🎙️ NEW
        btnVoiceIncome = view.findViewById(R.id.btnVoiceIncome);
        tvVoiceDetectedIncome = view.findViewById(R.id.tvVoiceDetectedIncome);

        progressSaveIncome.setVisibility(View.GONE);
        btnSaveIncome.setEnabled(false);
    }

    // ================= FIREBASE =================
    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(requireContext(),
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        incomeRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("incomes");
    }

    // ================= SPINNERS =================
    private void setupSourceSpinner() {
        String[] sources = {
                "Salary", "Business", "Freelance", "Investment",
                "Rental Income", "Bonus", "Gift", "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        sources);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spIncomeSource.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Cheque"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        modes);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spPaymentMode.setAdapter(adapter);
    }

    // ================= DATE PICKER =================
    private void setupDatePicker() {
        etIncomeDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (picker, y, m, d) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        selectedDateMillis = cal.getTimeInMillis();
                        etIncomeDate.setText(d + "/" + (m + 1) + "/" + y);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ================= UX HELPERS =================
    private void setupDefaultDate() {
        Date now = new Date();
        selectedDateMillis = now.getTime();
        etIncomeDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(now)
        );
    }

    private void setupAmountWatcher() {
        etIncomeAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double val = Double.parseDouble(s.toString());
                    btnSaveIncome.setEnabled(val > 0);
                } catch (Exception e) {
                    btnSaveIncome.setEnabled(false);
                }
            }
        });
    }

    private void focusAmountField() {
        etIncomeAmount.requestFocus();
        etIncomeAmount.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etIncomeAmount, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    // ================= VOICE =================
    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your income");

            Toast.makeText(requireContext(), "Listening...", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, REQ_CODE_SPEECH);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Voice input not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH &&
                resultCode == Activity.RESULT_OK &&
                data != null) {

            ArrayList<String> result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (result != null && !result.isEmpty()) {

                String rawText = result.get(0);
                String lower = rawText.toLowerCase(Locale.ROOT);

                // Amount detect (simple)
                double amount = detectAmount(lower);
                if (amount > 0) etIncomeAmount.setText(String.valueOf(amount));

                // Note
                etIncomeNote.setText(cleanIncomeNote(lower));

                // Date (basic)
                long time = detectDateMillis(lower);
                selectedDateMillis = time;
                etIncomeDate.setText(
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(new Date(time))
                );

                // Payment Mode detect
                String pay = detectPaymentMode(lower);
                setSpinnerByText(spPaymentMode, pay);

                // Source detect
                String src = detectIncomeSource(lower);
                setSpinnerByText(spIncomeSource, src);

                // UI feedback
                tvVoiceDetectedIncome.setVisibility(View.VISIBLE);
                tvVoiceDetectedIncome.setText("🎙️ Detected: " + rawText);
            }
        }
    }

    // ===== Voice helpers =====
    private double detectAmount(String text) {
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
        if (text.contains("refund") || text.contains("cashback")) return "Other";

        return "Other";
    }

    private String cleanIncomeNote(String text) {

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

        if (clean.isEmpty()) return "Income";

        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    private void setSpinnerByText(Spinner spinner, String text) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;

        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(text)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // ================= SAVE =================
    private void validateAndSave() {

        if (incomeRef == null) return;

        String amountStr = etIncomeAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etIncomeAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etIncomeAmount.setError("Amount must be > 0");
                return;
            }
        } catch (Exception e) {
            etIncomeAmount.setError("Invalid amount");
            return;
        }

        // 🔥 FIX: reset se pehle source save
        String selectedSource = spIncomeSource.getSelectedItem().toString();

        btnSaveIncome.setEnabled(false);
        progressSaveIncome.setVisibility(View.VISIBLE);

        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("time", selectedDateMillis);
        map.put("source", selectedSource);
        map.put("paymentMode", spPaymentMode.getSelectedItem().toString());
        map.put("note", etIncomeNote.getText().toString().trim());

        incomeRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    hideLoader();
                    resetFields();

                    NotificationHelper.showIncomeSummaryNotification(
                            requireContext(),
                            amount,
                            selectedSource
                    );

                    Toast.makeText(requireContext(),
                            "Income saved successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideLoader();
                    Toast.makeText(requireContext(),
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void hideLoader() {
        progressSaveIncome.setVisibility(View.GONE);
        btnSaveIncome.setEnabled(true);
    }

    private void resetFields() {
        etIncomeAmount.setText("");
        etIncomeNote.setText("");
        spIncomeSource.setSelection(0);
        spPaymentMode.setSelection(0);

        tvVoiceDetectedIncome.setText("");
        tvVoiceDetectedIncome.setVisibility(View.GONE);

        setupDefaultDate();
    }

    // ================= NOTIFICATION =================
    private void createTransactionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            requireContext()
                    .getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }
}
