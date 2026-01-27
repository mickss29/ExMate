package com.example.exmate;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.*;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private List<AdminUserModel> userList = new ArrayList<>();
    private List<AdminUserModel> filteredList = new ArrayList<>();

    private DatabaseReference usersRef;

    private EditText etSearch;
    private Button btnWeek, btnMonth, btnDate;

    private static final int STORAGE_REQ = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.usersToolbar);
        setSupportActionBar(toolbar);

        // UI
        etSearch = findViewById(R.id.etSearchUser);
        btnWeek = findViewById(R.id.btnWeek);
        btnMonth = findViewById(R.id.btnMonth);
        btnDate = findViewById(R.id.btnDate);

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabExport = findViewById(R.id.fabExport);
        fabExport.setOnClickListener(v -> showExportBottomSheet());

        // Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        checkPermission();
        loadUsersFromFirebase();

        // Filters
        btnWeek.setOnClickListener(v -> applyWeek());
        btnMonth.setOnClickListener(v -> applyMonth());
        btnDate.setOnClickListener(v -> pickCustomDate());

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }
        });
    }

    // ================= LOAD USERS =================
    private void loadUsersFromFirebase() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                userList.clear();
                filteredList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {

                    String uid = s.getKey();
                    String name = s.child("name").getValue(String.class);
                    String email = s.child("email").getValue(String.class);
                    String phone = s.child("phone").getValue(String.class);
                    Boolean blocked = s.child("blocked").getValue(Boolean.class);
                    Long createdAt = s.child("createdAt").getValue(Long.class);

                    if (blocked == null) blocked = false;
                    if (createdAt == null) createdAt = 0L;

                    AdminUserModel u = new AdminUserModel(
                            uid, safe(name), safe(email), safe(phone), blocked
                    );
                    u.setCreatedAt(createdAt);

                    userList.add(u);
                }

                filteredList.addAll(userList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this,
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= SEARCH =================
    private void searchUsers(String q) {
        filteredList.clear();
        q = q.toLowerCase();

        for (AdminUserModel u : userList) {
            if (u.getName().toLowerCase().contains(q)
                    || u.getEmail().toLowerCase().contains(q)
                    || u.getPhone().toLowerCase().contains(q)) {
                filteredList.add(u);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ================= FILTERS =================
    private void applyWeek() {
        long now = System.currentTimeMillis();
        filterByRange(now - 7L * 24 * 60 * 60 * 1000, now);
    }

    private void applyMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        filterByRange(c.getTimeInMillis(), System.currentTimeMillis());
    }

    private void pickCustomDate() {
        Calendar cal = Calendar.getInstance();

        // FROM DATE
        new DatePickerDialog(this, (v, y, m, d) -> {

            Calendar from = Calendar.getInstance();
            from.set(y, m, d, 0, 0, 0);
            long fromTime = from.getTimeInMillis();

            // TO DATE
            new DatePickerDialog(this, (v2, y2, m2, d2) -> {

                Calendar to = Calendar.getInstance();
                to.set(y2, m2, d2, 23, 59, 59);
                long toTime = to.getTimeInMillis();

                if (toTime < fromTime) {
                    Toast.makeText(this,
                            "End date must be after start date",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                filterByRange(fromTime, toTime);

            }, y, m, d).show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void filterByRange(long start, long end) {
        filteredList.clear();
        for (AdminUserModel u : userList) {
            if (u.getCreatedAt() >= start && u.getCreatedAt() <= end) {
                filteredList.add(u);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ================= EXPORT =================
    private void showExportBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_export, null);
        dialog.setContentView(v);

        v.findViewById(R.id.btnExportPDF).setOnClickListener(x -> {
            exportPDF(filteredList);
            dialog.dismiss();
        });

        v.findViewById(R.id.btnExportCSV).setOnClickListener(x -> {
            exportCSV(filteredList);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void exportCSV(List<AdminUserModel> list) {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(dir, "Filtered_Users.csv");

            FileWriter w = new FileWriter(file);
            w.append("Name,Email,Phone,Status\n");

            for (AdminUserModel u : list) {
                w.append(u.getName()).append(",")
                        .append(u.getEmail()).append(",")
                        .append(u.getPhone()).append(",")
                        .append(u.isBlocked() ? "Blocked" : "Active")
                        .append("\n");
            }
            w.close();
            openFile(file, "text/csv");

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPDF(List<AdminUserModel> list) {
        try {
            PdfDocument pdf = new PdfDocument();
            Paint titlePaint = new Paint();
            Paint textPaint = new Paint();
            Paint headerPaint = new Paint();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            PdfDocument.Page page = pdf.startPage(info);
            Canvas c = page.getCanvas();

            int y = 40;

            // Title
            titlePaint.setTextSize(18);
            titlePaint.setFakeBoldText(true);
            c.drawText("ExMate - Users Detailed Report", 40, y, titlePaint);

            y += 25;
            textPaint.setTextSize(11);
            c.drawText("Total Users: " + list.size(), 40, y, textPaint);

            y += 20;

            // Header
            headerPaint.setFakeBoldText(true);
            c.drawText("Name", 40, y, headerPaint);
            c.drawText("Email", 160, y, headerPaint);
            c.drawText("Mobile", 320, y, headerPaint);
            c.drawText("Status", 460, y, headerPaint);

            y += 10;
            c.drawLine(40, y, 555, y, textPaint);
            y += 18;

            headerPaint.setFakeBoldText(false);

            for (AdminUserModel u : list) {

                if (y > 780) {
                    pdf.finishPage(page);
                    page = pdf.startPage(info);
                    c = page.getCanvas();
                    y = 40;
                }

                c.drawText(trim(u.getName(), 18), 40, y, textPaint);
                c.drawText(trim(u.getEmail(), 25), 160, y, textPaint);
                c.drawText(trim(u.getPhone(), 14), 320, y, textPaint);
                c.drawText(u.isBlocked() ? "Blocked" : "Active", 460, y, textPaint);

                y += 18;
            }

            pdf.finishPage(page);

            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(dir, "ExMate_Full_User_Report.pdf");

            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    // ================= HELPERS =================
    private String safe(String v) {
        return v == null ? "-" : v;
    }

    private void openFile(File file, String type) {
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", file);

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Open file"));
    }

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
    private String trim(String text, int max) {
        if (text == null) return "-";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

}
