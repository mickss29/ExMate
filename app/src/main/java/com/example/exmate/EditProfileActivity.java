package com.example.exmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone;
    private Button btnUpdate;

    private FirebaseUser user;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        btnUpdate = findViewById(R.id.btnUpdate);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        loadCurrentData();

        btnUpdate.setOnClickListener(v -> showPasswordDialog());
    }

    // Load existing data
    private void loadCurrentData() {
        userRef.get().addOnSuccessListener(snapshot -> {
            etName.setText(snapshot.child("name").getValue(String.class));
            etPhone.setText(snapshot.child("phone").getValue(String.class));
        });
    }

    // Password confirmation dialog
    private void showPasswordDialog() {

        EditText etPassword = new EditText(this);
        etPassword.setHint("Enter your password");
        etPassword.setInputType(0x00000081); // textPassword

        new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setView(etPassword)
                .setPositiveButton("Confirm", (d, w) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this,
                                "Password required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reAuthenticate(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Firebase re-authentication
    private void reAuthenticate(String password) {

        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> updateProfile())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Wrong password",
                                Toast.LENGTH_LONG).show());
    }

    // Update profile after auth
    private void updateProfile() {

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this,
                    "Name and mobile cannot be empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.child("name").setValue(name);
        userRef.child("phone").setValue(phone);

        Toast.makeText(this,
                "Profile updated successfully",
                Toast.LENGTH_SHORT).show();

        finish();
    }
}
