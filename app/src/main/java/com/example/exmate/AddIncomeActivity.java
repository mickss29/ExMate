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

        // 🔐 Firebase init
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

        btnSaveIncome.setOnClickListener(v -> validateAndSave());
    }

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

    private void setupDatePicker() {
        etIncomeDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int year  = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day   = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        String date = d + "/" + (m + 1) + "/" + y;
                        etIncomeDate.setText(date);
                    },
                    year, month, day
            );
            dialog.show();
        });
    }

    private void validateAndSave() {

        String amountStr = etIncomeAmount.getText().toString().trim();
        String date      = etIncomeDate.getText().toString().trim();
        String source    = spIncomeSource.getSelectedItem().toString();
        String payment   = spPaymentMode.getSelectedItem().toString();
        String note      = etIncomeNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etIncomeAmount.setError("Enter amount");
            return;
        }

        if (date.isEmpty()) {
            etIncomeDate.setError("Select date");
            return;
        }

        double amount = Double.parseDouble(amountStr);

        String incomeId = incomeRef.push().getKey();
        if (incomeId == null) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> incomeMap = new HashMap<>();
        incomeMap.put("amount", amount);
        incomeMap.put("date", date);
        incomeMap.put("source", source);
        incomeMap.put("paymentMode", payment);
        incomeMap.put("note", note);
        incomeMap.put("time", System.currentTimeMillis());

        incomeRef.child(incomeId).setValue(incomeMap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
