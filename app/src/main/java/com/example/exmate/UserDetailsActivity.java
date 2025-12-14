package com.example.exmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserDetailsActivity extends AppCompatActivity {

    TextView tvName, tvEmail, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvStatus = findViewById(R.id.tvDetailStatus);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        boolean blocked = getIntent().getBooleanExtra("blocked", false);

        tvName.setText("Name: " + name);
        tvEmail.setText("Email: " + email);
        tvStatus.setText("Status: " + (blocked ? "Blocked" : "Active"));
    }
}
