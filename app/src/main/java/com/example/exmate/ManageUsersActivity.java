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

    private static final int STORAGE_REQ = 101;

    // 🔥 SINGLE SOURCE OF TRUTH FORMAT
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

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

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                filteredList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {

                    String uid = s.getKey();
                    String name = s.child("name").getValue(String.class);
                    String email = s.child("email").getValue(String.class);
                    String phone = s.child("phone").getValue(String.class);
                    Boolean blocked = s.child("blocked").getValue(Boolean.class);
                    if (blocked == null) blocked = false;

                    // 🔥 SAFE DATE READ (String + Long + null)
                    String createdAt;
                    Object raw = s.child("createdAt").getValue();

                    if (raw instanceof String) {
                        createdAt = (String) raw;
                    } else if (raw instanceof Long) {
                        createdAt = sdf.format(new Date((Long) raw));
                        s.getRef().child("createdAt").setValue(createdAt); // migrate
                    } else {
                        createdAt = sdf.format(new Date());
                        s.getRef().child("createdAt").setValue(createdAt);
                    }

                    // 🔥 normalize format (THIS FIXES FILTER)
                    long millis = parseDate(createdAt);
                    createdAt = sdf.format(new Date(millis));
                    s.getRef().child("createdAt").setValue(createdAt);

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
                        Long.compare(parseDate(b.getCreatedAt()), parseDate(a.getCreatedAt()))
                );

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

        new DatePickerDialog(this, (v, y, m, d) -> {

            Calendar from = Calendar.getInstance();
            from.set(y, m, d, 0, 0, 0);

            new DatePickerDialog(this, (v2, y2, m2, d2) -> {

                Calendar to = Calendar.getInstance();
                to.set(y2, m2, d2, 23, 59, 59);

                if (to.before(from)) {
                    Toast.makeText(this,
                            "End date must be after start date",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                filterByRange(from.getTimeInMillis(), to.getTimeInMillis());

            }, y, m, d).show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void filterByRange(long start, long end) {
        filteredList.clear();
        for (AdminUserModel u : userList) {
            long t = parseDate(u.getCreatedAt());
            if (t >= start && t <= end) {
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
            File file = new File(dir, "Users.csv");

            FileWriter w = new FileWriter(file);
            w.append("Date,Name,Email,Phone,Status\n");

            for (AdminUserModel u : list) {
                w.append(u.getCreatedAt()).append(",")
                        .append(u.getName()).append(",")
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

            y += 25;
            header.setFakeBoldText(true);

            c.drawText("Date & Time", 40, y, header);
            c.drawText("Name", 170, y, header);
            c.drawText("Email", 300, y, header);
            c.drawText("Mobile", 460, y, header);

            y += 18;
            header.setFakeBoldText(false);

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
                y += 18;
            }

            pdf.finishPage(page);
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "ExMate_Users_Report.pdf");
            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ================= HELPERS =================
    private long parseDate(String date) {
        try { return sdf.parse(date).getTime(); }
        catch (Exception e) { return 0L; }
    }

    private String safe(String v) { return v == null ? "-" : v; }

    private String trim(String t, int max) {
        if (t == null) return "-";
        return t.length() > max ? t.substring(0, max - 3) + "..." : t;
    }

    private void openFile(File file, String type) {
        Uri uri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
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
}
