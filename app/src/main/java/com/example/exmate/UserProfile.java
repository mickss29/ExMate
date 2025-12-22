package com.example.exmate;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class UserProfile extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvFeedbackStatus;
    private MaterialButton btnEditProfile, btnLogout, btnSendFeedback;

    private DatabaseReference userRef, feedbackRef;
    private String userId, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvFeedbackStatus = findViewById(R.id.tvFeedbackStatus);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnSendFeedback = findViewById(R.id.btnSendFeedback);

        userId = FirebaseAuth.getInstance().getUid();

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        feedbackRef = FirebaseDatabase.getInstance()
                .getReference("feedbacks");

        loadProfile();
        listenLatestFeedback();

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finishAffinity();
        });

        btnSendFeedback.setOnClickListener(v -> showFeedbackDialog());
    }

    private void loadProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userName = snapshot.child("name").getValue(String.class);
                tvUserName.setText(userName);
                tvUserEmail.setText(snapshot.child("email").getValue(String.class));
                tvUserPhone.setText(snapshot.child("phone").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void showFeedbackDialog() {
        EditText input = new EditText(this);
        input.setHint("Write your problem or question");

        new AlertDialog.Builder(this)
                .setTitle("Send Feedback")
                .setView(input)
                .setPositiveButton("Submit", (d, w) -> {
                    String msg = input.getText().toString().trim();
                    if (TextUtils.isEmpty(msg)) {
                        Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitFeedback(msg);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitFeedback(String message) {
        String id = feedbackRef.push().getKey();

        HashMap<String, Object> map = new HashMap<>();
        map.put("uid", userId);
        map.put("userName", userName);
        map.put("message", message);
        map.put("reply", "");
        map.put("status", "pending");
        map.put("timestamp", System.currentTimeMillis());

        feedbackRef.child(id).setValue(map)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Feedback sent successfully", Toast.LENGTH_SHORT).show());
    }

    private void listenLatestFeedback() {
        feedbackRef.orderByChild("uid")
                .equalTo(userId)
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String status = s.child("status").getValue(String.class);
                            String reply = s.child("reply").getValue(String.class);

                            if ("solved".equals(status)) {
                                tvFeedbackStatus.setText("✅ Solved\nReply: " + reply);
                            } else {
                                tvFeedbackStatus.setText("⏳ Pending – Admin will reply soon");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
}
