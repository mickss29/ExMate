package com.example.exmate;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class AuthActivity extends AppCompatActivity {

    // ── Security ───────────────────────────────────────────────────────────
    private static final int  MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION      = 30_000L; // 30 s
    private int  failedAttempts = 0;
    private long lockUntil      = 0;

    // ── UI ─────────────────────────────────────────────────────────────────
    private ConstraintLayout rootLayout;
    private LinearLayout     layoutLogin, layoutSignup;
    private TextView         tabLogin, tabSignup, tvForgotPassword, tvAuthTitle, tvAuthSub;
    private View             toggleIndicator;
    private View             shapeCircle, shapeTriangle;   // bg blobs / glows
    private View             glowTop, glowBottom;          // new ambient glows
    private LinearLayout     swipeHintLogin, swipeHintSignup;

    // Password-strength bars (signup only)
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;

    // ── Input fields ───────────────────────────────────────────────────────
    private TextInputEditText etLoginEmail, etLoginPassword;
    private TextInputEditText etFullName, etSignupEmail,
            etSignupPassword, etSignupConfirmPassword;

    // ── Buttons ────────────────────────────────────────────────────────────
    private MaterialButton btnLogin, btnSignup;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth      auth;
    private DatabaseReference usersRef;

    // ── Misc ───────────────────────────────────────────────────────────────
    private Dialog   loader;
    private Vibrator vibrator;
    private boolean  isLoginVisible = true;
    private int      toggleWidth    = 0;

    // Swipe config
    private float swipeStartX;
    private long  swipeStartTime;
    private static final int  SWIPE_DISTANCE = 70;
    private static final int  SWIPE_VELOCITY = 600;

    // ── Validation patterns ────────────────────────────────────────────────
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z ]+$");
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])" +
                            "(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
            );

    // ══════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ① Premium edge-to-edge look
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        auth     = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseUser user = auth.getCurrentUser();

        // Already logged in → skip UI, go to dashboard
        if (user != null && user.isEmailVerified()) {
            if (AppLockManager.isEnabled(this) && !AppLockManager.isUnlocked(this)) {
                startActivity(new Intent(this, AppLockActivity.class));
                finish();
                return;
            }
            redirectToDashboard(user);
            return;
        }

        setContentView(R.layout.activity_auth);
        setupEdgeInsets();
        initUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing()) AppLockManager.markBackgroundTime(this);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════

    private void setupEdgeInsets() {
        View root = findViewById(R.id.rootLayout);
        if (root == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top    = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
    }

    private void initUI() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // ── Root & containers
        rootLayout    = findViewById(R.id.rootLayout);
        layoutLogin   = findViewById(R.id.layoutLogin);
        layoutSignup  = findViewById(R.id.layoutSignup);

        // ── Toggle
        tabLogin        = findViewById(R.id.tabLogin);
        tabSignup       = findViewById(R.id.tabSignup);
        toggleIndicator = findViewById(R.id.toggleIndicator);

        // ── Decorative blobs / glows
        glowTop       = findViewById(R.id.glowTop);       // may be null on old layout
        glowBottom    = findViewById(R.id.glowBottom);

        // ── Labels
        tvAuthTitle      = findViewById(R.id.tvAuthTitle);
        tvAuthSub        = findViewById(R.id.tvAuthSub);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // ── Input fields
        etLoginEmail            = findViewById(R.id.etLoginEmail);
        etLoginPassword         = findViewById(R.id.etLoginPassword);
        etFullName              = findViewById(R.id.etFullName);
        etSignupEmail           = findViewById(R.id.etSignupEmail);
        etSignupPassword        = findViewById(R.id.etSignupPassword);
        etSignupConfirmPassword = findViewById(R.id.etSignupConfirmPassword);

        // ── Buttons
        btnLogin  = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // ── Hint nav links
        swipeHintLogin  = findViewById(R.id.swipeHintLogin);
        swipeHintSignup = findViewById(R.id.swipeHintSignup);

        // ── Strength bars
        strengthBar1 = findViewById(R.id.strengthBar1);
        strengthBar2 = findViewById(R.id.strengthBar2);
        strengthBar3 = findViewById(R.id.strengthBar3);
        strengthBar4 = findViewById(R.id.strengthBar4);

        // ── Toggle pill sizing
        toggleIndicator.post(() -> {
            toggleWidth = ((View) toggleIndicator.getParent()).getWidth() / 2;
            ViewGroup.LayoutParams lp = toggleIndicator.getLayoutParams();
            lp.width = toggleWidth;
            toggleIndicator.setLayoutParams(lp);
        });

        // ── Wire up
        setupLoader();
        setupToggleListeners();
        setupAuthCardSwipe();
        setupButtonAnimations();
        setupPasswordStrength();

        btnLogin.setOnClickListener(v  -> { pulseButton(btnLogin);  loginUser(); });
        btnSignup.setOnClickListener(v -> { pulseButton(btnSignup); registerUser(); });
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // ── Initial state + entrance
        showLogin(false);
        startFloatingParallax();
        runEntranceAnimation();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TOGGLE & TAB
    // ══════════════════════════════════════════════════════════════════════

    private void setupToggleListeners() {
        tabLogin.setOnClickListener(v  -> { if (!isLoginVisible)  switchToLogin(); });
        tabSignup.setOnClickListener(v -> { if (isLoginVisible)   switchToSignup(); });
        if (swipeHintLogin  != null) swipeHintLogin.setOnClickListener(v  -> switchToSignup());
        if (swipeHintSignup != null) swipeHintSignup.setOnClickListener(v -> switchToLogin());
    }

    private void switchToLogin() {
        performToggleHaptic();
        animateAuthSwapPremium(() -> showLogin(true));
    }

    private void switchToSignup() {
        performToggleHaptic();
        animateAuthSwapPremium(() -> showSignup(true));
    }

    private void showLogin(boolean animate) {
        isLoginVisible = true;

        // ── Content swap with fade+slide
        layoutSignup.animate().alpha(0f).translationY(-16f).setDuration(150)
                .withEndAction(() -> {
                    layoutSignup.setVisibility(View.GONE);
                    layoutSignup.setTranslationY(0f);
                }).start();

        layoutLogin.setAlpha(0f);
        layoutLogin.setTranslationY(16f);
        layoutLogin.setVisibility(View.VISIBLE);
        layoutLogin.animate().alpha(1f).translationY(0f)
                .setDuration(220).setStartDelay(80)
                .setInterpolator(new DecelerateInterpolator()).start();

        // ── Tab colours
        tabLogin.setTextColor(0xFFFFFFFF);
        tabSignup.setTextColor(0xFF3D5580);

        // ── Title
        if (tvAuthTitle != null) tvAuthTitle.setText("Welcome back");
        if (tvAuthSub   != null) tvAuthSub.setText("Sign in to continue tracking smarter");

        // ── Pill
        if (animate) {
            toggleIndicator.animate().translationX(0)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator()).start();
        } else {
            toggleIndicator.setTranslationX(0);
        }
    }

    private void showSignup(boolean animate) {
        isLoginVisible = false;

        // ── Content swap
        layoutLogin.animate().alpha(0f).translationY(-16f).setDuration(150)
                .withEndAction(() -> {
                    layoutLogin.setVisibility(View.GONE);
                    layoutLogin.setTranslationY(0f);
                }).start();

        layoutSignup.setAlpha(0f);
        layoutSignup.setTranslationY(16f);
        layoutSignup.setVisibility(View.VISIBLE);
        layoutSignup.animate().alpha(1f).translationY(0f)
                .setDuration(220).setStartDelay(80)
                .setInterpolator(new DecelerateInterpolator()).start();

        // ── Tab colours
        tabSignup.setTextColor(0xFFFFFFFF);
        tabLogin.setTextColor(0xFF3D5580);

        // ── Title
        if (tvAuthTitle != null) tvAuthTitle.setText("Create account");
        if (tvAuthSub   != null) tvAuthSub.setText("Start your smart expense journey");

        // ── Pill
        if (animate) {
            toggleIndicator.animate().translationX(toggleWidth)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator()).start();
        } else {
            toggleIndicator.setTranslationX(toggleWidth);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SWIPE GESTURE
    // ══════════════════════════════════════════════════════════════════════

    private void setupAuthCardSwipe() {
        View.OnTouchListener swipeListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    swipeStartX    = event.getRawX();
                    swipeStartTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_UP:
                    float diffX    = event.getRawX() - swipeStartX;
                    long  duration = System.currentTimeMillis() - swipeStartTime;
                    float velocity = Math.abs(diffX) / Math.max(duration, 1) * 1000;

                    if ((diffX > SWIPE_DISTANCE || velocity > SWIPE_VELOCITY) && isLoginVisible) {
                        switchToSignup();
                        return true;
                    }
                    if ((diffX < -SWIPE_DISTANCE || velocity > SWIPE_VELOCITY) && !isLoginVisible) {
                        switchToLogin();
                        return true;
                    }
                    return false;
            }
            return false;
        };

        layoutLogin.setOnTouchListener(swipeListener);
        layoutSignup.setOnTouchListener(swipeListener);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════

    /** Staggered entrance: brand floats in → card rises up */
    private void runEntranceAnimation() {
        View brandLayout = findViewById(R.id.layoutBrand);
        View card        = findViewById(R.id.containerCard);
        View footer      = findViewById(R.id.layoutFooter);

        if (brandLayout != null) {
            brandLayout.setAlpha(0f);
            brandLayout.setTranslationY(-28f);
            brandLayout.animate().alpha(1f).translationY(0f)
                    .setDuration(500).setStartDelay(80)
                    .setInterpolator(new DecelerateInterpolator(1.5f)).start();
        }
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(60f);
            card.animate().alpha(1f).translationY(0f)
                    .setDuration(600).setStartDelay(220)
                    .setInterpolator(new DecelerateInterpolator(2f)).start();
        }
        if (footer != null) {
            footer.setAlpha(0f);
            footer.animate().alpha(1f).setDuration(400).setStartDelay(600).start();
        }
    }

    private void startFloatingParallax() {
        // shapeCircle / shapeTriangle removed — using glow blobs now
        if (glowTop != null)
            ObjectAnimator.ofFloat(glowTop, "translationY", 0f, 12f).setDuration(7000).start();
        if (glowBottom != null)
            ObjectAnimator.ofFloat(glowBottom, "translationY", 0f, -10f).setDuration(6500).start();
    }
    /** Subtle squeeze before tab swap — premium tactile feel */
    private void animateAuthSwapPremium(Runnable onComplete) {
        View target = isLoginVisible ? layoutLogin : layoutSignup;
        target.animate()
                .scaleX(0.97f).scaleY(0.97f).alpha(0.85f)
                .setDuration(110)
                .withEndAction(() -> {
                    target.setScaleX(1f);
                    target.setScaleY(1f);
                    target.setAlpha(1f);
                    if (onComplete != null) onComplete.run();
                }).start();
    }

    // ── Button press bounce ────────────────────────────────────────────
    private void setupButtonAnimations() {
        setupPressAnimation(btnLogin);
        setupPressAnimation(btnSignup);
    }

    private void setupPressAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.96f).scaleY(0.96f)
                        .setDuration(90).setInterpolator(new DecelerateInterpolator()).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(200).setInterpolator(new OvershootInterpolator(2f)).start();
            }
            return false; // let onClick still fire
        });
    }

    private void pulseButton(View btn) {
        if (btn == null) return;
        ObjectAnimator sx  = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 0.94f, 1f);
        ObjectAnimator sy  = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 0.94f, 1f);
        sx.setDuration(280); sy.setDuration(280);
        sx.setInterpolator(new OvershootInterpolator(3f));
        sy.setInterpolator(new OvershootInterpolator(3f));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.start();
    }

    // ── Haptic ────────────────────────────────────────────────────────
    private void performToggleHaptic() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.EFFECT_TICK));
        } else {
            vibrator.vibrate(14);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSWORD STRENGTH
    // ══════════════════════════════════════════════════════════════════════

    private void setupPasswordStrength() {
        if (etSignupPassword == null) return;
        etSignupPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateStrengthBars(s.toString()); }
        });
    }

    private void updateStrengthBars(String pw) {
        int score = 0;
        if (pw.length() >= 6)  score++;
        if (pw.length() >= 10) score++;
        if (pw.matches(".*[A-Z].*") && pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[@#$%^&+=!].*")) score++;

        View[] bars   = {strengthBar1, strengthBar2, strengthBar3, strengthBar4};
        int[]  colors = {0xFFFF4444, 0xFFFFAA22, 0xFF22AAFF, 0xFF22CC66};

        for (int i = 0; i < bars.length; i++) {
            if (bars[i] == null) continue;
            int color = (i < score) ? colors[Math.max(0, score - 1)] : 0xFF1A2540;
            final int finalColor = color;
            final View bar = bars[i];
            bar.animate().alpha(0.3f).setDuration(70).withEndAction(() -> {
                bar.setBackgroundTintList(ColorStateList.valueOf(finalColor));
                bar.animate().alpha(1f).setDuration(110).start();
            }).start();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOADER
    // ══════════════════════════════════════════════════════════════════════

    private void setupLoader() {
        loader = new Dialog(this);
        loader.setContentView(R.layout.dialog_loader);
        loader.setCancelable(false);
        if (loader.getWindow() != null)
            loader.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ImageView ring = loader.findViewById(R.id.loaderRing);
        if (ring != null) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f);
            rotate.setDuration(1200);
            rotate.setRepeatCount(ValueAnimator.INFINITE);
            rotate.setInterpolator(null);
            rotate.start();
        }
    }

    private void dismissLoader() {
        if (loader != null && loader.isShowing()) loader.dismiss();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIREBASE — REGISTER
    // ══════════════════════════════════════════════════════════════════════

    private void registerUser() {
        String name  = etFullName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String pass  = etSignupPassword.getText().toString().trim();
        String cpass = etSignupConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
            toast("Fill all required fields"); return;
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            toast("Name must contain only alphabets"); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email address"); return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(pass).matches()) {
            toast("Password must have uppercase, lowercase, number & special character"); return;
        }
        if (!pass.equals(cpass)) {
            toast("Passwords do not match"); return;
        }
        if (!isInternetAvailable()) {
            toast("No internet connection"); return;
        }

        loader.show();
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        dismissLoader();
                        toast(task.getException().getMessage());
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) { dismissLoader(); return; }

                    user.sendEmailVerification()
                            .addOnFailureListener(e -> toast("Failed to send verification email"));

                    String uid = user.getUid();
                    String createdAtText = new java.text.SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()
                    ).format(new java.util.Date());

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("uid",             uid);
                    map.put("name",            name);
                    map.put("email",           email);
                    map.put("role",            "user");
                    map.put("createdAtText",   createdAtText);
                    map.put("createdAtMillis", System.currentTimeMillis());

                    usersRef.child(uid).setValue(map).addOnCompleteListener(dbTask -> {
                        dismissLoader();
                        auth.signOut();
                        toast("Verification email sent. Please verify before logging in.");
                        switchToLogin();
                    });
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIREBASE — LOGIN
    // ══════════════════════════════════════════════════════════════════════

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String pass  = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            toast("Enter email & password"); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email"); return;
        }
        if (System.currentTimeMillis() < lockUntil) {
            long remaining = (lockUntil - System.currentTimeMillis()) / 1000;
            toast("Too many attempts. Try again in " + remaining + " sec");
            return;
        }
        if (!isInternetAvailable()) {
            toast("No internet connection"); return;
        }

        loader.show();
        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        failedAttempts++;
                        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
                            lockUntil      = System.currentTimeMillis() + LOCK_DURATION;
                            failedAttempts = 0;
                            toast("Too many failed attempts. Locked for 30 seconds.");
                        } else {
                            toast("Invalid credentials. Attempts left: "
                                    + (MAX_LOGIN_ATTEMPTS - failedAttempts));
                        }
                        dismissLoader();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null || !user.isEmailVerified()) {
                        dismissLoader();
                        auth.signOut();
                        toast("Email not verified. Please check your inbox.");
                        return;
                    }

                    usersRef.child(user.getUid()).child("role")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    dismissLoader();
                                    failedAttempts = 0;
                                    saveUidLocally(user.getUid());
                                    String role = snapshot.getValue(String.class);
                                    startActivity(new Intent(AuthActivity.this,
                                            "admin".equals(role)
                                                    ? AdminDashboardActivity.class
                                                    : UserDashboardActivity.class));
                                    finish();
                                }
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    dismissLoader();
                                    toast("Login failed. Please try again.");
                                }
                            });
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIREBASE — REDIRECT
    // ══════════════════════════════════════════════════════════════════════

    private void redirectToDashboard(FirebaseUser user) {
        usersRef.child(user.getUid()).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        saveUidLocally(user.getUid());
                        String role = snapshot.getValue(String.class);
                        startActivity(new Intent(AuthActivity.this,
                                "admin".equals(role)
                                        ? AdminDashboardActivity.class
                                        : UserDashboardActivity.class));
                        finish();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AuthActivity.this,
                                "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD DIALOG
    // ══════════════════════════════════════════════════════════════════════

    private void showForgotPasswordDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter registered email");

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) {
                        toast("Email required"); return;
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        toast("Enter a valid email"); return;
                    }
                    if (!isInternetAvailable()) {
                        toast("No internet connection"); return;
                    }
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(u -> toast("Password reset email sent"))
                            .addOnFailureListener(e -> toast(e.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void saveUidLocally(String uid) {
        getSharedPreferences("USER_PREF", MODE_PRIVATE)
                .edit().putString("UID", uid).apply();
    }

    private boolean isInternetAvailable() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network net = cm.getActiveNetwork();
            if (net == null) return false;
            android.net.NetworkCapabilities cap = cm.getNetworkCapabilities(net);
            return cap != null && (
                    cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                            || cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                            || cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }
}