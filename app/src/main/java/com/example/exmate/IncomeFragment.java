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

public class IncomeFragment extends Fragment {

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;
    private long selectedDateMillis = -1;

    private DatabaseReference incomeRef;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.income_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();
        setupSourceSpinner();
        setupPaymentSpinner();
        setupDatePicker();
        createTransactionChannel();

        btnSaveIncome.setOnClickListener(v -> validateAndSave());
    }

    private void initViews(View view) {
        etIncomeAmount = view.findViewById(R.id.etIncomeAmount);
        etIncomeDate   = view.findViewById(R.id.etIncomeDate);
        etIncomeNote   = view.findViewById(R.id.etIncomeNote);
        spIncomeSource = view.findViewById(R.id.spIncomeSource);
        spPaymentMode  = view.findViewById(R.id.spPaymentMode);
        btnSaveIncome  = view.findViewById(R.id.btnSaveIncome);
    }

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            incomeRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("incomes");
        }
    }

    private void setupSourceSpinner() {
        String[] sources = {"Salary", "Business", "Freelance", "Investment",
                "Rental Income", "Bonus", "Gift", "Other"};

        spIncomeSource.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sources
        ));
    }

    private void setupPaymentSpinner() {
        String[] modes = {"Cash", "UPI", "Bank Transfer", "Card", "Cheque"};

        spPaymentMode.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                modes
        ));
    }

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
                etIncomeAmount.setError("Amount must be more than 0");
                return;
            }
        } catch (Exception e) {
            etIncomeAmount.setError("Invalid amount");
            return;
        }

        if (selectedDateMillis == -1) {
            etIncomeDate.setError("Select date");
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("time", selectedDateMillis);
        map.put("source", spIncomeSource.getSelectedItem().toString());
        map.put("paymentMode", spPaymentMode.getSelectedItem().toString());
        map.put("note", etIncomeNote.getText().toString().trim());

        incomeRef.push().setValue(map)
                .addOnSuccessListener(unused -> {
                    showTransactionNotification("Income Added",
                            "₹" + amount + " added from " + spIncomeSource.getSelectedItem());

                    // 🌟 Reset fields
                    etIncomeAmount.setText("");
                    etIncomeDate.setText("");
                    etIncomeNote.setText("");
                    spIncomeSource.setSelection(0);
                    spPaymentMode.setSelection(0);
                    selectedDateMillis = -1;

                    // 🔄 Refresh Dashboard
                    requireActivity().setResult(101);

                    Toast.makeText(getContext(), "Income Saved!", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                });
    }

    private void createTransactionChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel("transaction_alert",
                            "Transaction Alerts",
                            android.app.NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager =
                    requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showTransactionNotification(String title, String msg) {
        Intent intent = new Intent(getContext(), UserDashboardActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification =
                new NotificationCompat.Builder(requireContext(), "transaction_alert")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("💵 " + title)
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .setContentIntent(pending)
                        .build();

        NotificationManager manager =
                (NotificationManager) requireContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), notification);
    }
}
