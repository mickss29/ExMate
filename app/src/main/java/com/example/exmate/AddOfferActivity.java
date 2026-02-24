package com.example.exmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddOfferActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String IMGBB_API_KEY = "c2a57968ac9e3f5cf23caea37d08df2e";

    private EditText etTitle, etSubtitle,
            etCoupon, etDiscount;

    private AutoCompleteTextView etCategory;
    private Button btnSelectExpiry, btnSave;
    private ImageView ivPreview;

    private String selectedExpiryString = "";
    private String uploadedImageUrl = "";
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_offer);

        initViews();
        setupCategoryDropdown();

        ivPreview.setOnClickListener(v -> openGallery());
        btnSelectExpiry.setOnClickListener(v -> pickDateTime());
        btnSave.setOnClickListener(v -> saveOffer());
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etSubtitle = findViewById(R.id.etSubtitle);
        etCategory = findViewById(R.id.etCategory);
        etCoupon = findViewById(R.id.etCoupon);
        etDiscount = findViewById(R.id.etDiscount);


        btnSelectExpiry = findViewById(R.id.btnSelectExpiry);
        btnSave = findViewById(R.id.btnSaveOffer);
        ivPreview = findViewById(R.id.ivPreview);
    }

    private void setupCategoryDropdown() {
        String[] categories = {
                "Food", "Shopping", "Travel", "Recharge", "Entertainment"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        categories);

        etCategory.setAdapter(adapter);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            imageUri = data.getData();
            ivPreview.setImageURI(imageUri);
            uploadToImageBB(imageUri);
        }
    }

    private void uploadToImageBB(Uri uri) {

        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(uri);

            ByteArrayOutputStream baos =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = baos.toByteArray();
            String encodedImage =
                    Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = new FormBody.Builder()
                    .add("key", IMGBB_API_KEY)
                    .add("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(AddOfferActivity.this,
                                    "Upload failed",
                                    Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {

                        if (!response.isSuccessful()) {
                            runOnUiThread(() ->
                                    Toast.makeText(AddOfferActivity.this,
                                            "Upload error",
                                            Toast.LENGTH_SHORT).show());
                            return;
                        }

                        String res = response.body().string();
                        JSONObject jsonObject = new JSONObject(res);

                        uploadedImageUrl =
                                jsonObject.getJSONObject("data")
                                        .getString("url");

                        runOnUiThread(() ->
                                Toast.makeText(AddOfferActivity.this,
                                        "Image Uploaded",
                                        Toast.LENGTH_SHORT).show());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveOffer() {

        String title = etTitle.getText().toString().trim();
        String subtitle = etSubtitle.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String coupon = etCoupon.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();


        if (TextUtils.isEmpty(title) ||
                TextUtils.isEmpty(subtitle) ||
                TextUtils.isEmpty(category) ||
                TextUtils.isEmpty(coupon) ||
                TextUtils.isEmpty(discountStr) ||

                TextUtils.isEmpty(selectedExpiryString) ||
                TextUtils.isEmpty(uploadedImageUrl)) {

            Toast.makeText(this,
                    "Fill all fields & upload image",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int discountPercent;
        try {
            discountPercent = Integer.parseInt(discountStr);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Invalid discount value",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("DiscoverOffers");

        String id = ref.push().getKey();
        if (id == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("category", category);
        map.put("couponCode", coupon);
        map.put("discountPercent", discountPercent);
        map.put("imageUrl", uploadedImageUrl);

        map.put("expiryDateTime", selectedExpiryString);
        map.put("isActive", true);

        ref.child(id).setValue(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Offer Added",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void pickDateTime() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker =
                new DatePickerDialog(this,
                        (view, year, month, day) -> {

                            Calendar selected =
                                    Calendar.getInstance();
                            selected.set(year, month, day);

                            TimePickerDialog timePicker =
                                    new TimePickerDialog(this,
                                            (timeView, hour, minute) -> {

                                                selected.set(Calendar.HOUR_OF_DAY, hour);
                                                selected.set(Calendar.MINUTE, minute);

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
}