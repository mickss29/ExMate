package com.example.exmate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    // UI
    private ImageView imgAvatar;
    private View btnEditAvatar;

    private TextView tvUserName, tvUserEmail, tvFeedbackStatus;

    private View itemProfileInfo,
            itemAppLock,
            itemSubscription,
            itemAnalysis,     // ⭐ NEW
            itemAbout,
            itemTerms,
            itemPrivacy;

    private View btnLogout;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference userRef;

    // Gallery pick
    private static final int REQ_GALLERY = 101;

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
        btnEditAvatar    = view.findViewById(R.id.btnEditAvatar);

        tvUserName       = view.findViewById(R.id.tvUserName);
        tvUserEmail      = view.findViewById(R.id.tvUserEmail);
        tvFeedbackStatus = view.findViewById(R.id.tvFeedbackStatus);

        itemProfileInfo  = view.findViewById(R.id.itemProfileInfo);
        itemAppLock      = view.findViewById(R.id.itemAppLock);
        itemSubscription = view.findViewById(R.id.itemSubscription);
        itemAnalysis     = view.findViewById(R.id.itemAnalysis); // ⭐ NEW
        itemAbout        = view.findViewById(R.id.itemAbout);
        itemTerms        = view.findViewById(R.id.itemTerms);
        itemPrivacy      = view.findViewById(R.id.itemPrivacy);

        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupRows(View root) {

        bindRow(root, R.id.itemProfileInfo, R.drawable.ic_user, "Profile Info");
        bindRow(root, R.id.itemAppLock, R.drawable.ic_lock, "App Lock");
        bindRow(root, R.id.itemSubscription, R.drawable.ic_subscription, "Subscription");

        // ⭐ NEW ANALYSIS ROW
        bindRow(root, R.id.itemAnalysis, R.drawable.ic_stats, "Analyze Transactions");

        bindRow(root, R.id.itemAbout, R.drawable.ic_info, "About Us");
        bindRow(root, R.id.itemTerms, R.drawable.ic_terms, "Terms & Conditions");
        bindRow(root, R.id.itemPrivacy, R.drawable.ic_privacy, "Privacy Policy");
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

                String profileImage = snapshot.child("profileImage").getValue(String.class);

                if (!TextUtils.isEmpty(profileImage) && isAdded()) {
                    Glide.with(requireContext())
                            .load(profileImage)
                            .circleCrop()
                            .into(imgAvatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupClicks() {

        imgAvatar.setOnClickListener(v -> openGallery());

        if (btnEditAvatar != null) {
            btnEditAvatar.setOnClickListener(v -> openGallery());
        }

        itemProfileInfo.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        itemAppLock.setOnClickListener(v -> {
            if (AppLockManager.isEnabled(requireContext())) {
                Toast.makeText(getContext(),
                        "App Lock already enabled",
                        Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(requireContext(), SetPinActivity.class));
            }
        });

        itemSubscription.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SubscriptionActivity.class)));

        // ⭐ OPEN TRANSACTION ANALYSIS SCREEN
        itemAnalysis.setOnClickListener(v -> {

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new TransactionAnalysisFragment())
                    .addToBackStack(null)
                    .commit();
        });

        itemAbout.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutActivity.class)));

        itemTerms.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TermsActivity.class)));

        itemPrivacy.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PrivacyActivity.class)));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void openGallery() {

        if (userRef == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_GALLERY && resultCode == Activity.RESULT_OK && data != null) {

            Uri uri = data.getData();
            if (uri == null) return;

            if (isAdded()) {
                Glide.with(requireContext())
                        .load(uri)
                        .circleCrop()
                        .into(imgAvatar);
            }

            userRef.child("profileImage").setValue(uri.toString());
        }
    }

    private void logoutUser() {
        auth.signOut();
        Intent i = new Intent(requireActivity(), AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }
}