package com.example.exmate;

import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ExpenseFragment extends Fragment {

    private EditText etExpenseAmount, etExpenseDate, etExpenseNote;
    private Spinner spExpenseCategory, spExpensePaymentMode;
    private Button btnSaveExpense;

    private DatabaseReference expenseRef;
    private String userId;
    private long selectedDateMillis = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.expense_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupCategorySpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        btnSaveExpense.setOnClickListener(v -> validateAndSave());
    }

    // ========= INIT =========
    private void initViews(View view) {
        etExpenseAmount       = view.findViewById(R.id.etExpenseAmount);
        etExpenseDate         = view.findViewById(R.id.etExpenseDate);
        etExpenseNote         = view.findViewById(R.id.etExpenseNote);
        spExpenseCategory     = view.findViewById(R.id.spExpenseCategory);
        spExpensePaymentMode  = view.findViewById(R.id.spExpensePaymentMode);
        btnSaveExpense        = view.findViewById(R.id.btnSaveExpense);
    }

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        expenseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("expenses");
    }

    // ========= SPINNERS =========
    private void setupCategorySpinner() {
        String[] categories = {
                "Food", "Transport", "Shopping", "Bills",
                "Entertainment", "Health", "Education", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spExpenseCategory.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Wallet"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                modes
        );
        spExpensePaymentMode.setAdapter(adapter);
    }

    // ========= DATE PICKER =========
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

    // ========= SAVE LOGIC + AUTO REFRESH + RESET =========
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
                etExpenseAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (Exception e) {
            etExpenseAmount.setError("Invalid amount");
            return;
        }

        if (selectedDateMillis == -1) {
            etExpenseDate.setError("Select date");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", selectedDateMillis);
        data.put("category", spExpenseCategory.getSelectedItem().toString());
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());

        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    showTransactionNotification(
                            "Expense Added",
                            "Expense of ₹" + amount + " added in " +
                                    spExpenseCategory.getSelectedItem().toString()
                    );

                    Toast.makeText(getContext(),
                            "Expense added successfully", Toast.LENGTH_SHORT).show();

                    // ⭐ reset fields after save
                    resetFields();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void resetFields() {
        etExpenseAmount.setText("");
        etExpenseDate.setText("");
        etExpenseNote.setText("");
        selectedDateMillis = -1;
        spExpenseCategory.setSelection(0);
        spExpensePaymentMode.setSelection(0);
    }

    // ========= NOTIFICATION =========
    private void createTransactionChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            NotificationManager manager =
                    requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showTransactionNotification(String title, String message) {
        Intent intent = new Intent(getContext(), UserDashboardActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                getContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification =
                new NotificationCompat.Builder(requireContext(), "transaction_alert")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("📉 " + title)
                        .setContentText(message)
                        .setContentIntent(pending)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                (NotificationManager) requireContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), notification);
    }
}
