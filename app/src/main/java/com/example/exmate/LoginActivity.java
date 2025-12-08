package com.example.exmate;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText email, password, fullName, confirmPassword;
    Button mainBtn, switchLogin, switchSignUp;
    TextView title, subTitle;
    boolean isLoginMode = true; // Default: Login mode

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Bind Views
        email = findViewById(R.id.emailInput);
        password = findViewById(R.id.passInput);
        fullName = findViewById(R.id.nameInput);
        confirmPassword = findViewById(R.id.confirmPassInput);

        mainBtn = findViewById(R.id.btnMainAction);
        title = findViewById(R.id.titleText);
        subTitle = findViewById(R.id.subTitleText);

        switchLogin = findViewById(R.id.btnSwitchSignIn);
        switchSignUp = findViewById(R.id.btnSwitchSignUp);

        // Switch to Sign Up Mode
        switchSignUp.setOnClickListener(v -> {
            isLoginMode = false;
            updateUI();
        });

        // Switch to Login Mode
        switchLogin.setOnClickListener(v -> {
            isLoginMode = true;
            updateUI();
        });

        // Main Action Button
        mainBtn.setOnClickListener(v -> {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleSignUp();
            }
        });

        updateUI(); // Ensure correct UI on launch
    }

    // Smooth fade animation for showing/hiding fields
    private void fadeView(View view, boolean show) {
        AlphaAnimation animation = show
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        animation.setDuration(250);
        view.startAnimation(animation);
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateUI() {

        if (isLoginMode) {
            // TEXT UPDATES
            title.setText("Welcome Back 👋");
            subTitle.setText("Sign in to continue");
            mainBtn.setText("Sign In");

            // FIELD VISIBILITY
            fadeView(fullName, false);
            fadeView(confirmPassword, false);

            // BUTTON STYLING
            switchLogin.setBackgroundResource(R.drawable.bg_toggle_selected);
            switchLogin.setTextColor(getColor(android.R.color.black));

            switchSignUp.setBackgroundResource(android.R.color.transparent);
            switchSignUp.setTextColor(0xA8FFFFFF); // slight faded white

        } else {
            // TEXT UPDATES
            title.setText("Welcome Aboard ✨");
            subTitle.setText("Create your account");
            mainBtn.setText("Sign Up");

            // FIELD VISIBILITY
            fadeView(fullName, true);
            fadeView(confirmPassword, true);

            // BUTTON STYLING
            switchSignUp.setBackgroundResource(R.drawable.bg_toggle_selected);
            switchSignUp.setTextColor(getColor(android.R.color.black));

            switchLogin.setBackgroundResource(android.R.color.transparent);
            switchLogin.setTextColor(0xA8FFFFFF);
        }
    }

    private void handleLogin() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        if (userEmail.isEmpty() || userPass.isEmpty()) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        // TODO: Firebase login code can be added here
    }

    private void handleSignUp() {
        String userName = fullName.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();
        String confirmPass = confirmPassword.getText().toString().trim();

        if (userName.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!userPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show();

        // TODO: Firebase sign-up code here
    }
}
