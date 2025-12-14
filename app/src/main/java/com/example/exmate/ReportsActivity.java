package com.example.exmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ReportsActivity extends AppCompatActivity {

    Toolbar reportsToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        reportsToolbar = findViewById(R.id.reportsToolbar);
        setSupportActionBar(reportsToolbar);

        // Back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        reportsToolbar.setNavigationOnClickListener(v -> finish());
    }
}
