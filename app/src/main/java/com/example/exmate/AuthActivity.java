package com.example.exmate;

import android.animation.ValueAnimator;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout layoutLogin, layoutSignup, containerCard;
    private TextView tabLogin, tabSignup;
    private ConstraintLayout rootLayout;
    private View shapeCircle, shapeTriangle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        rootLayout = findViewById(R.id.rootLayout);
        containerCard = findViewById(R.id.containerCard);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);

        shapeCircle = findViewById(R.id.shapeCircle);
        shapeTriangle = findViewById(R.id.shapeTriangle);

        // Initially show login
        layoutLogin.setVisibility(View.VISIBLE);
        layoutSignup.setVisibility(View.GONE);

        animateDarkBlueBackground();
        startFloatingParallax();

        tabLogin.setOnClickListener(v -> showLogin());
        tabSignup.setOnClickListener(v -> showSignup());
    }

    // -------------------------------------------------------------------
    // BACKGROUND ANIMATION
    // -------------------------------------------------------------------
    private void animateDarkBlueBackground() {
        final int color1 = 0xFF001633;
        final int color2 = 0xFF00204D;
        final int color3 = 0xFF003366;
        final int color4 = 0xFF004080;

        final GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{color1, color2, color3}
        );

        rootLayout.setBackground(gradientDrawable);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int start = blendColors(color1, color2, fraction);
            int middle = blendColors(color2, color3, fraction);
            int end = blendColors(color3, color4, fraction);
            gradientDrawable.setColors(new int[]{start, middle, end});
        });

        animator.start();
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverse = 1f - ratio;
        int a = (int) ((color1 >> 24 & 0xff) * inverse + (color2 >> 24 & 0xff) * ratio);
        int r = (int) ((color1 >> 16 & 0xff) * inverse + (color2 >> 16 & 0xff) * ratio);
        int g = (int) ((color1 >> 8 & 0xff) * inverse + (color2 >> 8 & 0xff) * ratio);
        int b = (int) ((color1 & 0xff) * inverse + (color2 & 0xff) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // -------------------------------------------------------------------
    // PARALLAX FLOATING ANIMATION
    // -------------------------------------------------------------------
    private void startFloatingParallax() {

        // Floating circle
        ValueAnimator circleAnim = ValueAnimator.ofFloat(0f, 20f);
        circleAnim.setDuration(6000);
        circleAnim.setRepeatCount(ValueAnimator.INFINITE);
        circleAnim.setRepeatMode(ValueAnimator.REVERSE);
        circleAnim.addUpdateListener(anim -> shapeCircle.setTranslationY((Float) anim.getAnimatedValue()));
        circleAnim.start();

        // Floating triangle
        ValueAnimator triangleAnim = ValueAnimator.ofFloat(0f, -15f);
        triangleAnim.setDuration(5000);
        triangleAnim.setRepeatCount(ValueAnimator.INFINITE);
        triangleAnim.setRepeatMode(ValueAnimator.REVERSE);
        triangleAnim.addUpdateListener(anim -> shapeTriangle.setTranslationY((Float) anim.getAnimatedValue()));
        triangleAnim.start();

        // Touch parallax
        rootLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                float centerX = rootLayout.getWidth() / 2f;
                float centerY = rootLayout.getHeight() / 2f;
                float offsetX = (event.getX() - centerX) / 50f;
                float offsetY = (event.getY() - centerY) / 50f;

                shapeCircle.setTranslationX(offsetX);
                shapeCircle.setTranslationY(offsetY);

                shapeTriangle.setTranslationX(-offsetX);
                shapeTriangle.setTranslationY(-offsetY);
            }
            return true;
        });
    }

    // -------------------------------------------------------------------
    // ULTRA-PREMIUM LOGIN ANIMATION
    // -------------------------------------------------------------------
    private void showLogin() {
        if (layoutLogin.getVisibility() == View.VISIBLE) return;

        // Card micro zoom effect
        containerCard.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(150)
                .withEndAction(() -> containerCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start())
                .start();

        // Fade-through transition
        layoutSignup.animate()
                .alpha(0f)
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutSignup.setVisibility(View.GONE);
                    layoutSignup.setAlpha(1f);
                    layoutSignup.setScaleX(1f);
                    layoutSignup.setScaleY(1f);

                    layoutLogin.setVisibility(View.VISIBLE);
                    layoutLogin.setAlpha(0f);
                    layoutLogin.setScaleX(0.9f);
                    layoutLogin.setScaleY(0.9f);

                    layoutLogin.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(280)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                })
                .start();

        animateTabs(true);
    }

    // -------------------------------------------------------------------
    // ULTRA-PREMIUM SIGNUP ANIMATION
    // -------------------------------------------------------------------
    private void showSignup() {
        if (layoutSignup.getVisibility() == View.VISIBLE) return;

        // Card micro zoom effect
        containerCard.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(150)
                .withEndAction(() -> containerCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start())
                .start();

        // Fade-through transition
        layoutLogin.animate()
                .alpha(0f)
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(180)
                .withEndAction(() -> {
                    layoutLogin.setVisibility(View.GONE);
                    layoutLogin.setAlpha(1f);
                    layoutLogin.setScaleX(1f);
                    layoutLogin.setScaleY(1f);

                    layoutSignup.setVisibility(View.VISIBLE);
                    layoutSignup.setAlpha(0f);
                    layoutSignup.setScaleX(0.9f);
                    layoutSignup.setScaleY(0.9f);

                    layoutSignup.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(280)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                })
                .start();

        animateTabs(false);
    }

    // -------------------------------------------------------------------
    // ULTRA-PREMIUM TAB ANIMATION
    // -------------------------------------------------------------------
    private void animateTabs(boolean loginSelected) {

        TextView selected = loginSelected ? tabLogin : tabSignup;
        TextView normal = loginSelected ? tabSignup : tabLogin;

        selected.animate()
                .alpha(1f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(180)
                .start();

        normal.animate()
                .alpha(0.6f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();

        if (loginSelected) {
            tabLogin.setBackgroundResource(R.drawable.toggle_selected_midnight);
            tabSignup.setBackground(null);
        } else {
            tabSignup.setBackgroundResource(R.drawable.toggle_selected_midnight);
            tabLogin.setBackground(null);
        }
    }
}
