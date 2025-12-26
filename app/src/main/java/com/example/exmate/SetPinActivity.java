package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SetPinActivity extends AppCompatActivity {

    private EditText etPin, etConfirmPin;
    private Button btnSavePin;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_set_pin);

        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnSavePin = findViewById(R.id.btnSavePin);

        btnSavePin.setOnClickListener(v -> savePin());
    }

    private void savePin() {

        String pin = etPin.getText().toString().trim();
        String confirmPin = etConfirmPin.getText().toString().trim();

        if (pin.length() != 4 || !pin.equals(confirmPin)) {
            Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        String pinHash = HashUtil.sha256(pin); // 👈 HASH IT

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("appLock");

        ref.setValue(new HashMap<String, Object>() {{
            put("enabled", true);
            put("pinHash", pinHash);
        }}).addOnSuccessListener(unused -> {

            // Save locally
            AppLockManager.enable(SetPinActivity.this, pinHash);

            startActivity(new Intent(SetPinActivity.this, UserDashboardActivity.class));
            finish();
        });
    }

}
