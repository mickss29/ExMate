package com.example.exmate;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private final String brand = "EXMATE";
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View line = findViewById(R.id.lineView);
        TextView brandText = findViewById(R.id.brandText);
        ImageView logo = findViewById(R.id.logo);
        TextView tagline = findViewById(R.id.tagline);

        // 1️⃣ Line Slide From Left
        line.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();

        // 2️⃣ Rotate Line
        handler.postDelayed(() -> {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(line, "rotation", 0f, 720f);
            rotate.setDuration(700);
            rotate.start();
        }, 700);

        // 3️⃣ Reveal EXMATE Text
        handler.postDelayed(() -> {
            line.animate().alpha(0f).setDuration(300).start();
            brandText.setAlpha(1f);
            startTypingBrand(brandText);
        }, 1400);

        // 4️⃣ Convert Text → Logo
        handler.postDelayed(() -> {

            brandText.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .alpha(0f)
                    .setDuration(600)
                    .start();

            logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(900)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();

        }, 2300);

        // 5️⃣ Tagline Splash Entry
        handler.postDelayed(() -> {
            tagline.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }, 3200);

        // 6️⃣ FINAL NAVIGATION CHECK (NO LOGIN FLASH)
        handler.postDelayed(() -> {

            if (isUserLoggedIn()) {
                startActivity(new Intent(SplashActivity.this, HomeFragment.class));
            } else {
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }

            finish();

        }, 4500);
    }

    // 🔐 LOGIN STATE CHECK
    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("exmate_prefs", MODE_PRIVATE);
        return prefs.getBoolean("is_logged_in", false);
    }

    // ✍️ Typing Animation
    private void startTypingBrand(TextView textView) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index <= brand.length()) {
                    textView.setText(brand.substring(0, index));
                    index++;
                    handler.postDelayed(this, 80);
                }
            }
        }, 0);
    }
}