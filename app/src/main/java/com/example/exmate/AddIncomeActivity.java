package com.example.exmate;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class AddIncomeActivity extends AppCompatActivity {

    private EditText etAmount, etNote;
    private Spinner spCategory;
    private Button btnSave;

    private DatabaseReference usersRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        spCategory = findViewById(R.id.spCategory);
        btnSave = findViewById(R.id.btnSaveIncome);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        btnSave.setOnClickListener(v -> saveIncome());
    }

    private void saveIncome() {

        String amountStr = etAmount.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        String id = usersRef.child(uid).child("income").push().getKey();

        HashMap<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        map.put("category", category);
        map.put("note", note);
        map.put("date", System.currentTimeMillis());

        usersRef.child(uid).child("income").child(id).setValue(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Income added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
