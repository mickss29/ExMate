package com.example.exmate;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class AddIncomeActivity extends AppCompatActivity {

    EditText etAmount, etSource;
    Button btnSave;

    DatabaseReference incomeRef;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        etAmount = findViewById(R.id.etAmount);
        etSource = findViewById(R.id.etSource);
        btnSave = findViewById(R.id.btnSave);

        userId = FirebaseAuth.getInstance().getUid();

        incomeRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("incomes");

        btnSave.setOnClickListener(v -> {

            String amtStr = etAmount.getText().toString().trim();
            String source = etSource.getText().toString().trim();

            if (amtStr.isEmpty() || source.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount = Integer.parseInt(amtStr);

            String id = incomeRef.push().getKey();

            HashMap<String, Object> map = new HashMap<>();
            map.put("amount", amount);
            map.put("source", source);
            map.put("time", System.currentTimeMillis());

            incomeRef.child(id).setValue(map)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Income Added", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
