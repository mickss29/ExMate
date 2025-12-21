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
                    Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
