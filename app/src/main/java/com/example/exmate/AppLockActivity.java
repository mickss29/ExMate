package com.example.exmate;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private EditText etPin;
    private final StringBuilder pinBuilder = new StringBuilder();
    private GridLayout keypad;
    private ImageView btnBackspace;

    private LinearLayout pinDotsLayout;
    private TextView tvBiometric;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_TIME_MS = 30_000;

    private int wrongAttempts = 0;
    private boolean isLocked = false;

    // ✅ FIX: biometric prompt 2 baar show na ho
    private boolean biometricShownOnce = false;
    private boolean biometricInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        etPin = findViewById(R.id.etPin);
        pinDotsLayout = findViewById(R.id.pinDotsLayout);
        tvBiometric = findViewById(R.id.tvBiometric);
        keypad = findViewById(R.id.keypad);
        btnBackspace = findViewById(R.id.btnBackspace);

        setupBiometric();

        // 🔥 Auto biometric only ONCE
        if (savedInstanceState == null) {
            tryBiometricAuth(true);
        }

        etPin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                updatePinDots(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 4) unlockWithPin();
            }
        });

        // 🔢 KEYPAD
        for (int i = 0; i < keypad.getChildCount(); i++) {
            View v = keypad.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setOnClickListener(view -> {
                    if (pinBuilder.length() < 4 && !isLocked) {
                        pinBuilder.append(((TextView) view).getText().toString());
                        etPin.setText(pinBuilder.toString());
                    }
                });
            }
        }

        btnBackspace.setOnClickListener(v -> {
            if (pinBuilder.length() > 0 && !isLocked) {
                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                etPin.setText(pinBuilder.toString());
            }
        });

        // 🔥 Manual biometric
        tvBiometric.setOnClickListener(v -> {
            if (!isLocked) {
                tryBiometricAuth(false);
            }
        });
    }

    private void updatePinDots(int length) {
        for (int i = 0; i < pinDotsLayout.getChildCount(); i++) {
            View dot = pinDotsLayout.getChildAt(i);
            dot.setBackgroundResource(
                    i < length
                            ? R.drawable.bg_pin_dot_filled
                            : R.drawable.bg_pin_dot_empty
            );
        }
    }

    // ================= BIOMETRIC =================

    private void setupBiometric() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {

                        biometricInProgress = false;

                        AppLockManager.setUnlocked(AppLockActivity.this, true);
                        redirectToDashboard();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        biometricInProgress = false;
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // keep silent
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock ExMate")
                .setSubtitle("Use fingerprint, face or device lock")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();
    }

    /**
     * @param autoCall true if this is automatic call from onCreate
     */
    private void tryBiometricAuth(boolean autoCall) {

        if (isLocked) return;
        if (biometricInProgress) return;

        // ✅ auto call sirf ek baar
        if (autoCall && biometricShownOnce) return;

        int can = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (can == BiometricManager.BIOMETRIC_SUCCESS) {

            biometricInProgress = true;

            if (autoCall) biometricShownOnce = true;

            biometricPrompt.authenticate(promptInfo);
        }
    }

    // ================= PIN =================

    private void unlockWithPin() {

        if (isLocked) return;

        String enteredPin = etPin.getText().toString().trim();
        if (enteredPin.length() < 4) return;

        // 🔹 1️⃣ TRY LOCAL PIN
        if (AppLockManager.checkPin(this, enteredPin)) {
            AppLockManager.setUnlocked(this, true);
            redirectToDashboard();
            return;
        }

        // 🔹 2️⃣ FALLBACK → FIREBASE PIN
        verifyPinFromFirebase(enteredPin);
    }

    private void verifyPinFromFirebase(String enteredPin) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String enteredHash = HashUtil.sha256(enteredPin);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("appLock")
                .child("pinHash")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        String savedHash = snapshot.getValue(String.class);

                        if (savedHash != null && savedHash.equals(enteredHash)) {

                            // 🔐 RESTORE LOCAL PIN AFTER CLEAR DATA
                            AppLockManager.enable(AppLockActivity.this, enteredPin);
                            AppLockManager.setUnlocked(AppLockActivity.this, true);
                            redirectToDashboard();

                        } else {
                            handleWrongPin();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        handleWrongPin();
                    }
                });
    }

    private void handleWrongPin() {

        wrongAttempts++;

        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(120);
        }

        pinBuilder.setLength(0);
        etPin.setText("");
        updatePinDots(0);

        pinDotsLayout.animate().translationX(12).setDuration(60)
                .withEndAction(() ->
                        pinDotsLayout.animate().translationX(-12).setDuration(60)
                                .withEndAction(() ->
                                        pinDotsLayout.animate().translationX(0).setDuration(60).start()
                                ).start()
                ).start();

        if (wrongAttempts >= MAX_ATTEMPTS) lockFor30Seconds();
        else Toast.makeText(this,
                "Wrong PIN (" + (MAX_ATTEMPTS - wrongAttempts) + " tries left)",
                Toast.LENGTH_SHORT).show();
    }

    private void lockFor30Seconds() {

        isLocked = true;

        // ✅ Disable all keypad keys properly
        setKeypadEnabled(false);

        Toast.makeText(this,
                "Locked for 30 seconds",
                Toast.LENGTH_LONG).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            isLocked = false;
            wrongAttempts = 0;

            setKeypadEnabled(true);

            pinBuilder.setLength(0);
            etPin.setText("");
            updatePinDots(0);

        }, LOCK_TIME_MS);
    }

    private void setKeypadEnabled(boolean enabled) {

        keypad.setEnabled(enabled);
        btnBackspace.setEnabled(enabled);
        tvBiometric.setEnabled(enabled);

        for (int i = 0; i < keypad.getChildCount(); i++) {
            View v = keypad.getChildAt(i);
            v.setEnabled(enabled);
            v.setAlpha(enabled ? 1f : 0.5f);
        }

        btnBackspace.setAlpha(enabled ? 1f : 0.5f);
        tvBiometric.setAlpha(enabled ? 1f : 0.5f);
    }

    // ================= REDIRECT =================

    private void redirectToDashboard() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);

                        Intent i = new Intent(
                                AppLockActivity.this,
                                "admin".equals(role)
                                        ? AdminDashboardActivity.class
                                        : UserDashboardActivity.class
                        );

                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AppLockActivity.this,
                                "Unlock failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
