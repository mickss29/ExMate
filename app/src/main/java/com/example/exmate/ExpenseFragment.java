package com.example.exmate;

import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ExpenseFragment extends Fragment {

    private EditText etExpenseAmount, etExpenseDate, etExpenseNote;
    private Spinner spExpenseCategory, spExpensePaymentMode;
    private Button btnSaveExpense;
    private ProgressBar progressSave;

    private DatabaseReference expenseRef;
    private String userId;
    private long selectedDateMillis = -1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.expense_fragment, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupCategorySpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        setupDefaultDate();
        setupAmountWatcher();
        focusAmountField();

        btnSaveExpense.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndSave();
        });
    }

    // ================= INIT =================
    private void initViews(View view) {
        etExpenseAmount      = view.findViewById(R.id.etExpenseAmount);
        etExpenseDate        = view.findViewById(R.id.etExpenseDate);
        etExpenseNote        = view.findViewById(R.id.etExpenseNote);
        spExpenseCategory    = view.findViewById(R.id.spExpenseCategory);
        spExpensePaymentMode = view.findViewById(R.id.spExpensePaymentMode);
        btnSaveExpense       = view.findViewById(R.id.btnSaveExpense);
        progressSave         = view.findViewById(R.id.progressSave);

        progressSave.setVisibility(View.GONE);
        btnSaveExpense.setEnabled(false);
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

        expenseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("expenses");
    }

    // ================= SPINNERS =================
    private void setupCategorySpinner() {
        String[] categories = {
                "Food", "Transport", "Shopping", "Bills",
                "Entertainment", "Health", "Education", "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        categories);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpenseCategory.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Wallet"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_premium,
                        modes);

        adapter.setDropDownViewResource(R.layout.spinner_item_premium);
        spExpensePaymentMode.setAdapter(adapter);
    }

    // ================= DATE PICKER =================
    private void setupDatePicker() {
        etExpenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (picker, y, m, d) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        selectedDateMillis = cal.getTimeInMillis();
                        etExpenseDate.setText(d + "/" + (m + 1) + "/" + y);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // ================= UX HELPERS =================
    private void setupDefaultDate() {
        Date now = new Date();
        selectedDateMillis = now.getTime();
        etExpenseDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(now)
        );
    }

    private void setupAmountWatcher() {
        etExpenseAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    double val = Double.parseDouble(s.toString());
                    btnSaveExpense.setEnabled(val > 0);
                } catch (Exception e) {
                    btnSaveExpense.setEnabled(false);
                }
            }
        });
    }

    private void focusAmountField() {
        etExpenseAmount.requestFocus();
        etExpenseAmount.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etExpenseAmount, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    // ================= SAVE =================
    private void validateAndSave() {

        if (expenseRef == null) return;

        String amountStr = etExpenseAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etExpenseAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etExpenseAmount.setError("Amount must be > 0");
                return;
            }
        } catch (Exception e) {
            etExpenseAmount.setError("Invalid amount");
            return;
        }

        // 🔥 IMPORTANT: reset se pehle category store
        String selectedCategory = spExpenseCategory.getSelectedItem().toString();

        btnSaveExpense.setEnabled(false);
        progressSave.setVisibility(View.VISIBLE);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", selectedDateMillis);
        data.put("category", selectedCategory);
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());

        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    hideLoader();
                    resetFields();

                    // 🔔 FINAL CORRECT NOTIFICATION CALL
                    NotificationHelper.showExpenseSummaryNotification(
                            requireContext(),
                            amount,
                            selectedCategory
                    );

                    Toast.makeText(requireContext(),
                            "Expense added successfully",
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
        progressSave.setVisibility(View.GONE);
        btnSaveExpense.setEnabled(true);
    }

    private void resetFields() {
        etExpenseAmount.setText("");
        etExpenseNote.setText("");
        spExpenseCategory.setSelection(0);
        spExpensePaymentMode.setSelection(0);
        setupDefaultDate();
    }

    // ================= NOTIFICATION CHANNEL =================
    private void createTransactionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts.",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            requireContext()
                    .getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }
}
