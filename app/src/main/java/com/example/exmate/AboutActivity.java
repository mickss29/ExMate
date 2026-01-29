package com.example.exmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    TextView tvAboutContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        tvAboutContent = findViewById(R.id.tvAboutContent);
        tvAboutContent.setText(getAboutText());
    }

    private String getAboutText() {
        return "About ExMate\n\n"
                + "ExMate is a smart expense management app designed to help users "
                + "track income, expenses, and savings easily.\n\n"
                + "Our goal is to make money management simple, fast, and stress-free.\n\n"
                + "Key Features:\n"
                + "• Track daily income and expenses\n"
                + "• Simple and clean interface\n"
                + "• Secure data storage\n"
                + "• Future premium features\n\n"
                + "ExMate is built with love to help users stay financially organized.\n\n"
                + "Version: 1.0\n"
                + "© 2026 ExMate. All rights reserved.";
    }
}
