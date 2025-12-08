package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    // Tabs
    private TextView tabLogin, tabSignup;

    // Layout containers
    private LinearLayout layoutLogin, layoutSignup;

    // Buttons
    private Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Init UI
        initViews();

        // Default state
        showLogin();

        // Tab click listeners
        tabLogin.setOnClickListener(v -> showLogin());
        tabSignup.setOnClickListener(v -> showSignup());

        // Button actions (logic later)
        btnLogin.setOnClickListener(v -> {
            // TODO: Add login logic
        });

        btnSignup.setOnClickListener(v -> {
            // TODO: Add signup logic
        });
    }

    private void initViews() {
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);

        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);

        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
    }

    private void showLogin() {
        tabLogin.setBackgroundResource(R.drawable.bg_toggle_selected);
        tabSignup.setBackground(null);

        fadeOut(layoutSignup);
        layoutSignup.setVisibility(View.GONE);

        layoutLogin.setVisibility(View.VISIBLE);
        fadeIn(layoutLogin);
    }

    private void showSignup() {
        tabSignup.setBackgroundResource(R.drawable.bg_toggle_selected);
        tabLogin.setBackground(null);

        fadeOut(layoutLogin);
        layoutLogin.setVisibility(View.GONE);

        layoutSignup.setVisibility(View.VISIBLE);
        fadeIn(layoutSignup);
    }

    private void fadeIn(View view) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(300);
        view.startAnimation(anim);
    }

    private void fadeOut(View view) {
        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(200);
        view.startAnimation(anim);
    }
}
