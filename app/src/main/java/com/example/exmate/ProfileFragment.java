package com.example.exmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * ProfileFragment
 *
 * BUGS FIXED:
 * 1. Profile image: was saving local content:// URI to Firebase (breaks on relaunch/other devices).
 *    Now saves uri.toString() for local session display only — shows a Toast explaining real upload
 *    needs Firebase Storage (easy to wire up when you add Storage to your project).
 *
 * 2. loadStats(): was fetching entire user node just to read incomes/expenses.
 *    Now uses targeted listeners on "users/{uid}/incomes" and "users/{uid}/expenses" separately.
 *
 * 3. feedbackListener: was never removed → memory leak + potential NPE after fragment detach.
 *    Now stored as field and removed in onDestroyView().
 *
 * 4. setupInitialsAvatar(): parent cast was unsafe (could throw ClassCastException on some layouts).
 *    Added instanceof check.
 *
 * 5. onActivityResult deprecated API — kept as-is for Java compatibility but added note.
 */
public class ProfileFragment extends Fragment {

    // ── Views ──
    private ImageView imgAvatar;
    private View btnEditAvatar;
    private TextView tvAvatarInitials, tvUserName, tvUserEmail;
    private TextView tvMemberSince, tvAppLockStatus, tvFeedbackStatus;
    private TextView tvStatIncome, tvStatExpense, tvStatBalance;

    private View itemProfileInfo, itemAppLock, itemSubscription;
    private View itemAnalysis, itemFeedback;
    private View itemAbout, itemTerms, itemPrivacy;
    private View btnLogout;

    // ── Firebase ──
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private String userId;
    private String cachedUserName = "User";

    // FIX #3: Hold reference to remove in onDestroyView
    private ValueEventListener feedbackListener;
    private DatabaseReference  feedbackQueryRef;

    // ── Formatting ──
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    // ── Avatar color palette ──
    private static final int[] AVATAR_COLORS = {
            0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899, 0xFFEF4444,
            0xFFF97316, 0xFFEAB308, 0xFF22C55E, 0xFF14B8A6,
            0xFF3B82F6, 0xFF06B6D4, 0xFFA855F7, 0xFFF43F5E
    };

    private static final int REQ_GALLERY = 101;

    // ════════════════════════════════════════════════════
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);
        initViews(view);
        setupFirebase();
        loadUserData();
        loadStats();
        setupClicks();
        return view;
    }

    // ════════════════════════════════════════════════════
    //  INIT VIEWS
    // ════════════════════════════════════════════════════
    private void initViews(View v) {
        imgAvatar        = v.findViewById(R.id.imgAvatar);
        btnEditAvatar    = v.findViewById(R.id.btnEditAvatar);
        tvAvatarInitials = v.findViewById(R.id.tvAvatarInitials);
        tvUserName       = v.findViewById(R.id.tvUserName);
        tvUserEmail      = v.findViewById(R.id.tvUserEmail);
        tvMemberSince    = v.findViewById(R.id.tvMemberSince);
        tvAppLockStatus  = v.findViewById(R.id.tvAppLockStatus);
        tvFeedbackStatus = v.findViewById(R.id.tvFeedbackStatus);
        tvStatIncome     = v.findViewById(R.id.tvStatIncome);
        tvStatExpense    = v.findViewById(R.id.tvStatExpense);
        tvStatBalance    = v.findViewById(R.id.tvStatBalance);

        itemProfileInfo  = v.findViewById(R.id.itemProfileInfo);
        itemAppLock      = v.findViewById(R.id.itemAppLock);
        itemSubscription = v.findViewById(R.id.itemSubscription);
        itemAnalysis     = v.findViewById(R.id.itemAnalysis);
        itemFeedback     = v.findViewById(R.id.itemFeedback);
        itemAbout        = v.findViewById(R.id.itemAbout);
        itemTerms        = v.findViewById(R.id.itemTerms);
        itemPrivacy      = v.findViewById(R.id.itemPrivacy);
        btnLogout        = v.findViewById(R.id.btnLogout);
    }

    // ════════════════════════════════════════════════════
    //  FIREBASE SETUP
    // ════════════════════════════════════════════════════
    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        userId  = user.getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);
    }

    // ════════════════════════════════════════════════════
    //  LOAD USER DATA
    // ════════════════════════════════════════════════════
    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        tvUserEmail.setText(user.getEmail());

        if (tvAppLockStatus != null) {
            boolean locked = AppLockManager.isEnabled(requireContext());
            tvAppLockStatus.setText(locked ? "Enabled ✓" : "Disabled");
            tvAppLockStatus.setTextColor(locked
                    ? Color.parseColor("#34D399")
                    : Color.parseColor("#4B6280"));
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String name = snapshot.child("name").getValue(String.class);
                if (TextUtils.isEmpty(name)) name = "User";
                cachedUserName = name;
                tvUserName.setText(name);

                setupInitialsAvatar(name);

                Long createdAt = snapshot.child("createdAtMillis").getValue(Long.class);
                if (createdAt != null && tvMemberSince != null) {
                    String since = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            .format(new Date(createdAt));
                    tvMemberSince.setText("Member since " + since);
                }

                String imgUrl = snapshot.child("profileImage").getValue(String.class);
                // FIX #1: Only load URLs that start with "https://" (Firebase Storage URLs)
                // Local content:// URIs are NOT stored — they break on relaunch
                if (!TextUtils.isEmpty(imgUrl) && imgUrl.startsWith("https://") && isAdded()) {
                    Glide.with(requireContext())
                            .load(imgUrl)
                            .circleCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(imgAvatar);
                    imgAvatar.setVisibility(View.VISIBLE);
                    tvAvatarInitials.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        listenFeedbackStatus();
    }

    // ── Initials + dynamic background color ──
    // FIX #4: Added instanceof check before casting parent
    private void setupInitialsAvatar(String name) {
        if (TextUtils.isEmpty(name)) return;

        String initials = String.valueOf(name.charAt(0)).toUpperCase();
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1 && !TextUtils.isEmpty(parts[1])) {
            initials += String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        if (tvAvatarInitials != null) tvAvatarInitials.setText(initials);

        int idx = Math.abs(Character.toUpperCase(name.charAt(0)) - 'A')
                % AVATAR_COLORS.length;

        // FIX #4: Safe parent cast
        if (tvAvatarInitials != null && tvAvatarInitials.getParent() instanceof View) {
            View parent = (View) tvAvatarInitials.getParent();
            parent.setBackgroundColor(AVATAR_COLORS[idx]);
        }
    }

    // ════════════════════════════════════════════════════
    //  STATS — FIX #2: Use targeted sub-paths, not full user node
    // ════════════════════════════════════════════════════
    private void loadStats() {
        if (userId == null) return;

        DatabaseReference incomeRef  = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("incomes");
        DatabaseReference expenseRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("expenses");

        // Fetch incomes
        incomeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                double income = 0;
                for (DataSnapshot s : snapshot.getChildren()) {
                    Double a = s.child("amount").getValue(Double.class);
                    if (a != null) income += a;
                }
                final double finalIncome = income;

                // Now fetch expenses
                expenseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        double expense = 0;
                        for (DataSnapshot s : snap.getChildren()) {
                            Double a = s.child("amount").getValue(Double.class);
                            if (a != null) expense += a;
                        }
                        double balance = finalIncome - expense;

                        if (tvStatIncome  != null) tvStatIncome.setText("₹"  + fmt.format(finalIncome));
                        if (tvStatExpense != null) tvStatExpense.setText("₹" + fmt.format(expense));
                        if (tvStatBalance != null) {
                            tvStatBalance.setText("₹" + fmt.format(balance));
                            tvStatBalance.setTextColor(balance >= 0
                                    ? Color.parseColor("#34D399")
                                    : Color.parseColor("#F87171"));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ════════════════════════════════════════════════════
    //  FEEDBACK STATUS LISTENER — FIX #3: Store ref to remove later
    // ════════════════════════════════════════════════════
    private void listenFeedbackStatus() {
        if (userId == null || tvFeedbackStatus == null) return;

        feedbackQueryRef = FirebaseDatabase.getInstance().getReference("feedbacks");

        feedbackListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || tvFeedbackStatus == null) return;
                boolean hasAny = false;
                for (DataSnapshot s : snapshot.getChildren()) {
                    hasAny = true;
                    String status = s.child("status").getValue(String.class);
                    String reply  = s.child("reply").getValue(String.class);
                    if ("solved".equals(status) && !TextUtils.isEmpty(reply)) {
                        tvFeedbackStatus.setText("✅ Replied: " + reply);
                        tvFeedbackStatus.setTextColor(Color.parseColor("#34D399"));
                    } else {
                        tvFeedbackStatus.setText("⏳ Pending admin reply");
                        tvFeedbackStatus.setTextColor(Color.parseColor("#FBBF24"));
                    }
                }
                if (!hasAny && tvFeedbackStatus != null) {
                    tvFeedbackStatus.setText("No feedback sent yet");
                    tvFeedbackStatus.setTextColor(Color.parseColor("#4B6280"));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        feedbackQueryRef.orderByChild("uid")
                .equalTo(userId)
                .limitToLast(1)
                .addValueEventListener(feedbackListener);
    }

    // ════════════════════════════════════════════════════
    //  CLICK HANDLERS
    // ════════════════════════════════════════════════════
    private void setupClicks() {

        if (imgAvatar   != null) imgAvatar.setOnClickListener(v -> openGallery());
        if (btnEditAvatar != null) btnEditAvatar.setOnClickListener(v -> openGallery());

        if (itemProfileInfo != null)
            itemProfileInfo.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        if (itemAppLock != null)
            itemAppLock.setOnClickListener(v -> {
                if (AppLockManager.isEnabled(requireContext())) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("App Lock")
                            .setMessage("App Lock is currently enabled. Go to PIN settings?")
                            .setPositiveButton("Open Settings", (d, w) ->
                                    startActivity(new Intent(requireContext(), SetPinActivity.class)))
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    startActivity(new Intent(requireContext(), SetPinActivity.class));
                }
            });

        if (itemSubscription != null)
            itemSubscription.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SubscriptionActivity.class)));

        if (itemAnalysis != null)
            itemAnalysis.setOnClickListener(v ->
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new TransactionAnalysisFragment())
                            .addToBackStack(null)
                            .commit());

        if (itemFeedback != null)
            itemFeedback.setOnClickListener(v -> showFeedbackDialog());

        if (itemAbout   != null)
            itemAbout.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AboutActivity.class)));
        if (itemTerms   != null)
            itemTerms.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), TermsActivity.class)));
        if (itemPrivacy != null)
            itemPrivacy.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), PrivacyActivity.class)));

        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // ════════════════════════════════════════════════════
    //  DIALOGS
    // ════════════════════════════════════════════════════
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFeedbackDialog() {
        if (userId == null) return;

        EditText input = new EditText(requireContext());
        input.setHint("Describe your issue or suggestion…");
        input.setPadding(48, 32, 48, 32);
        input.setMinLines(3);
        input.setMaxLines(6);

        new AlertDialog.Builder(requireContext())
                .setTitle("Send Feedback 💬")
                .setView(input)
                .setPositiveButton("Submit", (d, w) -> {
                    String msg = input.getText().toString().trim();
                    if (msg.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Please write something", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitFeedback(msg);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitFeedback(String message) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("feedbacks");
        String id = ref.push().getKey();
        if (id == null) return;

        HashMap<String, Object> map = new HashMap<>();
        map.put("uid",       userId);
        map.put("userName",  cachedUserName);
        map.put("message",   message);
        map.put("reply",     "");
        map.put("status",    "pending");
        map.put("timestamp", System.currentTimeMillis());

        ref.child(id).setValue(map).addOnSuccessListener(v -> {
            if (!isAdded()) return;
            if (tvFeedbackStatus != null) {
                tvFeedbackStatus.setText("⏳ Pending admin reply");
                tvFeedbackStatus.setTextColor(Color.parseColor("#FBBF24"));
            }
            Toast.makeText(requireContext(), "Feedback sent! 🎉", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(),
                    "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // ════════════════════════════════════════════════════
    //  GALLERY — FIX #1: Don't persist local URI to Firebase
    // ════════════════════════════════════════════════════
    private void openGallery() {
        if (userRef == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_GALLERY
                && resultCode == Activity.RESULT_OK
                && data != null && isAdded()) {

            Uri uri = data.getData();
            if (uri == null) return;

            // Show image in UI for this session
            Glide.with(requireContext())
                    .load(uri)
                    .circleCrop()
                    .into(imgAvatar);
            imgAvatar.setVisibility(View.VISIBLE);
            if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);

            // FIX #1: DO NOT save local URI to Firebase — it only works on this device/session.
            // To persist properly, upload to Firebase Storage first, then save the download URL:
            //
            //   StorageReference storageRef = FirebaseStorage.getInstance()
            //       .getReference("profile_images/" + userId + ".jpg");
            //   storageRef.putFile(uri).continueWithTask(task -> storageRef.getDownloadUrl())
            //       .addOnSuccessListener(downloadUri ->
            //           userRef.child("profileImage").setValue(downloadUri.toString()));
            //
            // Add Firebase Storage to build.gradle:
            //   implementation 'com.google.firebase:firebase-storage'
            //
            // For now, image shows in session only (local preview).
            Toast.makeText(requireContext(),
                    "Photo updated for this session. Add Firebase Storage to persist it.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ════════════════════════════════════════════════════
    //  LOGOUT
    // ════════════════════════════════════════════════════
    private void performLogout() {
        auth.signOut();
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // ════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════
    @Override
    public void onResume() {
        super.onResume();
        loadStats();
        if (tvAppLockStatus != null) {
            boolean locked = AppLockManager.isEnabled(requireContext());
            tvAppLockStatus.setText(locked ? "Enabled ✓" : "Disabled");
            tvAppLockStatus.setTextColor(locked
                    ? Color.parseColor("#34D399")
                    : Color.parseColor("#4B6280"));
        }
    }

    // FIX #3: Remove Firebase listener to prevent memory leak
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedbackQueryRef != null && feedbackListener != null) {
            feedbackQueryRef.orderByChild("uid")
                    .equalTo(userId)
                    .limitToLast(1)
                    .removeEventListener(feedbackListener);
        }
    }
}