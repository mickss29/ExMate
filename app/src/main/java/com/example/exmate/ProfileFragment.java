package com.example.exmate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // ================= UI =================
    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvUserPhone, tvFeedbackStatus;
    private MaterialButton btnEditProfile, btnSendFeedback, btnLogout;

    // ================= FIREBASE =================
    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        initViews(view);
        setupFirebase();
        loadUserData();
        setupClicks();

        return view;
    }

    // ================= INIT =================
    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvFeedbackStatus = view.findViewById(R.id.tvFeedbackStatus);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSendFeedback = view.findViewById(R.id.btnSendFeedback);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    // ================= FIREBASE =================
    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
    }

    // ================= LOAD USER DATA =================
    private void loadUserData() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Email from FirebaseAuth
        tvUserEmail.setText(user.getEmail());

        // Name & Phone from Database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                tvUserName.setText(
                        name == null || name.isEmpty() ? "User" : name
                );

                tvUserPhone.setText(
                        phone == null || phone.isEmpty() ? "" : phone
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= BUTTON CLICKS =================
    private void setupClicks() {

        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(
                        getContext(),
                        "Edit profile coming soon",
                        Toast.LENGTH_SHORT
                ).show()
        );

        btnSendFeedback.setOnClickListener(v -> showFeedbackDialog());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();

            Intent i = new Intent(requireActivity(), AuthActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finish();
        });
    }

    // ================= FEEDBACK =================
    private void showFeedbackDialog() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());

        builder.setTitle("Send Feedback");

        final EditText input = new EditText(requireContext());
        input.setHint("Write your feedback here...");
        input.setMinLines(3);
        input.setPadding(32, 32, 32, 32);

        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {

            String message = input.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(
                        getContext(),
                        "Feedback cannot be empty",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) return;

            DatabaseReference feedbackRef =
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("feedback");

            String key = feedbackRef.push().getKey();

            Map<String, Object> map = new HashMap<>();
            map.put("message", message);
            map.put("time", System.currentTimeMillis());

            feedbackRef.child(key).setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        tvFeedbackStatus.setText("✅ Feedback sent successfully");
                    })
                    .addOnFailureListener(e -> {
                        tvFeedbackStatus.setText("❌ Failed to send feedback");
                    });
        });

        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        builder.show();
    }
}
