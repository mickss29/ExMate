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

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etExpenseAmount, etExpenseDate, etExpenseNote;
    private Spinner spExpenseCategory, spExpensePaymentMode, spExpenseType;
    private Button btnSaveExpense;

    // Firebase
    private DatabaseReference expenseRef;
    private String userId;

    // ✅ Store selected date properly
    private long selectedDateMillis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseDate   = findViewById(R.id.etExpenseDate);
        etExpenseNote   = findViewById(R.id.etExpenseNote);

        spExpenseCategory    = findViewById(R.id.spExpenseCategory);
        spExpensePaymentMode = findViewById(R.id.spExpensePaymentMode);
        spExpenseType        = findViewById(R.id.spExpenseType);

        btnSaveExpense = findViewById(R.id.btnSaveExpense);

        setupCategorySpinner();
        setupPaymentModeSpinner();
        setupExpenseTypeSpinner();
        setupDatePicker();
        setupFirebase();

        btnSaveExpense.setOnClickListener(v -> validateAndSave());
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spExpenseCategory.setAdapter(adapter);
    }

    private void setupPaymentModeSpinner() {
        String[] modes = {
                "Cash", "UPI", "Bank Transfer", "Card", "Wallet"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                modes
        );
        spExpensePaymentMode.setAdapter(adapter);
    }

    private void setupExpenseTypeSpinner() {
        String[] types = { "Personal", "Business" };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                types
        );
        spExpenseType.setAdapter(adapter);
    }

    // ================= DATE PICKER =================

    private void setupDatePicker() {
        etExpenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(y, m, d, 0, 0, 0);
                        selectedCal.set(Calendar.MILLISECOND, 0);

                        // ✅ save selected date
                        selectedDateMillis = selectedCal.getTimeInMillis();

                        etExpenseDate.setText(d + "/" + (m + 1) + "/" + y);
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
        } catch (NumberFormatException e) {
            etExpenseAmount.setError("Invalid amount");
            return;
        }

        if (selectedDateMillis == -1) {
            etExpenseDate.setError("Select date");
            return;
        }

        // Firebase data
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", selectedDateMillis); // ✅ correct date saved
        data.put("category", spExpenseCategory.getSelectedItem().toString());
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("type", spExpenseType.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());

        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {

                    double amt = Double.parseDouble(
                            etExpenseAmount.getText().toString().trim()
                    );
                    String category = spExpenseCategory.getSelectedItem().toString();

                    showTransactionNotification(
                            "✅ Expense Added",
                            "Expense of ₹" + amt + " added under " + category
                    );

                    Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
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
    private void showTransactionNotification(
            String type,
            double amount,
            String category
    ) {
        String title = "✅ Transaction Added";
        String msg = type + " of ₹" + amount + " added under " + category;

        android.app.Notification notification =
                new androidx.core.app.NotificationCompat.Builder(
                        this, "transaction_alert"
                )
                        .setSmallIcon(R.drawable.ic_notification) // use your app icon
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
