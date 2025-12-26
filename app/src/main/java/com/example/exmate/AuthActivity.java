package com.example.exmate;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        if (AppLockManager.isEnabled(this) && !AppLockManager.isUnlocked(this)) {
            startActivity(new Intent(this, AppLockActivity.class));
            finish();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        AppLockManager.markBackgroundTime(this);
    }


    // UI
    private LinearLayout layoutLogin, layoutSignup;
    private TextView tabLogin, tabSignup, tvForgotPassword;
    private ConstraintLayout rootLayout;
    private TextView tvSwipeHint;

    private static final String PREF_ONBOARDING = "auth_onboarding";
    private static final String KEY_SWIPE_HINT_SHOWN = "swipe_hint_shown";

    private View shapeCircle, shapeTriangle;
    private View toggleIndicator;
    private int toggleWidth = 0;
    private float swipeDownX;
    private static final int SWIPE_THRESHOLD = 120;
    private float swipeStartX;
    private long swipeStartTime;

    private static final int SWIPE_DISTANCE = 70;   // 🔥 small distance
    private static final int SWIPE_VELOCITY = 600;  // 🔥 fast flick





    private TextInputEditText etLoginEmail, etLoginPassword;
    private TextInputEditText etFullName, etSignupEmail,
            etSignupPassword, etSignupConfirmPassword, etSignupPhone;
    private Button btnLogin, btnSignup;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    // Loader
    private Dialog loader;

    // Drag + Haptic
    private float startX;
    private boolean hapticDone = false;
    private static final int DRAG_THRESHOLD = 180;

    private Vibrator vibrator;
    private boolean isLoginVisible = true;

    // ===== REGEX PATTERNS =====
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z ]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{10}$");

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile(
                    "^" +
                            "(?=.*[0-9])" +         // at least 1 digit
                            "(?=.*[a-z])" +         // at least 1 lower
                            "(?=.*[A-Z])" +         // at least 1 upper
                            "(?=.*[@#$%^&+=!])" +   // at least 1 special
                            "(?=\\S+$)" +           // no spaces
                            ".{8,}" +               // min 8 chars
                            "$"
            );
    private void autoLoginIfPossible() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        if (!user.isEmailVerified()) {
            auth.signOut();
            return;
        }

        // 🔐 FIRST CHECK → APP LOCK
        if (AppLockManager.isEnabled(this) && !AppLockManager.isUnlocked(this)) {
            startActivity(new Intent(this, AppLockActivity.class));
            finish();
            return;
        }

        // 🔓 ELSE → GO TO DASHBOARD
        usersRef.child(user.getUid()).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        loader.dismiss();

                        // 🔐 RESTORE APP LOCK AFTER CLEAR DATA
                        restoreAppLockFromServer();

                        String role = snapshot.getValue(String.class);

                        startActivity(new Intent(AuthActivity.this,
                                "admin".equals(role)
                                        ? AdminDashboardActivity.class
                                        : UserDashboardActivity.class));
                        finish();
                    }


                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

// ✅ AUTO LOGIN CHECK
        autoLoginIfPossible();


        // UI init
        rootLayout = findViewById(R.id.rootLayout);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);

        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        shapeCircle = findViewById(R.id.shapeCircle);
        shapeTriangle = findViewById(R.id.shapeTriangle);

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        etFullName = findViewById(R.id.etFullName);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        etSignupPhone = findViewById(R.id.etSignupPhone);

        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
        toggleIndicator = findViewById(R.id.toggleIndicator);
        tvSwipeHint = findViewById(R.id.tvSwipeHint);


// Set indicator width AFTER layout is drawn
        toggleIndicator.post(() -> {
            toggleWidth = ((View) toggleIndicator.getParent()).getWidth() / 2;

            // Apply width
            toggleIndicator.getLayoutParams().width = toggleWidth;
            toggleIndicator.requestLayout();

            // Default position (Login)
            toggleIndicator.setTranslationX(0);
        });


        setupLoader();
        setupDragWithHaptic();
        setupAuthCardSwipe();


        showLogin(false);
        animateDarkBlueBackground();
        startFloatingParallax();
        tabLogin.setOnClickListener(v -> {
            if (!isLoginVisible) {
                performToggleHaptic();
                animateAuthSwapSimple(false, () -> showLogin(false));
            }
        });

        tabSignup.setOnClickListener(v -> {
            if (isLoginVisible) {
                performToggleHaptic();
                animateAuthSwapSimple(true, () -> showSignup(false));
            }
        });



        btnSignup.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        SharedPreferences prefs =
                getSharedPreferences(PREF_ONBOARDING, MODE_PRIVATE);

        boolean shown = prefs.getBoolean(KEY_SWIPE_HINT_SHOWN, false);

        if (!shown) {
            layoutLogin.postDelayed(() -> {
                showSwipeOnboardingHint();

                prefs.edit()
                        .putBoolean(KEY_SWIPE_HINT_SHOWN, true)
                        .apply();
            }, 700);
        }

    }


    // ================= VALIDATED REGISTER =================
    private void registerUser() {

        String name = etFullName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String phone = etSignupPhone.getText().toString().trim();
        String pass = etSignupPassword.getText().toString().trim();
        String cpass = etSignupConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || pass.isEmpty() || cpass.isEmpty()) {
            toast("Fill all required fields");
            return;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            toast("Name must contain only alphabets");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter valid email address");
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            toast("Phone number must be 10 digits");
            return;
        }

        if (!STRONG_PASSWORD_PATTERN.matcher(pass).matches()) {
            toast("Password must contain uppercase, lowercase, number & special character");
            return;
        }

        if (!pass.equals(cpass)) {
            toast("Passwords do not match");
            return;
        }

        loader.show();

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        loader.dismiss();
                        toast(task.getException().getMessage());
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        loader.dismiss();
                        return;
                    }

                    user.sendEmailVerification();

                    String uid = user.getUid();

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("uid", uid);
                    map.put("name", name);
                    map.put("email", email);
                    map.put("phone", phone);
                    map.put("role", "user");

                    usersRef.child(uid).setValue(map).addOnCompleteListener(dbTask -> {
                        loader.dismiss();
                        auth.signOut();
                        toast("Verification email sent. Please verify.");
                        showLogin(true);
                    });
                });
    }

    // ================= LOGIN (UNCHANGED) =================
    private void loginUser() {

        String email = etLoginEmail.getText().toString().trim();
        String pass = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            toast("Enter email & password");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter valid email");
            return;
        }

        loader.show();

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        loader.dismiss();
                        toast(task.getException().getMessage());
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null || !user.isEmailVerified()) {
                        loader.dismiss();
                        auth.signOut();
                        toast("Email not verified");
                        return;
                    }

                    usersRef.child(user.getUid()).child("role")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    loader.dismiss();
                                    String role = snapshot.getValue(String.class);
                                    startActivity(new Intent(AuthActivity.this,
                                            "admin".equals(role)
                                                    ? AdminDashboardActivity.class
                                                    : UserDashboardActivity.class));
                                    finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    loader.dismiss();
                                    toast("Login failed");
                                }
                            });
                });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ================= UI / ANIMATION (UNCHANGED) =================
    private void setupDragWithHaptic() {
        View.OnTouchListener dragListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    hapticDone = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float diffX = event.getRawX() - startX;
                    v.setTranslationX(diffX);
                    v.setRotation(diffX / 35f);
                    if (!hapticDone && Math.abs(diffX) > DRAG_THRESHOLD) {
                        performHaptic();
                        hapticDone = true;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    resetPosition(v);
                    return true;
            }
            return false;
        };
        layoutLogin.setOnTouchListener(dragListener);
        layoutSignup.setOnTouchListener(dragListener);
    }

    private void performHaptic() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(20,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(20);
    }

    private void resetPosition(View view) {
        view.animate().translationX(0).rotation(0)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void showLogin(boolean animate) {
        isLoginVisible = true;

        layoutSignup.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.VISIBLE);

        tabLogin.setTextColor(0xFFFFFFFF);
        tabSignup.setTextColor(0xFFB0C4DE);

        if (animate) {
            toggleIndicator.animate()
                    .translationX(0)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            toggleIndicator.setTranslationX(0);
        }
    }

    private void showSignup(boolean animate) {
        isLoginVisible = false;

        layoutLogin.setVisibility(View.GONE);
        layoutSignup.setVisibility(View.VISIBLE);

        tabSignup.setTextColor(0xFFFFFFFF);
        tabLogin.setTextColor(0xFFB0C4DE);

        if (animate) {
            toggleIndicator.animate()
                    .translationX(toggleWidth)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            toggleIndicator.setTranslationX(toggleWidth);
        }
    }


    private void setupLoader() {

        loader = new Dialog(this);
        loader.setContentView(R.layout.dialog_loader);
        loader.setCancelable(false);

        // ✅ FIX 1: Window null safety
        if (loader.getWindow() != null) {
            loader.getWindow()
                    .setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ring = loader.findViewById(R.id.loaderRing);

        // ✅ FIX 2: View null safety
        if (ring != null) {
            ObjectAnimator rotate =
                    ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f);
            rotate.setDuration(1200);
            rotate.setRepeatCount(ValueAnimator.INFINITE);
            rotate.setInterpolator(null);
            rotate.start();
        }
    }


    private void animateDarkBlueBackground() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF001633, 0xFF00204D, 0xFF003366});
        rootLayout.setBackground(gd);
    }

    private void startFloatingParallax() {
        ObjectAnimator.ofFloat(shapeCircle, "translationY", 0f, 18f)
                .setDuration(6000).start();
        ObjectAnimator.ofFloat(shapeTriangle, "translationY", 0f, -14f)
                .setDuration(5000).start();
    }
    private void showForgotPasswordDialog() {

        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter registered email");

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {

                    String email = input.getText().toString().trim();

                    if (email.isEmpty()) {
                        Toast.makeText(this,
                                "Email required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this,
                                "Enter valid email",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Reset email sent",
                                            Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupAuthCardSwipe() {

        View.OnTouchListener swipeListener = (v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    swipeStartX = event.getRawX();
                    swipeStartTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_UP:
                    float endX = event.getRawX();
                    long endTime = System.currentTimeMillis();

                    float diffX = endX - swipeStartX;
                    long duration = endTime - swipeStartTime;

                    // velocity in px/sec
                    float velocity = Math.abs(diffX) / Math.max(duration, 1) * 1000;

                    // 👉 Swipe RIGHT → Signup
                    if ((diffX > SWIPE_DISTANCE || velocity > SWIPE_VELOCITY)
                            && isLoginVisible) {

                        animateAuthSwapPremium(() -> {
                            performToggleHaptic();
                            showSignup(true);
                        });
                        return true;
                    }

                    // 👉 Swipe LEFT → Login
                    if ((diffX < -SWIPE_DISTANCE || velocity > SWIPE_VELOCITY)
                            && !isLoginVisible) {

                        animateAuthSwapPremium(() -> {
                            performToggleHaptic();
                            showLogin(true);
                        });
                        return true;
                    }

                    return false;
            }
            return false;
        };

        layoutLogin.setOnTouchListener(swipeListener);
        layoutSignup.setOnTouchListener(swipeListener);
    }

    private void animateAuthSwap(boolean toSignup) {

        View target = isLoginVisible ? layoutLogin : layoutSignup;

        float direction = toSignup ? -1f : 1f;

        target.animate()
                .translationX(direction * 40)
                .alpha(0.95f)
                .setDuration(120)
                .withEndAction(() -> {
                    target.setTranslationX(0);
                    target.setAlpha(1f);
                })
                .start();
    }
    private void performToggleHaptic() {
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            14, // short premium tick
                            VibrationEffect.EFFECT_TICK
                    )
            );
        } else {
            vibrator.vibrate(14);
        }
    }
    private void animateAuthSwapPremium(Runnable onComplete) {

        View target = isLoginVisible ? layoutLogin : layoutSignup;

        target.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .alpha(0.88f)
                .setDuration(120)
                .withEndAction(() -> {

                    // reset instantly before next screen
                    target.setScaleX(1f);
                    target.setScaleY(1f);
                    target.setAlpha(1f);

                    if (onComplete != null) onComplete.run();
                })
                .start();
    }
    private void showSwipeOnboardingHint() {

        View target = isLoginVisible ? layoutLogin : layoutSignup;

        ObjectAnimator slideRight =
                ObjectAnimator.ofFloat(target, "translationX", 0f, 40f);
        slideRight.setDuration(250);

        ObjectAnimator slideLeft =
                ObjectAnimator.ofFloat(target, "translationX", 40f, -40f);
        slideLeft.setDuration(350);

        ObjectAnimator backToCenter =
                ObjectAnimator.ofFloat(target, "translationX", -40f, 0f);
        backToCenter.setDuration(250);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(slideRight, slideLeft, backToCenter);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
    private void showSwipeTextHint() {

        if (tvSwipeHint == null) return;

        tvSwipeHint.setVisibility(View.VISIBLE);

        tvSwipeHint.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> tvSwipeHint.postDelayed(() ->

                                tvSwipeHint.animate()
                                        .alpha(0f)
                                        .setDuration(400)
                                        .withEndAction(() ->
                                                tvSwipeHint.setVisibility(View.GONE))
                                        .start()

                        , 1500))
                .start();
    }
    private void animateAuthSwapSimple(boolean toSignup, Runnable onEnd) {

        View current = isLoginVisible ? layoutLogin : layoutSignup;
        View next = isLoginVisible ? layoutSignup : layoutLogin;

        float direction = toSignup ? -1f : 1f;

        next.setTranslationX(-direction * 60);
        next.setAlpha(0f);
        next.setVisibility(View.VISIBLE);

        current.animate()
                .translationX(direction * 60)
                .alpha(0f)
                .setDuration(180)
                .start();

        next.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(180)
                .withEndAction(() -> {
                    current.setTranslationX(0);
                    current.setAlpha(1f);
                    current.setVisibility(View.GONE);
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    private void restoreAppLockFromServer() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("appLock");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Boolean enabled = snapshot.child("enabled").getValue(Boolean.class);
                String pinHash = snapshot.child("pinHash").getValue(String.class);

                if (enabled != null && enabled && pinHash != null) {
                    AppLockManager.restoreFromServer(AuthActivity.this, pinHash);
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }



}
