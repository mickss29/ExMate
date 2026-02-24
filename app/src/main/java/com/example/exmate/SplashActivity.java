package com.example.exmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        TextView tagline = findViewById(R.id.tagline);

        // 🍎 Apple-style natural easing curve
        PathInterpolator premiumEase =
                new PathInterpolator(0.22f, 1f, 0.36f, 1f);

        // Initial state
        logo.setAlpha(0f);
        logo.setScaleX(0.94f);
        logo.setScaleY(0.94f);
        tagline.setAlpha(0f);

        // 🔹 LOGO animation
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1500)
                .setInterpolator(premiumEase)
                .start();

        // 🔹 TAGLINE animation
        tagline.animate()
                .alpha(1f)
                .setStartDelay(1000)
                .setDuration(800)
                .setInterpolator(premiumEase)
                .start();

        // 🔐 Request SMS permission silently (no UI break)
        requestSmsPermissionIfNeeded();

        // 🔹 Move to Auth screen
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2800);
    }

    private void requestSmsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                        },
                        SMS_PERMISSION_CODE
                );
            }
        }
    }
}