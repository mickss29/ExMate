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

public class AddIncomeActivity extends Fragment {

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;

    private DatabaseReference incomeRef;
    private String userId;

    private long selectedDateMillis = -1; // date

    public AddIncomeActivity() {} // empty constructor

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.income_fragment, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // ====== BIND UI VIEWS ======
        etIncomeAmount = view.findViewById(R.id.etIncomeAmount);
        etIncomeDate   = view.findViewById(R.id.etIncomeDate);
        etIncomeNote   = view.findViewById(R.id.etIncomeNote);
        btnSaveIncome  = view.findViewById(R.id.btnSaveIncome);

        // ❗ These were missing earlier (reason for crash)
        spIncomeSource = view.findViewById(R.id.spIncomeSource);
        spPaymentMode  = view.findViewById(R.id.spPaymentMode);

        // ====== SETUP ======
        setupFirebase();
        setupSourceSpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        // ====== SAVE CLICK ======
        btnSaveIncome.setOnClickListener(v -> validateAndSave());
    }

    // ================= FIREBASE =================
    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sources
        );
        spIncomeSource.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Cheque"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                modes
        );
        spPaymentMode.setAdapter(adapter);
    }

    // ================= DATE PICKER =================
    private void setupDatePicker() {
        etIncomeDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, y, m, d) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 0, 0, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        selectedDateMillis = cal.getTimeInMillis();
                        etIncomeDate.setText(d + "/" + (m + 1) + "/" + y);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();
        });
    }

    // ================= SAVE =================
    private void validateAndSave() {

        String amountStr = etIncomeAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etIncomeAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etIncomeAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            etIncomeAmount.setError("Invalid number");
            return;
        }

        if (selectedDateMillis == -1) {
            etIncomeDate.setError("Select date");
            return;
        }

        Map<String, Object> incomeMap = new HashMap<>();
        incomeMap.put("amount", amount);
        incomeMap.put("time", selectedDateMillis);
        incomeMap.put("source", spIncomeSource.getSelectedItem().toString());
        incomeMap.put("paymentMode", spPaymentMode.getSelectedItem().toString());
        incomeMap.put("note", etIncomeNote.getText().toString().trim());

        incomeRef.push().setValue(incomeMap)
                .addOnSuccessListener(unused -> {

                    showTransactionNotification(
                            "Income Added",
                            "Income of ₹" + amount + " from " +
                                    spIncomeSource.getSelectedItem().toString()
                    );

                    Toast.makeText(requireContext(),
                            "Income added successfully", Toast.LENGTH_SHORT).show();

                    resetFields(); // ⭐ Auto reset after save

                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ⭐ RESET INPUTS AFTER SAVE
    private void resetFields() {
        etIncomeAmount.setText("");
        etIncomeDate.setText("");
        etIncomeNote.setText("");
        selectedDateMillis = -1;
        spIncomeSource.setSelection(0);
        spPaymentMode.setSelection(0);
    }

    // ================= NOTIFICATION =================
    private void createTransactionChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts",
                            android.app.NotificationManager.IMPORTANCE_HIGH
                    );

            android.app.NotificationManager manager =
                    requireContext().getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void showTransactionNotification(String title, String message) {

        Intent intent = new Intent(requireContext(), UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        requireContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new NotificationCompat.Builder(requireContext(), "transaction_alert")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("💰 " + title)
                        .setContentText(message)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build();

        NotificationManager manager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), notification);
    }
}
