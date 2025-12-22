package com.example.exmate;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.HashMap;

public class AdminFeedbackDetailActivity extends AppCompatActivity {

    private TextView tvUserName, tvMessage, tvStatus;
    private EditText etReply;
    private MaterialButton btnSolve;

    private DatabaseReference feedbackRef;
    private String feedbackId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_feedback_detail);

        tvUserName = findViewById(R.id.tvUserName);
        tvMessage = findViewById(R.id.tvMessage);
        tvStatus = findViewById(R.id.tvStatus);
        etReply = findViewById(R.id.etReply);
        btnSolve = findViewById(R.id.btnSolve);

        feedbackId = getIntent().getStringExtra("feedbackId");

        if (feedbackId == null) {
            finish();
            return;
        }

        feedbackRef = FirebaseDatabase.getInstance()
                .getReference("feedbacks")
                .child(feedbackId);

        loadFeedback();

        btnSolve.setOnClickListener(v -> submitReply());
    }

    private void loadFeedback() {
        feedbackRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {

                tvUserName.setText(s.child("userName").getValue(String.class));
                tvMessage.setText(s.child("message").getValue(String.class));
                tvStatus.setText("Status: " + s.child("status").getValue(String.class));

                String reply = s.child("reply").getValue(String.class);
                if (reply != null) {
                    etReply.setText(reply);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        AdminFeedbackDetailActivity.this,
                        "Failed to load feedback",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void submitReply() {
        String reply = etReply.getText().toString().trim();

        if (TextUtils.isEmpty(reply)) {
            etReply.setError("Reply required");
            return;
        }

        HashMap<String, Object> update = new HashMap<>();
        update.put("reply", reply);
        update.put("status", "solved");

        feedbackRef.updateChildren(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(
                            this,
                            "Reply sent & marked as solved",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
