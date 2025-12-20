package com.example.exmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private List<AdminUserModel> userList;
    private DatabaseReference usersRef;

    private static final int STORAGE_REQ = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        Toolbar toolbar = findViewById(R.id.usersToolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new AdminUserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        checkPermission();
        loadUsersFromFirebase();

        FloatingActionButton fabExport = findViewById(R.id.fabExport);
        fabExport.setOnClickListener(v -> showExportBottomSheet());
    }

    // ================= SAFE STRING =================
    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String trim(String text, int max) {
        if (text == null) return "-";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    // ================= LOAD USERS =================
    private void loadUsersFromFirebase() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                userList.clear();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    String name = userSnap.child("name").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String phone = userSnap.child("phone").getValue(String.class);
                    Boolean blocked = userSnap.child("blocked").getValue(Boolean.class);

                    if (blocked == null) blocked = false;

                    userList.add(new AdminUserModel(
                            uid,
                            safe(name),
                            safe(email),
                            safe(phone),
                            blocked
                    ));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= EXPORT OPTIONS =================
    private void showExportBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_export, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnExportPDF).setOnClickListener(v -> {
            exportProfessionalPDF();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnExportCSV).setOnClickListener(v -> {
            exportCSV();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ================= CSV EXPORT (AUTO OPEN) =================
    private void exportCSV() {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();

            File file = new File(dir, "ExMate_Users.csv");
            FileWriter writer = new FileWriter(file);

            writer.append("UID,Name,Email,Phone,Status\n");

            for (AdminUserModel user : userList) {
                writer.append(safe(user.getUid())).append(",")
                        .append(safe(user.getName())).append(",")
                        .append(safe(user.getEmail())).append(",")
                        .append(safe(user.getPhone())).append(",")
                        .append(user.isBlocked() ? "Blocked" : "Active")
                        .append("\n");
            }

            writer.flush();
            writer.close();

            openFile(file, "text/csv");

        } catch (Exception e) {
            Toast.makeText(this, "CSV error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ================= PDF EXPORT (AUTO OPEN + SIGNATURE) =================
    private void exportProfessionalPDF() {
        try {
            PdfDocument pdf = new PdfDocument();

            Paint titlePaint = new Paint();
            Paint textPaint = new Paint();
            Paint headerPaint = new Paint();
            Paint footerPaint = new Paint();

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            int pageNumber = 1;
            int totalUsers = userList.size();
            int blockedUsers = 0;

            for (AdminUserModel u : userList) if (u.isBlocked()) blockedUsers++;

            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            int y = 50;

            titlePaint.setTextSize(22);
            titlePaint.setFakeBoldText(true);
            canvas.drawText("ExMate – Users Report", 40, y, titlePaint);

            y += 28;
            textPaint.setTextSize(11);
            String generatedAt = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault()
            ).format(new Date());

            canvas.drawText("Generated on: " + generatedAt, 40, y, textPaint);

            y += 25;
            headerPaint.setFakeBoldText(true);
            canvas.drawText("Summary", 40, y, headerPaint);
            y += 15;

            canvas.drawText("Total Users: " + totalUsers, 40, y, textPaint);
            canvas.drawText("Blocked Users: " + blockedUsers, 200, y, textPaint);

            y += 20;
            canvas.drawLine(40, y, 555, y, textPaint);
            y += 20;

            headerPaint.setFakeBoldText(true);
            canvas.drawText("Name", 40, y, headerPaint);
            canvas.drawText("Email", 150, y, headerPaint);
            canvas.drawText("Phone", 320, y, headerPaint);
            canvas.drawText("Status", 460, y, headerPaint);

            y += 15;
            canvas.drawLine(40, y, 555, y, textPaint);
            y += 15;
            headerPaint.setFakeBoldText(false);

            for (AdminUserModel user : userList) {

                if (y > 760) {
                    drawFooter(canvas, footerPaint, pageNumber);
                    pdf.finishPage(page);
                    pageNumber++;
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }

                canvas.drawText(trim(user.getName(), 16), 40, y, textPaint);
                canvas.drawText(trim(user.getEmail(), 26), 150, y, textPaint);
                canvas.drawText(trim(user.getPhone(), 14), 320, y, textPaint);
                canvas.drawText(user.isBlocked() ? "Blocked" : "Active", 460, y, textPaint);
                y += 18;
            }

            // ===== DIGITAL SIGNATURE =====
            y += 30;
            canvas.drawLine(40, y, 300, y, textPaint);
            y += 16;
            canvas.drawText("Digitally signed by ExMate Admin", 40, y, textPaint);
            y += 14;
            canvas.drawText("Signed on: " + generatedAt, 40, y, textPaint);

            drawFooter(canvas, footerPaint, pageNumber);
            pdf.finishPage(page);

            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();

            File file = new File(dir, "ExMate_Users_Report.pdf");
            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ================= FOOTER =================
    private void drawFooter(Canvas canvas, Paint paint, int pageNumber) {
        paint.setTextSize(10);
        canvas.drawLine(40, 810, 555, 810, paint);
        canvas.drawText("Generated by ExMate Admin Panel", 180, 828, paint);
        canvas.drawText("Page " + pageNumber, 480, 828, paint);
    }

    // ================= OPEN FILE SAFELY =================
    private void openFile(File file, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Open file"));

        } catch (Exception e) {
            Toast.makeText(this,
                    "No app found to open this file",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ================= PERMISSION =================
    private void checkPermission() {
        if (Build.VERSION.SDK_INT < 29 &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_REQ);
        }
    }
}
