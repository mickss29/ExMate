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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // UI
    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvFeedbackStatus;

    private View itemProfileInfo,
            itemAppLock, itemRateApp, itemSubscription, itemInvite,
            itemAbout, itemTerms, itemPrivacy,
            itemFinancialSupport, itemWhatsNew;

    private View btnLogout;

    // Firebase
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
        setupRows(view);
        loadUserData();
        setupClicks();

        return view;
    }

    private void initViews(View view) {
        imgAvatar        = view.findViewById(R.id.imgAvatar);
        tvUserName       = view.findViewById(R.id.tvUserName);
        tvUserEmail      = view.findViewById(R.id.tvUserEmail);
        tvFeedbackStatus = view.findViewById(R.id.tvFeedbackStatus);

        itemProfileInfo      = view.findViewById(R.id.itemProfileInfo);
        itemAppLock          = view.findViewById(R.id.itemAppLock);
        itemRateApp          = view.findViewById(R.id.itemRateApp);
        itemSubscription     = view.findViewById(R.id.itemSubscription);
        itemInvite           = view.findViewById(R.id.itemInvite);
        itemAbout            = view.findViewById(R.id.itemAbout);
        itemTerms            = view.findViewById(R.id.itemTerms);
        itemPrivacy          = view.findViewById(R.id.itemPrivacy);
        itemFinancialSupport = view.findViewById(R.id.itemFinancialSupport);
        itemWhatsNew         = view.findViewById(R.id.itemWhatsNew);

        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupRows(View root) {
        bindRow(root, R.id.itemProfileInfo, R.drawable.ic_user, "Profile Info");
        bindRow(root, R.id.itemAppLock, R.drawable.ic_lock, "App Lock");
        bindRow(root, R.id.itemRateApp, R.drawable.ic_star, "Rate App");
        bindRow(root, R.id.itemSubscription, R.drawable.ic_subscription, "Subscription");
        bindRow(root, R.id.itemInvite, R.drawable.ic_invite, "Invite Friend & Family");
        bindRow(root, R.id.itemAbout, R.drawable.ic_info, "About Us");
        bindRow(root, R.id.itemTerms, R.drawable.ic_terms, "Terms & Conditions");
        bindRow(root, R.id.itemPrivacy, R.drawable.ic_privacy, "Privacy Policy");
        bindRow(root, R.id.itemFinancialSupport, R.drawable.ic_support, "Financial Support");
        bindRow(root, R.id.itemWhatsNew, R.drawable.ic_new, "What's New");
    }

    private void bindRow(View root, int rowId, int icon, String title) {
        View row = root.findViewById(rowId);
        if (row == null) return;

        ImageView img = row.findViewById(R.id.imgIcon);
        TextView tv = row.findViewById(R.id.tvTitle);

        if (img != null) img.setImageResource(icon);
        if (tv != null) tv.setText(title);
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) return;

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        tvUserEmail.setText(user.getEmail());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                tvUserName.setText(TextUtils.isEmpty(name) ? "User" : name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupClicks() {
        itemProfileInfo.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));


        itemRateApp.setOnClickListener(v ->
                toast("Play Store redirect coming"));

        itemAppLock.setOnClickListener(v -> {
            if (AppLockManager.isEnabled(requireContext())) {
                toast("App Lock already enabled");
            } else {
                startActivity(new Intent(requireContext(), SetPinActivity.class));
            }
        });



        itemFinancialSupport.setOnClickListener(v ->
                showFeedbackDialog());

        itemWhatsNew.setOnClickListener(v ->
                toast("What's new coming"));

        itemSubscription.setOnClickListener(v ->
                toast("Premium plans coming"));

        itemInvite.setOnClickListener(v ->
                toast("Invite feature coming"));

        itemAbout.setOnClickListener(v ->
                toast("About page coming"));

        itemTerms.setOnClickListener(v ->
                toast("Terms page coming"));

        itemPrivacy.setOnClickListener(v ->
                toast("Privacy policy coming"));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        auth.signOut();
        Intent i = new Intent(requireActivity(), AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    // Feedback dialog
    private void showFeedbackDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Write your feedback...");
        input.setMinLines(3);
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Send Feedback")
                .setView(input)
                .setPositiveButton("Send", (d, w) ->
                        sendFeedback(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendFeedback(String msg) {
        if (TextUtils.isEmpty(msg)) {
            toast("Feedback empty");
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
                        tvFeedbackStatus.setText("✔ Feedback sent"))
                .addOnFailureListener(e ->
                        tvFeedbackStatus.setText("❌ Failed"));
    }
}
