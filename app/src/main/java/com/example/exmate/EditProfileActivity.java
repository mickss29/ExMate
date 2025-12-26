package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail;
    private Button btnUpdate;

    private FirebaseUser user;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnUpdate = findViewById(R.id.btnUpdateProfile);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        loadUserData();

        etEmail.setText(user.getEmail());

        btnUpdate.setOnClickListener(v -> confirmPassword());
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                etName.setText(snapshot.child("name").getValue(String.class));
                etPhone.setText(snapshot.child("phone").getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // 🔐 Password confirmation dialog
    private void confirmPassword() {
        EditText etPassword = new EditText(this);
        etPassword.setHint("Enter your password");

        new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setView(etPassword)
                .setPositiveButton("Confirm", (d, w) ->
                        reAuthenticate(etPassword.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 🔐 Re-authentication
    private void reAuthenticate(String password) {
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(a -> updateProfile())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show());
    }

    // ✅ Update profile after verification
    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("phone", phone);

        userRef.updateChildren(map)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
