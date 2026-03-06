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

/**
 * UserProfile Activity
 *
 * BUGS FIXED:
 * 1. Removed tvUserPhone findViewById crash (R.id.tvUserPhone didn't exist in XML)
 * 2. Added feedbackListener field + removeEventListener in onDestroy() to prevent memory leak
 * 3. Added null-check on userId before any Firebase call
 * 4. submitFeedback: added null-check on push key
 */
public class UserProfile extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvFeedbackStatus;
    private MaterialButton btnEditProfile, btnLogout, btnSendFeedback;

    private DatabaseReference userRef, feedbackRef;
    private ValueEventListener feedbackListener;   // ← FIX #2: hold reference to remove later
    private String userId, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // FIX #1: Removed tvUserPhone — it doesn't exist in activity_user_profile.xml
        tvUserName       = findViewById(R.id.tvUserName);
        tvUserEmail      = findViewById(R.id.tvUserEmail);
        tvFeedbackStatus = findViewById(R.id.tvFeedbackStatus);

        btnEditProfile  = findViewById(R.id.btnEditProfile);
        btnLogout       = findViewById(R.id.btnLogout);
        btnSendFeedback = findViewById(R.id.btnSendFeedback);

        // FIX #3: null-check userId before proceeding
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finishAffinity();
            return;
        }

        userRef     = FirebaseDatabase.getInstance().getReference("users").child(userId);
        feedbackRef = FirebaseDatabase.getInstance().getReference("feedbacks");

        loadProfile();
        listenLatestFeedback();

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnSendFeedback.setOnClickListener(v -> showFeedbackDialog());
    }

    // ────────────────────────────────────────────────────
    //  LOAD PROFILE
    // ────────────────────────────────────────────────────
    private void loadProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userName = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                if (tvUserName  != null) tvUserName.setText(TextUtils.isEmpty(userName) ? "User" : userName);
                if (tvUserEmail != null) tvUserEmail.setText(TextUtils.isEmpty(email) ? "" : email);
                // NOTE: phone field removed — no matching view in this layout
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserProfile.this,
                        "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ────────────────────────────────────────────────────
    //  FEEDBACK DIALOG
    // ────────────────────────────────────────────────────
    private void showFeedbackDialog() {
        EditText input = new EditText(this);
        input.setHint("Write your problem or question");
        input.setPadding(48, 32, 48, 32);
        input.setMinLines(3);
        input.setMaxLines(6);

        new AlertDialog.Builder(this)
                .setTitle("Send Feedback 💬")
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

    // ────────────────────────────────────────────────────
    //  SUBMIT FEEDBACK
    // ────────────────────────────────────────────────────
    private void submitFeedback(String message) {
        // FIX #4: null-check on push key
        String id = feedbackRef.push().getKey();
        if (id == null) {
            Toast.makeText(this, "Failed to send feedback. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("uid",       userId);
        map.put("userName",  userName != null ? userName : "User");
        map.put("message",   message);
        map.put("reply",     "");
        map.put("status",    "pending");
        map.put("timestamp", System.currentTimeMillis());

        feedbackRef.child(id).setValue(map)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Feedback sent! ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ────────────────────────────────────────────────────
    //  FEEDBACK STATUS LISTENER
    // ────────────────────────────────────────────────────
    private void listenLatestFeedback() {
        // FIX #2: Store listener reference so we can remove it in onDestroy
        feedbackListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (tvFeedbackStatus == null) return;
                boolean hasData = false;
                for (DataSnapshot s : snapshot.getChildren()) {
                    hasData = true;
                    String status = s.child("status").getValue(String.class);
                    String reply  = s.child("reply").getValue(String.class);

                    if ("solved".equals(status) && !TextUtils.isEmpty(reply)) {
                        tvFeedbackStatus.setText("✅ Solved\nReply: " + reply);
                    } else {
                        tvFeedbackStatus.setText("⏳ Pending – Admin will reply soon");
                    }
                }
                if (!hasData) {
                    tvFeedbackStatus.setText("No feedback sent yet");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        feedbackRef.orderByChild("uid")
                .equalTo(userId)
                .limitToLast(1)
                .addValueEventListener(feedbackListener);
    }

    // ────────────────────────────────────────────────────
    //  LIFECYCLE — FIX #2: Remove listener to prevent memory leak
    // ────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedbackRef != null && feedbackListener != null) {
            feedbackRef.orderByChild("uid")
                    .equalTo(userId)
                    .limitToLast(1)
                    .removeEventListener(feedbackListener);
        }
    }
}