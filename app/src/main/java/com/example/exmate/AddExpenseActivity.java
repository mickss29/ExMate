package com.example.exmate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etExpenseAmount, etExpenseDate, etExpenseNote;
    private Spinner spExpenseCategory, spExpensePaymentMode, spExpenseType;
    private Button btnSaveExpense;

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

        btnSaveExpense.setOnClickListener(v -> validateAndSave());
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "Food",
                "Transport",
                "Shopping",
                "Bills",
                "Entertainment",
                "Health",
                "Education",
                "Other"
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
                "Cash",
                "UPI",
                "Bank Transfer",
                "Card",
                "Wallet"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                modes
        );
        spExpensePaymentMode.setAdapter(adapter);
    }

    private void setupExpenseTypeSpinner() {
        String[] types = {
                "Personal",
                "Business"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                types
        );
        spExpenseType.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etExpenseDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int year  = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day   = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        String date = d + "/" + (m + 1) + "/" + y;
                        etExpenseDate.setText(date);
                    },
                    year, month, day
            );
            dialog.show();
        });
    }

    private void validateAndSave() {
        String amount = etExpenseAmount.getText().toString().trim();
        String date   = etExpenseDate.getText().toString().trim();

        if (amount.isEmpty()) {
            etExpenseAmount.setError("Enter amount");
            return;
        }

        try {
            double value = Double.parseDouble(amount);
            if (value <= 0) {
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

        // 🔒 Firebase logic will be added later
        Toast.makeText(this, "Expense saved (UI only)", Toast.LENGTH_SHORT).show();

        finish(); // back to dashboard
    }
}
