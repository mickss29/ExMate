package com.example.exmate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
                    (view, y, m, d) ->
                            etExpenseDate.setText(d + "/" + (m + 1) + "/" + y),
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
        String date = etExpenseDate.getText().toString().trim();

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

        if (date.isEmpty()) {
            etExpenseDate.setError("Select date");
            return;
        }

        // Firebase data
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("time", System.currentTimeMillis());
        data.put("category", spExpenseCategory.getSelectedItem().toString());
        data.put("paymentMode", spExpensePaymentMode.getSelectedItem().toString());
        data.put("type", spExpenseType.getSelectedItem().toString());
        data.put("note", etExpenseNote.getText().toString().trim());

        expenseRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
