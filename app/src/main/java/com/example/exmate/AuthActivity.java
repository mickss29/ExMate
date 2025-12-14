package com.example.exmate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class AuthActivity extends AppCompatActivity {

    // ================= UI =================
    private LinearLayout layoutLogin, layoutSignup;
    private TextView tabLogin, tabSignup;
    private ConstraintLayout rootLayout;
    private View shapeCircle, shapeTriangle;

    private TextInputEditText etLoginEmail, etLoginPassword;
    private TextInputEditText etFullName, etSignupEmail,
            etSignupPassword, etSignupConfirmPassword, etSignupPhone;
    private Button btnLogin, btnSignup;

    // ================= FIREBASE =================
    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    // ================= STATE =================
    private boolean isLoginVisible = false;

    // ================= ADMIN (TEMP) =================
    private static final String ADMIN_EMAIL = "admin@exmate.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // UI init
        rootLayout = findViewById(R.id.rootLayout);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);
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

        // DEFAULT SCREEN
        showLogin(false);

        animateDarkBlueBackground();
        startFloatingParallax();

        tabLogin.setOnClickListener(v -> showLogin(true));
        tabSignup.setOnClickListener(v -> showSignup(true));

        btnSignup.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
    }

    // ================= TOGGLE =================

    private void showLogin(boolean animate) {
        if (isLoginVisible) return;
        isLoginVisible = true;
        updateTabs(true);

        if (animate) animateSwitch(layoutSignup, layoutLogin);
        else {
            layoutSignup.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        }
    }

    private void showSignup(boolean animate) {
        if (!isLoginVisible) return;
        isLoginVisible = false;
        updateTabs(false);

        if (animate) animateSwitch(layoutLogin, layoutSignup);
        else {
            layoutLogin.setVisibility(View.GONE);
            layoutSignup.setVisibility(View.VISIBLE);
        }
    }

    // ================= FIXED VOID ERROR HERE =================
    private void animateSwitch(View hide, View show) {

        hide.animate()
                .alpha(0f)
                .translationX(-40)
                .setDuration(200)
                .withEndAction(() -> {

                    hide.setVisibility(View.GONE);
                    hide.setAlpha(1f);
                    hide.setTranslationX(0);

                    show.setAlpha(0f);
                    show.setTranslationX(40);
                    show.setVisibility(View.VISIBLE);

                    show.animate()
                            .alpha(1f)
                            .translationX(0)
                            .setDuration(250)
                            .setInterpolator(new DecelerateInterpolator())
                            .start(); // ✅ allowed
                });
    }

    private void updateTabs(boolean loginSelected) {
        tabLogin.setBackground(loginSelected
                ? getDrawable(R.drawable.toggle_selected_midnight)
                : null);

        tabSignup.setBackground(!loginSelected
                ? getDrawable(R.drawable.toggle_selected_midnight)
                : null);

        tabLogin.setTextColor(loginSelected ? 0xFFFFFFFF : 0xFFB0C4DE);
        tabSignup.setTextColor(!loginSelected ? 0xFFFFFFFF : 0xFFB0C4DE);
    }

    // ================= SIGNUP =================

    private void registerUser() {

        String name = etFullName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String pass = etSignupPassword.getText().toString().trim();
        String cPass = etSignupConfirmPassword.getText().toString().trim();
        String phone = etSignupPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(cPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        clearSignupFields();
        btnSignup.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    btnSignup.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("name", name);
                    map.put("email", email);
                    map.put("phone", phone);
                    map.put("role", "user");
                    map.put("createdAt", System.currentTimeMillis());

                    usersRef.child(user.getUid()).setValue(map);
                    auth.signOut();

                    Toast.makeText(this,
                            "Account created! Please login.",
                            Toast.LENGTH_LONG).show();

                    showLogin(true);
                });
    }

    private void clearSignupFields() {
        etFullName.setText("");
        etSignupEmail.setText("");
        etSignupPassword.setText("");
        etSignupConfirmPassword.setText("");
        etSignupPhone.setText("");
    }

    // ================= LOGIN =================

    private void loginUser() {

        String email = etLoginEmail.getText().toString().trim();
        String pass = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        // ADMIN
        if (email.equals(ADMIN_EMAIL) && pass.equals(ADMIN_PASSWORD)) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    btnLogin.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    startActivity(new Intent(this, UserDashboardActivity.class));
                    finish();
                });
    }

    // ================= BACKGROUND =================

    private void animateDarkBlueBackground() {

        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF001633, 0xFF00204D, 0xFF003366}
        );

        rootLayout.setBackground(gd);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
    }

    private void startFloatingParallax() {

        ObjectAnimator circle = ObjectAnimator.ofFloat(
                shapeCircle, "translationY", 0f, 18f);
        circle.setDuration(6000);
        circle.setRepeatCount(ValueAnimator.INFINITE);
        circle.setRepeatMode(ValueAnimator.REVERSE);
        circle.start();

        ObjectAnimator triangle = ObjectAnimator.ofFloat(
                shapeTriangle, "translationY", 0f, -14f);
        triangle.setDuration(5000);
        triangle.setRepeatCount(ValueAnimator.INFINITE);
        triangle.setRepeatMode(ValueAnimator.REVERSE);
        triangle.start();
    }
}
