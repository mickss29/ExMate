package com.example.exmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddOfferActivity extends AppCompatActivity {

    private EditText etTitle, etSubtitle, etCategory,
            etCoupon, etDiscount, etImageUrl, etAccentColor;

    private Button btnSelectExpiry, btnSave;

    private String selectedExpiryString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_offer);

        etTitle = findViewById(R.id.etTitle);
        etSubtitle = findViewById(R.id.etSubtitle);
        etCategory = findViewById(R.id.etCategory);
        etCoupon = findViewById(R.id.etCoupon);
        etDiscount = findViewById(R.id.etDiscount);
        etImageUrl = findViewById(R.id.etImageUrl);
        etAccentColor = findViewById(R.id.etAccentColor);

        btnSelectExpiry = findViewById(R.id.btnSelectExpiry);
        btnSave = findViewById(R.id.btnSaveOffer);

        btnSelectExpiry.setOnClickListener(v -> pickDateTime());

        btnSave.setOnClickListener(v -> saveOffer());
    }

    private void pickDateTime() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (timeView, hour, minute) -> {

                                selected.set(Calendar.HOUR_OF_DAY, hour);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);

                                SimpleDateFormat sdf =
                                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                                                Locale.getDefault());

                                selectedExpiryString =
                                        sdf.format(selected.getTime());

                                btnSelectExpiry.setText(selectedExpiryString);

                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true);

                    timePicker.show();

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void saveOffer() {

        String title = etTitle.getText().toString().trim();
        String subtitle = etSubtitle.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String coupon = etCoupon.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();
        String accentColor = etAccentColor.getText().toString().trim();

        if (TextUtils.isEmpty(title) ||
                TextUtils.isEmpty(subtitle) ||
                TextUtils.isEmpty(category) ||
                TextUtils.isEmpty(coupon) ||
                TextUtils.isEmpty(discountStr) ||
                TextUtils.isEmpty(imageUrl) ||
                TextUtils.isEmpty(accentColor) ||
                TextUtils.isEmpty(selectedExpiryString)) {

            Toast.makeText(this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int discountPercent = Integer.parseInt(discountStr);

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault());

        String createdTime = sdf.format(Calendar.getInstance().getTime());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("DiscoverOffers");

        String id = ref.push().getKey();

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("category", category);
        map.put("couponCode", coupon);
        map.put("discountPercent", discountPercent);
        map.put("imageUrl", imageUrl);
        map.put("accentColor", accentColor);
        map.put("expiryDateTime", selectedExpiryString);
        map.put("createdAt", createdTime);
        map.put("isActive", true);

        ref.child(id).setValue(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Offer Added Successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
