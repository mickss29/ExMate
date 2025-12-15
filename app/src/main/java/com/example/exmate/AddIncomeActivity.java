package com.example.exmate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddIncomeActivity extends AppCompatActivity {

    private EditText etIncomeAmount, etIncomeDate, etIncomeNote;
    private Spinner spIncomeSource, spPaymentMode;
    private Button btnSaveIncome;

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
        String amount = etIncomeAmount.getText().toString().trim();
        String date   = etIncomeDate.getText().toString().trim();

        if (amount.isEmpty()) {
            etIncomeAmount.setError("Enter amount");
            return;
        }

        if (date.isEmpty()) {
            etIncomeDate.setError("Select date");
            return;
        }

        // 🔒 Firebase logic will be added later by your partner
        Toast.makeText(this, "Income saved (UI only)", Toast.LENGTH_SHORT).show();

        finish(); // return to dashboard
    }
}
