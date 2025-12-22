package com.example.exmate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        TextView tagline = findViewById(R.id.tagline);

        // 🍎 Apple-style natural easing curve
        PathInterpolator premiumEase =
                new PathInterpolator(0.22f, 1f, 0.36f, 1f);

        // Initial state (clean & controlled)
        logo.setAlpha(0f);
        logo.setScaleX(0.94f);
        logo.setScaleY(0.94f);

        tagline.setAlpha(0f);

        // 🔹 LOGO — calm luxury reveal
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1500)
                .setInterpolator(premiumEase)
                .start();

        // 🔹 TAGLINE — delayed, respectful entrance
        tagline.animate()
                .alpha(1f)
                .setStartDelay(1000)
                .setDuration(800)
                .setInterpolator(premiumEase)
                .start();

        // 🔹 Seamless transition to next screen
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2800);
    }
}
