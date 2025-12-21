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

    // ================= UI =================
    private RecyclerView recyclerReports;
    private TextView tvEmpty, txtDateRange;
    private String selectedCategory = "All";

    // ================= FIREBASE =================
    private DatabaseReference userRef;

    // ================= DATA =================
    private final List<TransactionListItem> masterList = new ArrayList<>();
    private final List<TransactionListItem> transactionList = new ArrayList<>();

    private TransactionAdapter adapter;

    // ================= DATE RANGE =================
    private long startDateMillis = -1;
    private long endDateMillis = -1;

    private enum RangeType { NONE, THIS_MONTH, LAST_MONTH, CUSTOM }
    private RangeType selectedRangeType = RangeType.NONE;

    // ================= FORMATTERS =================
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

    // ================= LOAD TRANSACTIONS =================
    private void loadTransactions() {

        userRef.orderByChild("time")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        masterList.clear();
                        transactionList.clear();

                        if (!snapshot.exists()) {
                            showEmpty(true);
                            return;
                        }

                        Map<String, List<DataSnapshot>> grouped =
                                new TreeMap<>(Collections.reverseOrder());

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Long time = snap.child("time").getValue(Long.class);
                            if (time == null) continue;

                            String key = dateKeyFormat.format(new Date(time));
                            grouped.putIfAbsent(key, new ArrayList<>());
                            grouped.get(key).add(snap);
                        }

                        for (String key : grouped.keySet()) {

                            TransactionListItem header =
                                    new TransactionListItem(TransactionListItem.TYPE_DATE);
                            header.setDateTitle(dateHeaderFormat.format(parseDateKey(key)));
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
                                item.setTimeMillis(time);

                                masterList.add(item);
                            }
                        }

                        applyDateFilter();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showEmpty(true);
                    }
                });
    }

    // ================= FILTER =================
    private void applyDateFilter() {

        transactionList.clear();
        String currentHeader = "";

        for (TransactionListItem item : masterList) {

            if (item.getType() == TransactionListItem.TYPE_DATE) {
                currentHeader = item.getDateTitle();
                continue;
            }

            long txTime = item.getTimeMillis();

            boolean dateMatch =
                    (startDateMillis == -1 || endDateMillis == -1) ||
                            (txTime >= startDateMillis && txTime <= endDateMillis);

            boolean categoryMatch =
                    selectedCategory.equals("All") ||
                            item.getCategory().equalsIgnoreCase(selectedCategory);

            if (dateMatch && categoryMatch) {

                if (transactionList.isEmpty() ||
                        transactionList.get(transactionList.size() - 1).getType()
                                != TransactionListItem.TYPE_DATE) {

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

    // ================= DATE PICKER =================
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

    // ================= EXPORT SHEET =================
    private void showExportSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottomsheet_export, null);
        dialog.setContentView(v);

        v.findViewById(R.id.optionPdf).setOnClickListener(b -> {
            dialog.dismiss();
            exportPdf();
        });

        v.findViewById(R.id.optionCsv).setOnClickListener(b -> {
            dialog.dismiss();
            Toast.makeText(this, "CSV export coming soon", Toast.LENGTH_SHORT).show();
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(b -> dialog.dismiss());

        dialog.show();
    }

    // ================= PDF EXPORT =================
    private void exportPdf() {

        if (startDateMillis == -1 || endDateMillis == -1) {
            Toast.makeText(this, "Select date range first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference incomeRef =
                FirebaseDatabase.getInstance().getReference("users").child(uid).child("incomes");
        DatabaseReference expenseRef =
                FirebaseDatabase.getInstance().getReference("users").child(uid).child("expenses");

        incomeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot incomeSnap) {

                List<Pair<String, Integer>> incomes = new ArrayList<>();
                final int[] totalIncome = {0};

                for (DataSnapshot s : incomeSnap.getChildren()) {
                    Long time = s.child("time").getValue(Long.class);
                    Double amt = s.child("amount").getValue(Double.class);
                    String src = s.child("source").getValue(String.class);

                    if (time == null || amt == null) continue;
                    if (time < startDateMillis || time > endDateMillis) continue;

                    incomes.add(new Pair<>(src == null ? "Other" : src, amt.intValue()));
                    totalIncome[0] += amt.intValue();
                }

                expenseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot expenseSnap) {

                        List<Pair<String, Integer>> expenses = new ArrayList<>();
                        final int[] totalExpense = {0};

                        for (DataSnapshot s : expenseSnap.getChildren()) {
                            Long time = s.child("time").getValue(Long.class);
                            Double amt = s.child("amount").getValue(Double.class);
                            String cat = s.child("category").getValue(String.class);

                            if (time == null || amt == null) continue;
                            if (time < startDateMillis || time > endDateMillis) continue;

                            expenses.add(new Pair<>(cat == null ? "Other" : cat, amt.intValue()));
                            totalExpense[0] += amt.intValue();
                        }

                        generateCorporatePdf(
                                incomes,
                                expenses,
                                totalIncome[0],
                                totalExpense[0]
                        );
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // ================= PDF DRAW (CORPORATE TABLE STYLE) =================
    private void generateCorporatePdf(
            List<Pair<String, Integer>> incomes,
            List<Pair<String, Integer>> expenses,
            int totalIncome,
            int totalExpense
    ) {

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdf.startPage(info);
        Canvas c = page.getCanvas();

        Paint text = new Paint();
        Paint bold = new Paint();
        Paint line = new Paint();
        Paint headerBg = new Paint();
        Paint totalBg = new Paint();

        bold.setFakeBoldText(true);
        line.setStrokeWidth(1f);
        headerBg.setARGB(255, 220, 220, 220);
        totalBg.setARGB(255, 255, 200, 70);

        int y = 40;

        bold.setTextSize(26);
        c.drawText("ExMate", 40, y, bold);

        bold.setTextSize(24);
        c.drawText("Income Expense Report", 40, y + 40, bold);

        y += 70;
        c.drawLine(40, y, 555, y, line);

        text.setTextSize(12);
        y += 25;
        c.drawText("Prepared by : ExMate User", 40, y, text);
        c.drawText("Period : " + txtDateRange.getText().toString(), 340, y, text);

        y += 35;

        // INCOME TABLE
        bold.setTextSize(16);
        c.drawText("Income Details :", 40, y, bold);

        y += 15;
        c.drawRect(40, y, 555, y + 25, headerBg);
        bold.setTextSize(12);
        c.drawText("No.", 50, y + 17, bold);
        c.drawText("Income Description", 100, y + 17, bold);
        c.drawText("Amount", 460, y + 17, bold);

        y += 30;
        text.setTextSize(11);
        int index = 1;
        for (Pair<String, Integer> p : incomes) {
            c.drawText(String.valueOf(index), 50, y, text);
            c.drawText(p.first, 100, y, text);
            c.drawText("₹ " + p.second, 460, y, text);
            y += 20;
            index++;
        }

        c.drawRect(40, y - 12, 555, y + 10, totalBg);
        bold.setTextSize(12);
        c.drawText("TOTAL INCOME", 100, y, bold);
        c.drawText("₹ " + totalIncome, 460, y, bold);

        y += 40;

        // EXPENSE TABLE
        bold.setTextSize(16);
        c.drawText("Expense Details :", 40, y, bold);

        y += 15;
        c.drawRect(40, y, 555, y + 25, headerBg);
        bold.setTextSize(12);
        c.drawText("No.", 50, y + 17, bold);
        c.drawText("Expense Description", 100, y + 17, bold);
        c.drawText("Amount", 460, y + 17, bold);

        y += 30;
        text.setTextSize(11);
        index = 1;
        for (Pair<String, Integer> p : expenses) {
            c.drawText(String.valueOf(index), 50, y, text);
            c.drawText(p.first, 100, y, text);
            c.drawText("₹ " + p.second, 460, y, text);
            y += 20;
            index++;
        }

        c.drawRect(40, y - 12, 555, y + 10, totalBg);
        bold.setTextSize(12);
        c.drawText("TOTAL EXPENSE", 100, y, bold);
        c.drawText("₹ " + totalExpense, 460, y, bold);

        y += 40;

        text.setTextSize(10);
        c.drawLine(40, 800, 555, 800, line);
        c.drawText("Digitally generated by ExMate App", 40, 820, text);
        c.drawText("Page 1", 520, 820, text);

        pdf.finishPage(page);
        savePdf(pdf);
    }

    // ================= SAVE PDF =================
    private void savePdf(PdfDocument pdf) {

        try {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "ExMate");

            if (!dir.exists()) dir.mkdirs();

            File file = new File(
                    dir,
                    "ExMate_Report_" +
                            monthFileFormat.format(new Date(startDateMillis)) + ".pdf"
            );

            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this, "PDF export failed", Toast.LENGTH_SHORT).show();
        }
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
}

