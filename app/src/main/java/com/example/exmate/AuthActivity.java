package com.example.exmate;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

public class AuthActivity extends AppCompatActivity {

    TextView tabLogin, tabSignup;
    LinearLayout layoutLogin, layoutSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        tabLogin = findViewById(R.id.tabLogin);
        tabSignup = findViewById(R.id.tabSignup);

        layoutLogin = findViewById(R.id.layoutLogin);
        layoutSignup = findViewById(R.id.layoutSignup);

        tabLogin.setOnClickListener(v -> showLogin());
        tabSignup.setOnClickListener(v -> showSignup());
    }

    private void showLogin() {
        tabLogin.setBackgroundResource(R.drawable.toggle_selected);
        tabSignup.setBackground(null);

        layoutSignup.animate().alpha(0).setDuration(200);
        layoutSignup.setVisibility(View.GONE);

        layoutLogin.setVisibility(View.VISIBLE);
        layoutLogin.animate().alpha(1).setDuration(200);
    }

    private void showSignup() {
        tabSignup.setBackgroundResource(R.drawable.toggle_selected);
        tabLogin.setBackground(null);

        layoutLogin.animate().alpha(0).setDuration(200);
        layoutLogin.setVisibility(View.GONE);

        layoutSignup.setVisibility(View.VISIBLE);
        layoutSignup.animate().alpha(1).setDuration(200);
    }
}
