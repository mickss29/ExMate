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
    private boolean isDragging = false;
    private boolean hapticDone = false;
    private static final int DRAG_THRESHOLD = 180;

    private Vibrator vibrator;
    private boolean isLoginVisible = true;

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

    // ================= AUTH LOGIC (ONLY UPDATED PART) =================

    private void registerUser() {

        String name = etFullName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String phone = etSignupPhone.getText().toString().trim();
        String pass = etSignupPassword.getText().toString().trim();
        String cpass = etSignupConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(cpass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        loader.show();

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        loader.dismiss();
                        Toast.makeText(this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
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
                    map.put("role", "user"); // ✅ ONLY ADDITION

                    usersRef.child(uid).setValue(map).addOnCompleteListener(dbTask -> {
                        loader.dismiss();
                        auth.signOut();
                        Toast.makeText(this,
                                "Verification email sent. Please verify.",
                                Toast.LENGTH_LONG).show();
                        showLogin(true);
                    });
                });
    }

    private void loginUser() {

        String email = etLoginEmail.getText().toString().trim();
        String pass = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        loader.show();

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        loader.dismiss();
                        Toast.makeText(this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        loader.dismiss();
                        return;
                    }

                    if (!user.isEmailVerified()) {
                        loader.dismiss();
                        auth.signOut();
                        Toast.makeText(this,
                                "Email not verified",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ ONLY ADDITION: ROLE CHECK
                    usersRef.child(user.getUid()).child("role")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    loader.dismiss();

                                    String role = snapshot.getValue(String.class);

                                    if ("admin".equals(role)) {
                                        startActivity(new Intent(AuthActivity.this,
                                                AdminDashboardActivity.class));
                                    } else {
                                        startActivity(new Intent(AuthActivity.this,
                                                UserDashboardActivity.class));
                                    }
                                    finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    loader.dismiss();
                                    Toast.makeText(AuthActivity.this,
                                            "Login failed",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void showForgotPasswordDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter registered email");

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        auth.sendPasswordResetEmail(email);
                        Toast.makeText(this,
                                "Reset email sent",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= UI / ANIMATION (UNCHANGED) =================

    private void setupDragWithHaptic() {
        View.OnTouchListener dragListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    isDragging = true;
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
                    isDragging = false;
                    return true;
            }
            return false;
        };
        layoutLogin.setOnTouchListener(dragListener);
        layoutSignup.setOnTouchListener(dragListener);
    }

    private void performHaptic() {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else vibrator.vibrate(20);
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
        loader.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ImageView ring = loader.findViewById(R.id.loaderRing);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f);
        rotate.setDuration(1200);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.start();
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
}
