package com.example.exmate;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

public class AddCategoryActivity extends AppCompatActivity {

    private EditText etCategoryName, etCategoryLimit;
    private MaterialButton btnSaveCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        initViews();
        setupClicks();
    }

    private void initViews() {
        etCategoryName   = findViewById(R.id.etCategoryName);
        etCategoryLimit  = findViewById(R.id.etCategoryLimit);
        btnSaveCategory  = findViewById(R.id.btnSaveCategory);
    }

    private void setupClicks() {
        btnSaveCategory.setOnClickListener(v -> {
            Toast.makeText(this, "Saving soon...", Toast.LENGTH_SHORT).show();
        });
    }
}
