package com.example.exmate;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserReportsActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerReports;
    private TextView tvEmpty, txtDateRange;
    private String selectedCategory = "All";


    // Firebase
    private DatabaseReference userRef;

    // 🔹 Master (all data)
    private final List<TransactionListItem> masterList = new ArrayList<>();

    // 🔹 Filtered (shown)
    private final List<TransactionListItem> transactionList = new ArrayList<>();

    // 🔹 Export
    private final List<TransactionListItem> exportList = new ArrayList<>();

    private TransactionAdapter adapter;

    // Date range
    private long startDateMillis = -1;
    private long endDateMillis = -1;

    // Button retain
    private enum RangeType { NONE, THIS_MONTH, LAST_MONTH, CUSTOM }
    private RangeType selectedRangeType = RangeType.NONE;

    // Formatters
    private final SimpleDateFormat dateKeyFormat =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat dateHeaderFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final SimpleDateFormat monthFileFormat =
            new SimpleDateFormat("MMMM_yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reports);

        recyclerReports = findViewById(R.id.recyclerReports);
        tvEmpty = findViewById(R.id.tvEmpty);
        txtDateRange = findViewById(R.id.txtDateRange);

        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactionList);
        recyclerReports.setAdapter(adapter);

        txtDateRange.setOnClickListener(v -> pickDateRange());
        findViewById(R.id.fabExport).setOnClickListener(v -> showExportSheet());

        setupFirebase();
        attachSwipe();
        loadTransactions();
    }

    // ================= FIREBASE =================

    private void setupFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("expenses");
    }

    // ================= LOAD DATA =================

    private void loadTransactions() {

        userRef.orderByChild("time")
                .addValueEventListener(new ValueEventListener() { // 🔥 REAL-TIME
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        masterList.clear();
                        transactionList.clear();

                        if (!snapshot.exists()) {
                            showEmpty(true);
                            return;
                        }

                        Map<String, List<DataSnapshot>> grouped = new TreeMap<>(Collections.reverseOrder());

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            Long time = snap.child("time").getValue(Long.class);
                            if (time == null) continue;

                            String key = dateKeyFormat.format(new Date(time));

                            if (!grouped.containsKey(key)) {
                                grouped.put(key, new ArrayList<>());
                            }
                            grouped.get(key).add(snap);
                        }

                        for (String key : grouped.keySet()) {

                            TransactionListItem header =
                                    new TransactionListItem(TransactionListItem.TYPE_DATE);
                            header.setDateTitle(
                                    dateHeaderFormat.format(parseDateKey(key))
                            );
                            masterList.add(header);

                            for (DataSnapshot snap : grouped.get(key)) {

                                String category = snap.child("category").getValue(String.class);
                                String note = snap.child("note").getValue(String.class);
                                Double amount = snap.child("amount").getValue(Double.class);
                                Long time = snap.child("time").getValue(Long.class);
                                String mode = snap.child("paymentMode").getValue(String.class);

                                if (amount == null || time == null) continue;

                                TransactionListItem item =
                                        new TransactionListItem(TransactionListItem.TYPE_TRANSACTION);

                                String meta =
                                        dateHeaderFormat.format(new Date(time))
                                                + " • "
                                                + timeFormat.format(new Date(time))
                                                + " • "
                                                + (mode == null ? "Cash" : mode);

                                item.setTransaction(
                                        category == null ? "Other" : category,
                                        note == null ? "" : note,
                                        "- ₹" + amount.intValue(),
                                        meta,
                                        amount >= 1000
                                );
                                item.setTimeMillis(time); // ✅ VERY IMPORTANT (REAL DATE FIX)


                                masterList.add(item);
                            }
                        }

                        applyDateFilter(); // 🔥 refresh UI
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showEmpty(true);
                    }
                });
    }

    // ================= DATE FILTER APPLY =================

    private void applyDateFilter() {

        transactionList.clear();
        String currentHeader = "";

        for (TransactionListItem item : masterList) {

            if (item.getType() == TransactionListItem.TYPE_DATE) {
                currentHeader = item.getDateTitle();
                continue;
            }

            long txTime = item.getTimeMillis(); // 🔥 REAL FIREBASE TIME

            boolean dateMatch =
                    (startDateMillis == -1 || endDateMillis == -1) ||
                            (txTime >= startDateMillis && txTime <= endDateMillis);

            boolean categoryMatch =
                    selectedCategory.equals("All") ||
                            item.getCategory().equalsIgnoreCase(selectedCategory);

            if (dateMatch && categoryMatch) {

                // add date header only once
                if (transactionList.isEmpty() ||
                        transactionList.get(transactionList.size() - 1).getType()
                                != TransactionListItem.TYPE_DATE ||
                        !transactionList
                                .get(transactionList.size() - 1)
                                .getDateTitle()
                                .equals(currentHeader)) {

                    TransactionListItem header =
                            new TransactionListItem(TransactionListItem.TYPE_DATE);
                    header.setDateTitle(currentHeader);
                    transactionList.add(header);
                }

                transactionList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        showEmpty(transactionList.isEmpty());
    }


    // ================= DATE RANGE PICKER =================

    private void pickDateRange() {

        long today = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints constraints =
                new CalendarConstraints.Builder()
                        .setEnd(today)
                        .build();

        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setCalendarConstraints(constraints)
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {

            if (selection == null) return;

            startDateMillis = selection.first;
            endDateMillis = selection.second;
            selectedRangeType = RangeType.CUSTOM;

            txtDateRange.setText(
                    dateHeaderFormat.format(new Date(startDateMillis))
                            + " - "
                            + dateHeaderFormat.format(new Date(endDateMillis))
            );

            applyDateFilter();
        });

        picker.show(getSupportFragmentManager(), "DATE_RANGE");
    }

    // ================= QUICK MONTH =================

    private void setThisMonth() {

        Calendar s = Calendar.getInstance();
        s.set(Calendar.DAY_OF_MONTH, 1);
        s.set(Calendar.HOUR_OF_DAY, 0);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);

        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        e.add(Calendar.SECOND, -1);

        startDateMillis = s.getTimeInMillis();
        endDateMillis = e.getTimeInMillis();
        selectedRangeType = RangeType.THIS_MONTH;

        txtDateRange.setText(
                dateHeaderFormat.format(s.getTime())
                        + " - "
                        + dateHeaderFormat.format(e.getTime())
        );

        applyDateFilter();
    }

    private void setLastMonth() {

        Calendar s = Calendar.getInstance();
        s.add(Calendar.MONTH, -1);
        s.set(Calendar.DAY_OF_MONTH, 1);
        s.set(Calendar.HOUR_OF_DAY, 0);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);

        Calendar e = (Calendar) s.clone();
        e.add(Calendar.MONTH, 1);
        e.add(Calendar.SECOND, -1);

        startDateMillis = s.getTimeInMillis();
        endDateMillis = e.getTimeInMillis();
        selectedRangeType = RangeType.LAST_MONTH;

        txtDateRange.setText(
                dateHeaderFormat.format(s.getTime())
                        + " - "
                        + dateHeaderFormat.format(e.getTime())
        );

        applyDateFilter();
    }

    // ================= EXPORT SHEET =================

    private void showExportSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottomsheet_export, null);
        dialog.setContentView(v);

        MaterialButton btnThis = v.findViewById(R.id.btnThisMonth);
        MaterialButton btnLast = v.findViewById(R.id.btnLastMonth);

        Runnable reset = () -> {
            btnThis.setBackgroundTintList(getColorStateList(R.color.darkBg));
            btnLast.setBackgroundTintList(getColorStateList(R.color.darkBg));
        };

        reset.run();
        if (selectedRangeType == RangeType.THIS_MONTH)
            btnThis.setBackgroundTintList(getColorStateList(R.color.primaryBlue));
        else if (selectedRangeType == RangeType.LAST_MONTH)
            btnLast.setBackgroundTintList(getColorStateList(R.color.primaryBlue));

        btnThis.setOnClickListener(b -> {
            reset.run();
            btnThis.setBackgroundTintList(getColorStateList(R.color.primaryBlue));
            setThisMonth();
        });

        btnLast.setOnClickListener(b -> {
            reset.run();
            btnLast.setBackgroundTintList(getColorStateList(R.color.primaryBlue));
            setLastMonth();
        });

        v.findViewById(R.id.optionPdf).setOnClickListener(b -> {
            dialog.dismiss();
            exportPdf();
        });

        v.findViewById(R.id.optionCsv).setOnClickListener(b -> {
            dialog.dismiss();
            exportCsv();
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(b -> dialog.dismiss());

        dialog.show();
    }

    // ================= EXPORT PREP =================

    private void prepareExportList() {

        exportList.clear();

        for (TransactionListItem i : transactionList) {
            if (i.getType() == TransactionListItem.TYPE_TRANSACTION)
                exportList.add(i);
        }
    }

    // ================= PDF =================
    private void exportPdf() {

        prepareExportList();
        if (exportList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdf = new PdfDocument();

        Paint text = new Paint();
        Paint bold = new Paint();
        Paint line = new Paint();
        Paint box = new Paint();

        bold.setFakeBoldText(true);
        line.setStrokeWidth(1f);
        box.setStyle(Paint.Style.STROKE);
        box.setStrokeWidth(1.5f);
        box.setARGB(255, 220, 220, 220);

        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        // =================================================
        // PAGE 1 : SUMMARY
        // =================================================
        PdfDocument.Page page1 = pdf.startPage(info);
        Canvas c = page1.getCanvas();
        int y = 40;

        // Header
        bold.setTextSize(26f);
        c.drawText("ExMate", 40, y, bold);

        text.setTextSize(11f);
        text.setARGB(255, 120, 120, 120);
        c.drawText("Smart Expense. Clear Control.", 40, y + 16, text);

        text.setTextSize(12f);
        text.setARGB(255, 0, 0, 0);
        c.drawText("EXPENSE SUMMARY", 400, y, text);

        y += 28;
        c.drawLine(40, y, 555, y, line);

        // Meta
        y += 25;
        text.setTextSize(11f);

        c.drawText("Date Range:", 40, y, text);
        c.drawText(txtDateRange.getText().toString(), 140, y, text);

        String genDate = new SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault()).format(new Date());

        c.drawText("Generated:", 360, y, text);
        c.drawText(genDate, 450, y, text);

        // Summary box
        y += 35;

        int total = 0;
        for (TransactionListItem i : exportList)
            total += parseAmount(i.getAmount());

        c.drawRect(40, y, 555, y + 70, box);

        text.setTextSize(14f);
        bold.setTextSize(14f);

        c.drawText("Total Transactions", 60, y + 28, text);
        c.drawText(String.valueOf(exportList.size()), 260, y + 28, bold);

        c.drawText("Total Amount Spent", 60, y + 52, text);
        c.drawText("₹ " + total, 260, y + 52, bold);

        // Footer
        text.setTextSize(10f);
        text.setARGB(255, 120, 120, 120);
        c.drawLine(40, 800, 555, 800, text);
        c.drawText("Generated by ExMate App", 40, 820, text);
        c.drawText("— ExMate Finance System", 360, 820, text);

        pdf.finishPage(page1);

        // =================================================
        // PAGE 2 : TRANSACTION DETAILS
        // =================================================
        PdfDocument.Page page2 = pdf.startPage(info);
        c = page2.getCanvas();
        y = 40;

        bold.setTextSize(20f);
        c.drawText("Transaction Details", 40, y, bold);

        y += 10;
        c.drawLine(40, y, 555, y, line);

        // Table header
        y += 25;
        bold.setTextSize(11f);
        c.drawText("Category", 40, y, bold);
        c.drawText("Note", 160, y, bold);
        c.drawText("Amount", 500, y, bold);

        y += 5;
        c.drawLine(40, y, 555, y, line);

        // Table data
        y += 18;
        text.setTextSize(10f);

        for (TransactionListItem item : exportList) {

            c.drawText(item.getCategory(), 40, y, text);
            c.drawText(item.getNote(), 160, y, text);
            c.drawText(item.getAmount(), 500, y, text);

            y += 16;

            if (y > 780) {
                pdf.finishPage(page2);
                page2 = pdf.startPage(info);
                c = page2.getCanvas();
                y = 40;
            }
        }

        pdf.finishPage(page2);

        savePdf(pdf);
    }

    // ================= SAVE =================

    private void savePdf(PdfDocument pdf) {

        try {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "ExMate");

            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir,
                    "ExMate_Report_" +
                            monthFileFormat.format(new Date(startDateMillis)) + ".pdf");

            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this,
                    "PDF saved in Downloads/ExMate",
                    Toast.LENGTH_LONG).show();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this,
                    "PDF export failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void exportCsv() {
        prepareExportList();
        Toast.makeText(this, "CSV export ready", Toast.LENGTH_SHORT).show();
    }

    // ================= HELPERS =================

    private void openFile(File file, String type) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file);

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerReports.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private Date parseDateKey(String key) {
        try {
            return dateKeyFormat.parse(key);
        } catch (Exception e) {
            return new Date();
        }
    }

    private void attachSwipe() {
        ItemTouchHelper.SimpleCallback cb =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(
                            RecyclerView r,
                            RecyclerView.ViewHolder v,
                            RecyclerView.ViewHolder t) {
                        return false;
                    }

                    @Override
                    public void onSwiped(
                            RecyclerView.ViewHolder vh, int d) {
                        adapter.notifyItemChanged(vh.getAdapterPosition());
                    }
                };

        new ItemTouchHelper(cb).attachToRecyclerView(recyclerReports);

    }
    private void showCategoryPicker() {

        String[] categories = {
                "All",
                "Food",
                "Travel",
                "Shopping",
                "Bills",
                "Other"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setItems(categories, (d, which) -> {

                    selectedCategory = categories[which];

                    Toast.makeText(
                            this,
                            "Category: " + selectedCategory,
                            Toast.LENGTH_SHORT
                    ).show();

                    applyDateFilter(); // 🔥 re-filter list
                })
                .show();
    }
    private int parseAmount(String amt) {
        try {
            return Integer.parseInt(
                    amt.replace("₹", "")
                            .replace("-", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }


}
