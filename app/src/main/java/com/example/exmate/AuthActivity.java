package com.example.exmate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    LinearLayout containerCard, layoutLogin, layoutSignup;
    TextView tabLogin, tabSignup;
    View shapeCircle, shapeTriangle;
    Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        containerCard = findViewById(R.id.containerCard);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);
        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);
        shapeCircle = findViewById(R.id.shapeCircle);
        shapeTriangle = findViewById(R.id.shapeTriangle);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // 1️⃣ Floating shapes rotation + drift
        shapeCircle.animate().translationXBy(20).translationYBy(20).setDuration(8000).setStartDelay(0).start();
        shapeTriangle.animate().translationXBy(-20).translationYBy(-20).setDuration(9000).setStartDelay(0).start();

        // 2️⃣ Card slide + fade in
        containerCard.setTranslationY(50f);
        containerCard.setAlpha(0f);
        containerCard.animate().translationY(0f).alpha(1f).setDuration(700).start();

        // 3️⃣ Tab switching
        tabLogin.setOnClickListener(v -> switchForm(true));
        tabSignup.setOnClickListener(v -> switchForm(false));

        // 4️⃣ Button micro-interaction
        addButtonEffect(btnLogin);
        addButtonEffect(btnSignup);
    }

    private void switchForm(boolean showLogin) {
        if (showLogin) {
            layoutSignup.setAlpha(1f);
            layoutSignup.animate().alpha(0f).setDuration(500).withEndAction(() -> layoutSignup.setVisibility(View.GONE)).start();

            layoutLogin.setAlpha(0f);
            layoutLogin.setVisibility(View.VISIBLE);
            layoutLogin.animate().alpha(1f).setDuration(500).start();
        } else {
            layoutLogin.setAlpha(1f);
            layoutLogin.animate().alpha(0f).setDuration(500).withEndAction(() -> layoutLogin.setVisibility(View.GONE)).start();

            layoutSignup.setAlpha(0f);
            layoutSignup.setVisibility(View.VISIBLE);
            layoutSignup.animate().alpha(1f).setDuration(500).start();
        }
    }

    private void addButtonEffect(Button btn) {
        btn.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        });
    }
}
