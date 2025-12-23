package com.example.exmate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
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

    // UI
    private LinearLayout layoutLogin, layoutSignup;
    private TextView tabLogin, tabSignup, tvForgotPassword;
    private ConstraintLayout rootLayout;
    private View shapeCircle, shapeTriangle;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

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

        setupLoader();
        setupDragWithHaptic();

        showLogin(false);
        animateDarkBlueBackground();
        startFloatingParallax();

        tabLogin.setOnClickListener(v -> showLogin(true));
        tabSignup.setOnClickListener(v -> showSignup(true));

        btnSignup.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
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
    }

    private void showSignup(boolean animate) {
        isLoginVisible = false;
        layoutLogin.setVisibility(View.GONE);
        layoutSignup.setVisibility(View.VISIBLE);
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

}
