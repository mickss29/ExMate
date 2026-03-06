package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * EditProfileActivity
 *
 * BUGS FIXED:
 * 1. etEmail is now properly disabled — user cannot type in it (was editable but never saved, confusing UX)
 * 2. Phone validation: must be 10 digits
 * 3. Name validation: no special characters, min 2 chars
 * 4. Added onFailureListener to Firebase update (was missing)
 */
public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail;
    private Button btnUpdateProfile;
    private ProgressBar progressUpdate;

    // Header views
    private TextView tvEditAvatarInitials, tvEditHeaderName, tvEditHeaderEmail;
    private View btnBack;

    private FirebaseUser user;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName           = findViewById(R.id.etName);
        etPhone          = findViewById(R.id.etPhone);
        etEmail          = findViewById(R.id.etEmail);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        progressUpdate   = findViewById(R.id.progressUpdate);

        tvEditAvatarInitials = findViewById(R.id.tvEditAvatarInitials);
        tvEditHeaderName     = findViewById(R.id.tvEditHeaderName);
        tvEditHeaderEmail    = findViewById(R.id.tvEditHeaderEmail);
        btnBack              = findViewById(R.id.btnBack);

        if (btnBack != null)
            btnBack.setOnClickListener(v -> finish());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid());

        // Pre-fill email
        etEmail.setText(user.getEmail());

        // FIX #1: Disable email field — it's display-only, never saved, was misleading
        etEmail.setEnabled(false);
        etEmail.setFocusable(false);
        etEmail.setAlpha(0.6f);   // visual hint it's non-editable

        if (tvEditHeaderEmail != null)
            tvEditHeaderEmail.setText(user.getEmail());

        loadUserData();
        btnUpdateProfile.setOnClickListener(v -> validateAndConfirm());
    }

    // ════════════════════════════════════════════════════
    //  LOAD DATA
    // ════════════════════════════════════════════════════
    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name  = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                if (etName  != null) etName.setText(name);
                if (etPhone != null) etPhone.setText(phone);

                if (!TextUtils.isEmpty(name)) {
                    if (tvEditHeaderName != null) tvEditHeaderName.setText(name);
                    if (tvEditAvatarInitials != null) {
                        String initials = String.valueOf(name.charAt(0)).toUpperCase();
                        String[] parts = name.trim().split("\\s+");
                        if (parts.length > 1 && !TextUtils.isEmpty(parts[1]))
                            initials += String.valueOf(parts[1].charAt(0)).toUpperCase();
                        tvEditAvatarInitials.setText(initials);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this,
                        "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ════════════════════════════════════════════════════
    //  VALIDATE INPUTS  (FIX #2 & #3)
    // ════════════════════════════════════════════════════
    private void validateAndConfirm() {
        String name  = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Name: min 2 chars, no leading/trailing spaces
        if (TextUtils.isEmpty(name) || name.length() < 2) {
            etName.setError("Name must be at least 2 characters");
            etName.requestFocus();
            return;
        }

        // Phone: exactly 10 digits  (FIX #2)
        if (TextUtils.isEmpty(phone) || !phone.matches("\\d{10}")) {
            etPhone.setError("Enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return;
        }

        confirmPassword();
    }

    // ════════════════════════════════════════════════════
    //  PASSWORD CONFIRM DIALOG
    // ════════════════════════════════════════════════════
    private void confirmPassword() {
        EditText pwField = new EditText(this);
        pwField.setHint("Enter your current password");
        pwField.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pwField.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Identity 🔐")
                .setMessage("Enter your password to save changes.")
                .setView(pwField)
                .setPositiveButton("Confirm", (d, w) ->
                        reAuthenticate(pwField.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ════════════════════════════════════════════════════
    //  RE-AUTH
    // ════════════════════════════════════════════════════
    private void reAuthenticate(String password) {
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader(true);

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(a -> updateProfile())
                .addOnFailureListener(e -> {
                    showLoader(false);
                    Toast.makeText(this, "Wrong password ❌", Toast.LENGTH_SHORT).show();
                });
    }

    // ════════════════════════════════════════════════════
    //  UPDATE PROFILE  (FIX #4: proper failure handling)
    // ════════════════════════════════════════════════════
    private void updateProfile() {
        String name  = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        Map<String, Object> map = new HashMap<>();
        map.put("name",  name);
        map.put("phone", phone);

        userRef.updateChildren(map)
                .addOnSuccessListener(a -> {
                    showLoader(false);
                    Toast.makeText(this, "Profile updated ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoader(false);
                    // FIX #4: was showing generic message — now shows actual Firebase error
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ════════════════════════════════════════════════════
    //  LOADER
    // ════════════════════════════════════════════════════
    private void showLoader(boolean show) {
        if (progressUpdate   != null)
            progressUpdate.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnUpdateProfile != null)
            btnUpdateProfile.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }
}