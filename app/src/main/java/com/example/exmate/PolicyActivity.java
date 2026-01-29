package com.example.exmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PolicyActivity extends AppCompatActivity {

    TextView tvTitle, tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);

        String type = getIntent().getStringExtra("type");

        if ("terms".equals(type)) {
            tvTitle.setText("Terms & Conditions");
            tvContent.setText(getTermsText());
        } else {
            tvTitle.setText("Privacy Policy");
            tvContent.setText(getPrivacyText());
        }
    }

    private String getTermsText() {
        return "Terms & Conditions\n\n"
                + "1. This app is for personal use only.\n"
                + "2. User must not post illegal or harmful content.\n"
                + "3. We are not responsible for user-generated content.\n"
                + "4. Account misuse may result in permanent ban.\n"
                + "5. Policies may change anytime.\n";
    }

    private String getPrivacyText() {
        return "Privacy Policy\n\n"
                + "1. We do not share your personal data.\n"
                + "2. Email is used only for login and security.\n"
                + "3. User data is protected using Firebase security.\n"
                + "4. We do not sell user information.\n"
                + "5. You may request data deletion anytime.\n";
    }
}
