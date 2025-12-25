package com.example.exmate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.android.material.card.MaterialCardView;
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

    // ---------- UI ----------
    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvFeedbackStatus;

    private MaterialCardView itemProfileInfo,
            itemAppLock, itemRateApp, itemSubscription, itemInvite,
            itemAbout, itemTerms, itemPrivacy,
            itemFinancialSupport, itemWhatsNew, itemLogout;

    // ---------- FIREBASE ----------
    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        initViews(view);
        setupFirebase();
        loadUserData();
        setupClicks();

        return view;
    }

    // ---------- INIT UI ----------
    private void initViews(View view) {
        imgAvatar        = view.findViewById(R.id.imgAvatar);
        tvUserName       = view.findViewById(R.id.tvUserName);
        tvUserEmail      = view.findViewById(R.id.tvUserEmail);
        tvFeedbackStatus = view.findViewById(R.id.tvFeedbackStatus);

        itemProfileInfo      = view.findViewById(R.id.rowProfileInfo);
        itemAppLock          = view.findViewById(R.id.rowAppLock);
        itemRateApp          = view.findViewById(R.id.rowRateApp);
        itemSubscription     = view.findViewById(R.id.rowSubscription);
        itemInvite           = view.findViewById(R.id.rowInvite);
        itemAbout            = view.findViewById(R.id.rowAbout);
        itemTerms            = view.findViewById(R.id.rowTerms);
        itemPrivacy          = view.findViewById(R.id.rowPrivacy);
        itemFinancialSupport = view.findViewById(R.id.rowFinancialSupport);
        itemWhatsNew         = view.findViewById(R.id.rowWhatsNew);
        itemLogout           = view.findViewById(R.id.rowLogout);
    }

    // ---------- FIREBASE SETUP ----------
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

    // ---------- LOAD USER DATA ----------
    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        tvUserEmail.setText(user.getEmail());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue(String.class);
                tvUserName.setText(!TextUtils.isEmpty(name) ? name : "User");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ---------- CLICK ACTIONS ----------
    private void setupClicks() {

        itemProfileInfo.setOnClickListener(v ->
                Toast.makeText(getContext(), "Profile editing soon ✨", Toast.LENGTH_SHORT).show()
        );

        itemRateApp.setOnClickListener(v ->
                Toast.makeText(getContext(), "Redirect to PlayStore coming soon", Toast.LENGTH_SHORT).show()
        );

        itemAppLock.setOnClickListener(v ->
                Toast.makeText(getContext(), "App Lock feature coming", Toast.LENGTH_SHORT).show()
        );

        itemFinancialSupport.setOnClickListener(v ->
                Toast.makeText(getContext(), "Support screen soon", Toast.LENGTH_SHORT).show()
        );

        itemWhatsNew.setOnClickListener(v ->
                Toast.makeText(getContext(), "What's new page soon", Toast.LENGTH_SHORT).show()
        );

        itemSubscription.setOnClickListener(v ->
                Toast.makeText(getContext(), "Premium plans coming ⚡", Toast.LENGTH_SHORT).show()
        );

        itemInvite.setOnClickListener(v ->
                Toast.makeText(getContext(), "Invite feature coming 🎉", Toast.LENGTH_SHORT).show()
        );

        itemAbout.setOnClickListener(v ->
                Toast.makeText(getContext(), "About page coming", Toast.LENGTH_SHORT).show()
        );

        itemTerms.setOnClickListener(v ->
                Toast.makeText(getContext(), "Terms page soon", Toast.LENGTH_SHORT).show()
        );

        itemPrivacy.setOnClickListener(v ->
                Toast.makeText(getContext(), "Privacy Policy soon", Toast.LENGTH_SHORT).show()
        );

        itemLogout.setOnClickListener(v -> logoutUser());
    }

    // ---------- LOGOUT ----------
    private void logoutUser() {
        auth.signOut();
        Intent i = new Intent(requireActivity(), AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    // ---------- FEEDBACK ----------
    public void showFeedbackDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Write your feedback...");
        input.setMinLines(3);
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Send Feedback")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) ->
                        sendFeedback(input.getText().toString().trim()))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendFeedback(String msg) {
        if (TextUtils.isEmpty(msg)) {
            Toast.makeText(getContext(), "Feedback empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("feedback")
                .push();

        Map<String, Object> map = new HashMap<>();
        map.put("message", msg);
        map.put("time", System.currentTimeMillis());

        ref.setValue(map)
                .addOnSuccessListener(a ->
                        tvFeedbackStatus.setText("✔ Sent"))
                .addOnFailureListener(e ->
                        tvFeedbackStatus.setText("❌ Failed"));
    }
}
