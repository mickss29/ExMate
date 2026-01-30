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
import java.text.SimpleDateFormat;
import java.util.*;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private List<AdminUserModel> userList = new ArrayList<>();
    private List<AdminUserModel> filteredList = new ArrayList<>();

    private DatabaseReference usersRef;

    private EditText etSearch;
    private Button btnWeek, btnMonth, btnDate;
    private Button btnClearFilter; // 🔥 NEW: Clear filter button

    private static final int STORAGE_REQ = 101;

    // Current filter state
    private long currentFilterStart = 0;
    private long currentFilterEnd = 0;

    // 🔥 SINGLE SOURCE OF TRUTH FORMAT
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private final SimpleDateFormat dbFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        Toolbar toolbar = findViewById(R.id.usersToolbar);
        setSupportActionBar(toolbar);

        etSearch = findViewById(R.id.etSearchUser);
        btnWeek = findViewById(R.id.btnWeek);
        btnMonth = findViewById(R.id.btnMonth);
        btnDate = findViewById(R.id.btnDate);
        btnClearFilter = findViewById(R.id.btnClearFilter); // Make sure this button exists in XML

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        filteredList = new ArrayList<>(userList); // Start with all users
        adapter = new AdminUserAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabExport = findViewById(R.id.fabExport);
        fabExport.setOnClickListener(v -> showExportBottomSheet());

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        checkPermission();
        loadUsersFromFirebase();

        btnWeek.setOnClickListener(v -> applyWeek());
        btnMonth.setOnClickListener(v -> applyMonth());
        btnDate.setOnClickListener(v -> pickCustomDate());

        // 🔥 NEW: Clear filter button
        btnClearFilter.setOnClickListener(v -> clearAllFilters());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }
        });
    }

    // ================= LOAD USERS (FIXED 100%) =================
    private void loadUsersFromFirebase() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String uid = s.getKey();
                    String name = s.child("name").getValue(String.class);
                    String email = s.child("email").getValue(String.class);
                    String phone = s.child("phone").getValue(String.class);
                    Boolean blocked = s.child("blocked").getValue(Boolean.class);
                    if (blocked == null) blocked = false;

                    // 🔥 FIXED DATE PARSING
                    String createdAt;
                    Object raw = s.child("createdAt").getValue();

                    if (raw instanceof String) {
                        createdAt = (String) raw;
                    } else if (raw instanceof Long) {
                        createdAt = sdf.format(new Date((Long) raw));
                    } else {
                        createdAt = sdf.format(new Date());
                    }

                    // Ensure consistent format
                    try {
                        Date date = parseDateFlexible(createdAt);
                        createdAt = sdf.format(date);
                    } catch (Exception e) {
                        createdAt = sdf.format(new Date());
                    }

                    AdminUserModel u = new AdminUserModel(
                            uid,
                            safe(name),
                            safe(email),
                            safe(phone),
                            blocked,
                            createdAt
                    );

                    userList.add(u);
                }

                // 🔥 NEWEST → OLDEST
                Collections.sort(userList, (a, b) ->
                        Long.compare(parseDateFlexible(b.getCreatedAt()).getTime(),
                                parseDateFlexible(a.getCreatedAt()).getTime())
                );

                // 🔥 IMPORTANT: Update filtered list
                applyCurrentFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= SEARCH (FIXED) =================
    private void searchUsers(String q) {
        List<AdminUserModel> tempList;

        // First apply current date filter
        if (currentFilterStart != 0 && currentFilterEnd != 0) {
            tempList = new ArrayList<>();
            for (AdminUserModel u : userList) {
                long t = parseDateFlexible(u.getCreatedAt()).getTime();
                if (t >= currentFilterStart && t <= currentFilterEnd) {
                    tempList.add(u);
                }
            }
        } else {
            tempList = new ArrayList<>(userList);
        }

        // Then apply search
        if (q.isEmpty()) {
            filteredList.clear();
            filteredList.addAll(tempList);
        } else {
            filteredList.clear();
            q = q.toLowerCase();
            for (AdminUserModel u : tempList) {
                if (u.getName().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q)
                        || u.getPhone().toLowerCase().contains(q)) {
                    filteredList.add(u);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ================= FILTERS (FIXED) =================
    private void applyWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long end = System.currentTimeMillis();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        long start = cal.getTimeInMillis();

        currentFilterStart = start;
        currentFilterEnd = end;

        filterByRange(start, end);
        Toast.makeText(this, "Showing users from last 7 days", Toast.LENGTH_SHORT).show();
    }

    private void applyMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();

        currentFilterStart = start;
        currentFilterEnd = end;

        filterByRange(start, end);
        Toast.makeText(this, "Showing users from this month", Toast.LENGTH_SHORT).show();
    }

    private void pickCustomDate() {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar from = Calendar.getInstance();
            from.set(y, m, d);
            from.set(Calendar.HOUR_OF_DAY, 0);
            from.set(Calendar.MINUTE, 0);
            from.set(Calendar.SECOND, 0);
            from.set(Calendar.MILLISECOND, 0);

            new DatePickerDialog(this, (v2, y2, m2, d2) -> {
                Calendar to = Calendar.getInstance();
                to.set(y2, m2, d2);
                to.set(Calendar.HOUR_OF_DAY, 23);
                to.set(Calendar.MINUTE, 59);
                to.set(Calendar.SECOND, 59);
                to.set(Calendar.MILLISECOND, 999);

                if (to.before(from)) {
                    Toast.makeText(this,
                            "End date must be after start date",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                currentFilterStart = from.getTimeInMillis();
                currentFilterEnd = to.getTimeInMillis();

                filterByRange(from.getTimeInMillis(), to.getTimeInMillis());
                Toast.makeText(this,
                        "Showing users from selected date range",
                        Toast.LENGTH_SHORT).show();

            }, y, m, d).show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void filterByRange(long start, long end) {
        filteredList.clear();
        for (AdminUserModel u : userList) {
            long t = parseDateFlexible(u.getCreatedAt()).getTime();
            if (t >= start && t <= end) {
                filteredList.add(u);
            }
        }

        // Apply search if any
        String searchText = etSearch.getText().toString().trim();
        if (!searchText.isEmpty()) {
            searchUsers(searchText);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void applyCurrentFilter() {
        if (currentFilterStart != 0 && currentFilterEnd != 0) {
            filterByRange(currentFilterStart, currentFilterEnd);
        } else {
            filteredList.clear();
            filteredList.addAll(userList);

            // Apply search if any
            String searchText = etSearch.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchUsers(searchText);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void clearAllFilters() {
        etSearch.setText("");
        currentFilterStart = 0;
        currentFilterEnd = 0;

        filteredList.clear();
        filteredList.addAll(userList);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "All filters cleared", Toast.LENGTH_SHORT).show();
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
            File file = new File(dir, "Users_" + System.currentTimeMillis() + ".csv");

            FileWriter w = new FileWriter(file);
            w.append("Date,Name,Email,Phone,Status\n");

            for (AdminUserModel u : list) {
                w.append("\"").append(u.getCreatedAt()).append("\",")
                        .append("\"").append(u.getName()).append("\",")
                        .append("\"").append(u.getEmail()).append("\",")
                        .append("\"").append(u.getPhone()).append("\",")
                        .append("\"").append(u.isBlocked() ? "Blocked" : "Active").append("\"")
                        .append("\n");
            }
            w.close();
            openFile(file, "text/csv");
            Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPDF(List<AdminUserModel> list) {
        try {
            PdfDocument pdf = new PdfDocument();
            Paint title = new Paint();
            Paint header = new Paint();
            Paint text = new Paint();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            PdfDocument.Page page = pdf.startPage(info);
            Canvas c = page.getCanvas();
            int y = 40;

            title.setTextSize(18);
            title.setFakeBoldText(true);
            c.drawText("ExMate Users Report", 40, y, title);

            // Add date
            title.setTextSize(12);
            title.setFakeBoldText(false);
            c.drawText("Generated: " + sdf.format(new Date()), 40, y + 20, title);
            c.drawText("Total Users: " + list.size(), 400, y + 20, title);

            y += 50;
            header.setTextSize(11);
            header.setFakeBoldText(true);

            c.drawText("Date & Time", 40, y, header);
            c.drawText("Name", 170, y, header);
            c.drawText("Email", 300, y, header);
            c.drawText("Mobile", 460, y, header);
            c.drawText("Status", 530, y, header);

            y += 15;
            c.drawLine(40, y, 570, y, header);
            y += 10;

            header.setFakeBoldText(false);
            text.setTextSize(10);

            for (AdminUserModel u : list) {
                if (y > 780) {
                    pdf.finishPage(page);
                    page = pdf.startPage(info);
                    c = page.getCanvas();
                    y = 40;
                }

                c.drawText(u.getCreatedAt(), 40, y, text);
                c.drawText(trim(u.getName(), 18), 170, y, text);
                c.drawText(trim(u.getEmail(), 25), 300, y, text);
                c.drawText(trim(u.getPhone(), 14), 460, y, text);
                c.drawText(u.isBlocked() ? "Blocked" : "Active", 530, y, text);
                y += 18;
            }

            pdf.finishPage(page);
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "ExMate_Users_Report_" + System.currentTimeMillis() + ".pdf");
            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            openFile(file, "application/pdf");
            Toast.makeText(this, "PDF exported successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ================= HELPERS =================
    private Date parseDateFlexible(String date) {
        try {
            return sdf.parse(date);
        } catch (Exception e1) {
            try {
                return dbFormat.parse(date);
            } catch (Exception e2) {
                try {
                    // Try with other common formats
                    SimpleDateFormat[] formats = {
                            new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                            new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH),
                            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
                            new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
                    };

                    for (SimpleDateFormat format : formats) {
                        try {
                            return format.parse(date);
                        } catch (Exception ignored) {}
                    }
                    return new Date(); // Default to current date
                } catch (Exception e3) {
                    return new Date();
                }
            }
        }
    }

    private String safe(String v) {
        return (v == null || v.trim().isEmpty()) ? "-" : v.trim();
    }

    private String trim(String t, int max) {
        if (t == null || t.equals("-")) return "-";
        return t.length() > max ? t.substring(0, max - 3) + "..." : t;
    }

    private void openFile(File file, String type) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", file);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, type);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Open file"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}