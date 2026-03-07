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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageUsersActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvUserCount, tvActiveFilter, btnClearFilter;
    private LinearLayout activeFilterBar, emptyState;
    private MaterialCardView btnBack, fabExport;
    private MaterialCardView btnWeek, btnMonth, btnDate;

    // ── Data ───────────────────────────────────────────────────────────────
    private AdminUserAdapter adapter;
    private final List<AdminUserModel> userList     = new ArrayList<>();
    private final List<AdminUserModel> filteredList = new ArrayList<>();
    private DatabaseReference usersRef;

    // ── Filter state ───────────────────────────────────────────────────────
    private long currentFilterStart = 0;
    private long currentFilterEnd   = 0;

    private static final int STORAGE_REQ = 101;

    // ── Date formats ───────────────────────────────────────────────────────
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private final SimpleDateFormat dbFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    // ── Tab colors ─────────────────────────────────────────────────────────
    private static final int COLOR_TAB_ACTIVE_BG      = 0xFF0D1F4A;
    private static final int COLOR_TAB_INACTIVE_BG    = 0xFF0A1228;
    private static final int COLOR_TAB_ACTIVE_STROKE  = 0xFF1A3BCC;
    private static final int COLOR_TAB_INACTIVE_STROKE = 0xFF131F3A;
    private static final int COLOR_TAB_ACTIVE_TEXT    = 0xFF5B8BFF;
    private static final int COLOR_TAB_INACTIVE_TEXT  = 0xFF3D5A80;

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        initViews();
        setupRecyclerView();
        setClickListeners();
        checkPermission();
        loadUsersFromFirebase();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════

    private void initViews() {
        btnBack         = findViewById(R.id.btnBack);
        fabExport       = findViewById(R.id.fabExport);
        tvUserCount     = findViewById(R.id.tvUserCount);
        etSearch        = findViewById(R.id.etSearchUser);
        btnWeek         = findViewById(R.id.btnWeek);
        btnMonth        = findViewById(R.id.btnMonth);
        btnDate         = findViewById(R.id.btnDate);
        activeFilterBar = findViewById(R.id.activeFilterBar);
        tvActiveFilter  = findViewById(R.id.tvActiveFilter);
        btnClearFilter  = findViewById(R.id.btnClearFilter);
        recyclerView    = findViewById(R.id.recyclerUsers);
        emptyState      = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        adapter = new AdminUserAdapter(this, filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        fabExport.setOnClickListener(v -> showExportBottomSheet());

        btnWeek.setOnClickListener(v  -> applyWeek());
        btnMonth.setOnClickListener(v -> applyMonth());
        btnDate.setOnClickListener(v  -> pickCustomDate());

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

    // ══════════════════════════════════════════════════════════════════════
    // Firebase
    // ══════════════════════════════════════════════════════════════════════

    private void loadUsersFromFirebase() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        tvUserCount.setText("Loading...");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String uid     = s.getKey();
                    String name    = s.child("name").getValue(String.class);
                    String email   = s.child("email").getValue(String.class);
                    String phone   = s.child("phone").getValue(String.class);
                    Boolean blocked = s.child("blocked").getValue(Boolean.class);
                    if (blocked == null) blocked = false;

                    // Date parsing — handles String or Long timestamps
                    String createdAt;
                    Object raw = s.child("createdAt").getValue();
                    if (raw instanceof String) {
                        createdAt = (String) raw;
                    } else if (raw instanceof Long) {
                        createdAt = sdf.format(new Date((Long) raw));
                    } else {
                        createdAt = sdf.format(new Date());
                    }

                    try {
                        Date date = parseDateFlexible(createdAt);
                        createdAt = sdf.format(date);
                    } catch (Exception e) {
                        createdAt = sdf.format(new Date());
                    }

                    userList.add(new AdminUserModel(
                            uid, safe(name), safe(email), safe(phone), blocked, createdAt));
                }

                // Newest → oldest
                Collections.sort(userList, (a, b) ->
                        Long.compare(
                                parseDateFlexible(b.getCreatedAt()).getTime(),
                                parseDateFlexible(a.getCreatedAt()).getTime()
                        ));

                tvUserCount.setText(userList.size() + " registered users");
                applyCurrentFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUserCount.setText("Failed to load");
                Toast.makeText(ManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Search
    // ══════════════════════════════════════════════════════════════════════

    private void searchUsers(String q) {
        List<AdminUserModel> base;

        if (currentFilterStart != 0 && currentFilterEnd != 0) {
            base = new ArrayList<>();
            for (AdminUserModel u : userList) {
                long t = parseDateFlexible(u.getCreatedAt()).getTime();
                if (t >= currentFilterStart && t <= currentFilterEnd) base.add(u);
            }
        } else {
            base = new ArrayList<>(userList);
        }

        filteredList.clear();
        if (q.isEmpty()) {
            filteredList.addAll(base);
        } else {
            String lq = q.toLowerCase();
            for (AdminUserModel u : base) {
                if (u.getName().toLowerCase().contains(lq)
                        || u.getEmail().toLowerCase().contains(lq)
                        || u.getPhone().toLowerCase().contains(lq)) {
                    filteredList.add(u);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Filters
    // ══════════════════════════════════════════════════════════════════════

    private void applyWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -7);

        currentFilterStart = cal.getTimeInMillis();
        currentFilterEnd   = System.currentTimeMillis();

        setTabActive(btnWeek);
        showFilterBar("Showing: Last 7 days");
        filterByRange(currentFilterStart, currentFilterEnd);
    }

    private void applyMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        currentFilterStart = cal.getTimeInMillis();
        currentFilterEnd   = System.currentTimeMillis();

        setTabActive(btnMonth);
        showFilterBar("Showing: This month");
        filterByRange(currentFilterStart, currentFilterEnd);
    }

    private void pickCustomDate() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar from = Calendar.getInstance();
            from.set(y, m, d, 0, 0, 0);
            from.set(Calendar.MILLISECOND, 0);

            new DatePickerDialog(this, (v2, y2, m2, d2) -> {
                Calendar to = Calendar.getInstance();
                to.set(y2, m2, d2, 23, 59, 59);
                to.set(Calendar.MILLISECOND, 999);

                if (to.before(from)) {
                    Toast.makeText(this,
                            "End date must be after start date", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentFilterStart = from.getTimeInMillis();
                currentFilterEnd   = to.getTimeInMillis();

                setTabActive(btnDate);
                showFilterBar("Showing: Custom range");
                filterByRange(currentFilterStart, currentFilterEnd);

            }, y, m, d).show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void filterByRange(long start, long end) {
        filteredList.clear();
        for (AdminUserModel u : userList) {
            long t = parseDateFlexible(u.getCreatedAt()).getTime();
            if (t >= start && t <= end) filteredList.add(u);
        }

        String searchText = etSearch.getText().toString().trim();
        if (!searchText.isEmpty()) {
            searchUsers(searchText);
        } else {
            adapter.notifyDataSetChanged();
            updateEmptyState();
        }
    }

    private void applyCurrentFilter() {
        if (currentFilterStart != 0 && currentFilterEnd != 0) {
            filterByRange(currentFilterStart, currentFilterEnd);
        } else {
            filteredList.clear();
            filteredList.addAll(userList);
            String searchText = etSearch.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchUsers(searchText);
            } else {
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        }
    }

    private void clearAllFilters() {
        etSearch.setText("");
        currentFilterStart = 0;
        currentFilterEnd   = 0;

        resetAllTabs();
        activeFilterBar.setVisibility(View.GONE);

        filteredList.clear();
        filteredList.addAll(userList);
        adapter.notifyDataSetChanged();
        updateEmptyState();

        Toast.makeText(this, "All filters cleared", Toast.LENGTH_SHORT).show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Tab highlight helpers
    // ══════════════════════════════════════════════════════════════════════

    private void setTabActive(MaterialCardView activeTab) {
        MaterialCardView[] tabs = {btnWeek, btnMonth, btnDate};
        for (MaterialCardView tab : tabs) {
            boolean active = tab == activeTab;
            tab.setCardBackgroundColor(active ? COLOR_TAB_ACTIVE_BG    : COLOR_TAB_INACTIVE_BG);
            tab.setStrokeColor        (active ? COLOR_TAB_ACTIVE_STROKE : COLOR_TAB_INACTIVE_STROKE);
            View inner = tab.getChildAt(0);
            if (inner instanceof android.widget.LinearLayout) {
                View child = ((android.widget.LinearLayout) inner).getChildAt(0);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(
                            active ? COLOR_TAB_ACTIVE_TEXT : COLOR_TAB_INACTIVE_TEXT);
                }
            }
        }
    }

    private void resetAllTabs() {
        for (MaterialCardView tab : new MaterialCardView[]{btnWeek, btnMonth, btnDate}) {
            tab.setCardBackgroundColor(COLOR_TAB_INACTIVE_BG);
            tab.setStrokeColor(COLOR_TAB_INACTIVE_STROKE);
            View inner = tab.getChildAt(0);
            if (inner instanceof android.widget.LinearLayout) {
                View child = ((android.widget.LinearLayout) inner).getChildAt(0);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(COLOR_TAB_INACTIVE_TEXT);
                }
            }
        }
    }

    private void showFilterBar(String label) {
        activeFilterBar.setVisibility(View.VISIBLE);
        tvActiveFilter.setText(label);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Empty state
    // ══════════════════════════════════════════════════════════════════════

    private void updateEmptyState() {
        boolean empty = filteredList.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE    : View.VISIBLE);
        emptyState.setVisibility  (empty ? View.VISIBLE : View.GONE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Export
    // ══════════════════════════════════════════════════════════════════════

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
            File dir  = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
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
            PdfDocument pdf  = new PdfDocument();
            Paint titlePaint = new Paint();
            Paint header     = new Paint();
            Paint text       = new Paint();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            Canvas c = page.getCanvas();
            int y = 40;

            titlePaint.setTextSize(18);
            titlePaint.setFakeBoldText(true);
            c.drawText("ExMate Users Report", 40, y, titlePaint);

            titlePaint.setTextSize(12);
            titlePaint.setFakeBoldText(false);
            c.drawText("Generated: " + sdf.format(new Date()), 40, y + 20, titlePaint);
            c.drawText("Total Users: " + list.size(), 400, y + 20, titlePaint);

            y += 50;
            header.setTextSize(11);
            header.setFakeBoldText(true);
            c.drawText("Date & Time", 40, y, header);
            c.drawText("Name",        170, y, header);
            c.drawText("Email",       300, y, header);
            c.drawText("Mobile",      460, y, header);
            c.drawText("Status",      530, y, header);

            y += 15;
            c.drawLine(40, y, 570, y, header);
            y += 10;

            text.setTextSize(10);

            for (AdminUserModel u : list) {
                if (y > 780) {
                    pdf.finishPage(page);
                    page = pdf.startPage(info);
                    c = page.getCanvas();
                    y = 40;
                }
                c.drawText(u.getCreatedAt(),          40,  y, text);
                c.drawText(trim(u.getName(),  18),    170, y, text);
                c.drawText(trim(u.getEmail(), 25),    300, y, text);
                c.drawText(trim(u.getPhone(), 14),    460, y, text);
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

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private Date parseDateFlexible(String date) {
        try { return sdf.parse(date); } catch (Exception ignored) {}
        try { return dbFormat.parse(date); } catch (Exception ignored) {}
        SimpleDateFormat[] formats = {
                new SimpleDateFormat("dd/MM/yyyy",  Locale.ENGLISH),
                new SimpleDateFormat("MM/dd/yyyy",  Locale.ENGLISH),
                new SimpleDateFormat("yyyy-MM-dd",  Locale.ENGLISH),
                new SimpleDateFormat("dd-MM-yyyy",  Locale.ENGLISH)
        };
        for (SimpleDateFormat fmt : formats) {
            try { return fmt.parse(date); } catch (Exception ignored) {}
        }
        return new Date();
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
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == STORAGE_REQ) {
            Toast.makeText(this,
                    results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
                            ? "Storage permission granted"
                            : "Storage permission denied",
                    Toast.LENGTH_SHORT).show();
        }
    }
}