package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class UserProfile extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone;
    private MaterialButton btnEditProfile, btnLogout;

    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        userId = FirebaseAuth.getInstance().getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        loadProfile();

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finishAffinity();
        });
    }

    private void loadProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tvUserName.setText(snapshot.child("name").getValue(String.class));
                tvUserEmail.setText(snapshot.child("email").getValue(String.class));
                tvUserPhone.setText(snapshot.child("phone").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
