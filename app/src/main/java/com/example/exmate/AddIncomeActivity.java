package com.example.exmate;

import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddIncomeActivity extends AppCompatActivity {

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;

    private DatabaseReference incomeRef;
    private String userId;

    // ✅ Store selected date correctly
    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        etIncomeAmount = findViewById(R.id.etIncomeAmount);
        etIncomeDate   = findViewById(R.id.etIncomeDate);
        etIncomeNote   = findViewById(R.id.etIncomeNote);
        spIncomeSource = findViewById(R.id.spIncomeSource);
        spPaymentMode  = findViewById(R.id.spPaymentMode);
        btnSaveIncome  = findViewById(R.id.btnSaveIncome);

        setupSourceSpinner();
        setupPaymentSpinner();
        setupDatePicker();
        setupFirebase();

        btnSaveIncome.setOnClickListener(v -> validateAndSave());
        createTransactionChannel();

    }

    // ================= FIREBASE =================

    private void setupFirebase() {
        userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
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
                "Salary",
                "Business",
                "Freelance",
                "Investment",
                "Rental Income",
                "Bonus",
                "Gift",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sources
        );
        spIncomeSource.setAdapter(adapter);
    }

    private void setupPaymentSpinner() {
        String[] modes = {
                "Cash",
                "UPI",
                "Bank Transfer",
                "Card",
                "Cheque"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
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
                    this,
                    (view, y, m, d) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(y, m, d, 0, 0, 0);
                        selectedCal.set(Calendar.MILLISECOND, 0);

                        // ✅ save selected date
                        selectedDateMillis = selectedCal.getTimeInMillis();

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
            etIncomeAmount.setError("Invalid amount");
            return;
        }

        if (selectedDateMillis == -1) {
            etIncomeDate.setError("Select date");
            return;
        }

        Map<String, Object> incomeMap = new HashMap<>();
        incomeMap.put("amount", amount);
        incomeMap.put("time", selectedDateMillis); // ✅ correct date saved
        incomeMap.put("source", spIncomeSource.getSelectedItem().toString());
        incomeMap.put("paymentMode", spPaymentMode.getSelectedItem().toString());
        incomeMap.put("note", etIncomeNote.getText().toString().trim());

        incomeRef.push().setValue(incomeMap)
                .addOnSuccessListener(unused -> {

                    double amt = Double.parseDouble(
                            etIncomeAmount.getText().toString().trim()
                    );
                    String source = spIncomeSource.getSelectedItem().toString();

                    showTransactionNotification(
                            "✅ Income Added",
                            "Income of ₹" + amt + " added from " + source
                    );

                    Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })


                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    private void createTransactionChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            android.app.NotificationChannel channel =
                    new android.app.NotificationChannel(
                            "transaction_alert",
                            "Transaction Alerts",
                            android.app.NotificationManager.IMPORTANCE_DEFAULT
                    );

            channel.setDescription("Transaction added notifications");

            android.app.NotificationManager manager =
                    getSystemService(android.app.NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }
    private void showIncomeNotification(
            double amount,
            String source
    ) {
        String title = "✅ Income Added";
        String msg = "Income of ₹" + amount + " added from " + source;

        android.app.Notification notification =
                new androidx.core.app.NotificationCompat.Builder(
                        this, "transaction_alert"
                )
                        .setSmallIcon(R.drawable.ic_notification) // your app icon
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build();

        android.app.NotificationManager manager =
                (android.app.NotificationManager)
                        getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), notification);
    }
    private void showTransactionNotification(
            String title,
            String message
    ) {

        // 👉 Open Dashboard on tap
        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new NotificationCompat.Builder(this, "transaction_alert")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setContentIntent(pendingIntent) // 👈 TAP ACTION
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_SOUND
                                | NotificationCompat.DEFAULT_VIBRATE) // 🔊📳
                        .build();

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) System.currentTimeMillis(), notification);
    }


}
