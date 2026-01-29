package com.example.exmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyActivity extends AppCompatActivity {

    TextView tvPrivacyContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        tvPrivacyContent = findViewById(R.id.tvPrivacyContent);

        tvPrivacyContent.setText(getPrivacyText());
    }

    private String getPrivacyText() {
        return "Privacy Policy\n\n"
                + "1. We respect your privacy and protect your personal data.\n\n"
                + "2. We collect only required information such as email and name.\n\n"
                + "3. We do not sell or share user data with third parties.\n\n"
                + "4. All data is securely stored using Firebase services.\n\n"
                + "5. We do not access contacts, gallery, or personal files.\n\n"
                + "6. User data is used only for authentication and app features.\n\n"
                + "7. You may request account deletion anytime.\n\n"
                + "8. This policy may be updated when required.\n\n"
                + "Effective Date: 01 January 2026";
    }
}
