package com.example.exmate;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout layoutLogin, layoutSignup, containerCard;
    private TextView tabLogin, tabSignup;
    private ConstraintLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth); // your XML file

        rootLayout = findViewById(R.id.rootLayout);
        containerCard = findViewById(R.id.containerCard);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);

        layoutLogin.setVisibility(android.view.View.VISIBLE);
        layoutSignup.setVisibility(android.view.View.GONE);

        // Animate background shades
        animateBackgroundShades();

        // Tab click listeners
        tabLogin.setOnClickListener(v -> showLogin());
        tabSignup.setOnClickListener(v -> showSignup());
    }

    private void animateBackgroundShades() {
        // Blue shades
        final int colorStart = 0xFF0D47A1; // dark blue
        final int colorMiddle = 0xFF1976D2; // medium blue
        final int colorEnd = 0xFF42A5F5;   // light blue

        final GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{colorStart, colorMiddle}
        );
        gradientDrawable.setCornerRadius(0f);
        rootLayout.setBackground(gradientDrawable);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(8000); // 8 seconds
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();

            // Blend colors
            int newStart = blendColors(colorStart, colorMiddle, fraction);
            int newEnd = blendColors(colorMiddle, colorEnd, fraction);

            gradientDrawable.setColors(new int[]{newStart, newEnd});
        });

        animator.start();
    }

    // Helper method to blend two colors
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        int a = (int) ((color1 >> 24 & 0xff) * inverseRatio + (color2 >> 24 & 0xff) * ratio);
        int r = (int) ((color1 >> 16 & 0xff) * inverseRatio + (color2 >> 16 & 0xff) * ratio);
        int g = (int) ((color1 >> 8 & 0xff) * inverseRatio + (color2 >> 8 & 0xff) * ratio);
        int b = (int) ((color1 & 0xff) * inverseRatio + (color2 & 0xff) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void showLogin() {
        if (layoutLogin.getVisibility() == android.view.View.VISIBLE) return;

        // Slide Signup out
        layoutSignup.animate()
                .translationX(containerCard.getWidth())
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> layoutSignup.setVisibility(android.view.View.GONE))
                .start();

        // Slide Login in
        layoutLogin.setTranslationX(-containerCard.getWidth());
        layoutLogin.setAlpha(0f);
        layoutLogin.setVisibility(android.view.View.VISIBLE);
        layoutLogin.animate()
                .translationX(0f)
                .alpha(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(400)
                .start();

        tabLogin.setBackgroundResource(R.drawable.toggle_selected_midnight);
        tabSignup.setBackgroundResource(android.R.color.transparent);
    }

    private void showSignup() {
        if (layoutSignup.getVisibility() == android.view.View.VISIBLE) return;

        // Slide Login out
        layoutLogin.animate()
                .translationX(-containerCard.getWidth())
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> layoutLogin.setVisibility(android.view.View.GONE))
                .start();

        // Slide Signup in
        layoutSignup.setTranslationX(containerCard.getWidth());
        layoutSignup.setAlpha(0f);
        layoutSignup.setVisibility(android.view.View.VISIBLE);
        layoutSignup.animate()
                .translationX(0f)
                .alpha(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(400)
                .start();

        tabSignup.setBackgroundResource(R.drawable.toggle_selected_midnight);
        tabLogin.setBackgroundResource(android.R.color.transparent);
    }
}
