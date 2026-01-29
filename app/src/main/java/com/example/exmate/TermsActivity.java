package com.example.exmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TermsActivity extends AppCompatActivity {

    TextView tvTermsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        tvTermsContent = findViewById(R.id.tvTermsContent);

        tvTermsContent.setText(getTermsText());
    }

    private String getTermsText() {
        return "Terms & Conditions\n\n"
                + "1. This application is intended for personal use only.\n\n"
                + "2. Users must not post or share illegal, harmful, or abusive content.\n\n"
                + "3. Any misuse of the app may result in suspension or permanent ban.\n\n"
                + "4. The app owner is not responsible for user-generated content.\n\n"
                + "5. Users are responsible for keeping their login credentials secure.\n\n"
                + "6. We reserve the right to modify these terms at any time.\n\n"
                + "7. Continued use of the app means acceptance of updated terms.\n\n"
                + "Effective Date: 01 January 2026";
    }
}
