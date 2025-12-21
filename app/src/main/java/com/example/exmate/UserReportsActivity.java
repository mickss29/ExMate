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
                .child(uid);

    }

    // ================= LOAD TRANSACTIONS =================
    // ================= LOAD TRANSACTIONS (INCOME + EXPENSE) =================
    private void loadTransactions() {

        masterList.clear();
        transactionList.clear();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference incomeRef = userRef.child("incomes");
        DatabaseReference expenseRef = userRef.child("expenses");

        Map<String, List<TransactionListItem>> grouped =
                new TreeMap<>(Collections.reverseOrder());

        // ================= EXPENSE LISTENER (DECLARE FIRST) =================
        ValueEventListener expenseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Long time = snap.child("time").getValue(Long.class);
                    Double amount = snap.child("amount").getValue(Double.class);
                    String category = snap.child("category").getValue(String.class);
                    String note = snap.child("note").getValue(String.class);
                    String mode = snap.child("paymentMode").getValue(String.class);

                    if (time == null || amount == null) continue;

                    String key = dateKeyFormat.format(new Date(time));
                    grouped.putIfAbsent(key, new ArrayList<>());

                    TransactionListItem item =
                            new TransactionListItem(TransactionListItem.TYPE_TRANSACTION);

                    String meta =
                            dateHeaderFormat.format(new Date(time)) + " • " +
                                    timeFormat.format(new Date(time)) + " • " +
                                    (mode == null ? "Cash" : mode);

                    item.setTransaction(
                            category == null ? "Expense" : category,
                            note == null ? "" : note,
                            "- ₹" + amount.intValue(),
                            meta,
                            amount >= 1000
                    );
                    item.setTimeMillis(time);

                    grouped.get(key).add(item);
                }

                // Build final list
                masterList.clear();

                for (String key : grouped.keySet()) {

                    TransactionListItem header =
                            new TransactionListItem(TransactionListItem.TYPE_DATE);
                    header.setDateTitle(dateHeaderFormat.format(parseDateKey(key)));
                    masterList.add(header);

                    grouped.get(key).sort((a, b) ->
                            Long.compare(b.getTimeMillis(), a.getTimeMillis()));

                    masterList.addAll(grouped.get(key));
                }

                applyDateFilter();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showEmpty(true);
            }
        };

        // ================= INCOME LISTENER =================
        ValueEventListener incomeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Long time = snap.child("time").getValue(Long.class);
                    Double amount = snap.child("amount").getValue(Double.class);
                    String source = snap.child("source").getValue(String.class);
                    String note = snap.child("note").getValue(String.class);

                    if (time == null || amount == null) continue;

                    String key = dateKeyFormat.format(new Date(time));
                    grouped.putIfAbsent(key, new ArrayList<>());

                    TransactionListItem item =
                            new TransactionListItem(TransactionListItem.TYPE_TRANSACTION);

                    String meta =
                            dateHeaderFormat.format(new Date(time)) + " • " +
                                    timeFormat.format(new Date(time)) + " • Income";

                    item.setTransaction(
                            source == null ? "Income" : source,
                            note == null ? "" : note,
                            "₹ " + amount.intValue(),   // ✅ ONLY THIS
                            meta,
                            amount >= 1000
                    );

                    item.setTimeMillis(time);

                    grouped.get(key).add(item);
                }

                // AFTER income → load expense
                expenseRef.addListenerForSingleValueEvent(expenseListener);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showEmpty(true);
            }
        };

        // START FLOW
        incomeRef.addListenerForSingleValueEvent(incomeListener);
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

            if (dateMatch) {

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

            startDateMillis = getStartOfDay(selection.first);
            endDateMillis = getEndOfDay(selection.second);

            txtDateRange.setText(
                    dateHeaderFormat.format(new Date(startDateMillis))
                            + " - "
                            + dateHeaderFormat.format(new Date(endDateMillis))
            );

            applyDateFilter();
        });

        picker.show(getSupportFragmentManager(), "DATE_RANGE");
    }

    // ================= DATE HELPERS =================
    private long getStartOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
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

        v.findViewById(R.id.btnCancel).setOnClickListener(b -> dialog.dismiss());

        dialog.show();
    }

    private void exportPdf() {

        if (startDateMillis == -1 || endDateMillis == -1) {
            Toast.makeText(this, "Select date range first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (transactionList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Pair<String, Integer>> incomes = new ArrayList<>();
        List<Pair<String, Integer>> expenses = new ArrayList<>();

        int totalIncome = 0;
        int totalExpense = 0;

        for (TransactionListItem item : transactionList) {

            if (item.getType() != TransactionListItem.TYPE_TRANSACTION)
                continue;

            String amtStr = item.getAmount();
            if (amtStr == null) continue;

            int amount = Integer.parseInt(
                    amtStr.replace("₹", "")
                            .replace("+", "")
                            .replace("-", "")
                            .trim()
            );

            if (amtStr.startsWith("-")) {
                // 🔴 EXPENSE
                expenses.add(new Pair<>(item.getCategory(), amount));
                totalExpense += amount;
            } else {
                // 🟢 INCOME
                incomes.add(new Pair<>(item.getCategory(), amount));
                totalIncome += amount;
            }
        }

        // 🔥 THIS IS THE IMPORTANT LINE
        generateCorporatePdf(
                incomes,
                expenses,
                totalIncome,
                totalExpense
        );
    }


    // ================= PDF DRAW =================
// ================= PDF DRAW (WITH DATA ROWS) =================
    // ================= PDF DRAW (CORPORATE UI + COLORS) =================
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

        Paint title = new Paint();
        Paint bold = new Paint();
        Paint text = new Paint();
        Paint line = new Paint();
        Paint headerBg = new Paint();
        Paint totalBg = new Paint();

        // 🔥 COLOR PAINTS
        Paint incomePaint = new Paint();
        Paint expensePaint = new Paint();

        title.setFakeBoldText(true);
        title.setTextSize(26);

        bold.setFakeBoldText(true);
        bold.setTextSize(12);

        text.setTextSize(11);

        incomePaint.setTextSize(11);
        incomePaint.setColor(0xFF2E7D32);   // ✅ GREEN

        expensePaint.setTextSize(11);
        expensePaint.setColor(0xFFC62828);  // ❌ RED

        line.setStrokeWidth(1);

        headerBg.setARGB(255, 220, 220, 220);
        totalBg.setARGB(255, 255, 204, 102);

        int y = 50;

        // ===== HEADER =====
        c.drawText("ExMate", 40, y, title);

        y += 35;
        bold.setTextSize(20);
        c.drawText("Income Expense Report", 40, y, bold);

        y += 25;
        c.drawLine(40, y, 555, y, line);

        y += 20;
        text.setTextSize(12);
        c.drawText("Prepared by : ExMate User", 40, y, text);
        c.drawText("Period : " + txtDateRange.getText().toString(), 340, y, text);

        y += 30;

        // ================= INCOME DETAILS =================
        bold.setTextSize(14);
        c.drawText("Income Details :", 40, y, bold);

        y += 10;
        c.drawRect(40, y, 555, y + 25, headerBg);
        c.drawText("No.", 50, y + 17, bold);
        c.drawText("Income Description", 100, y + 17, bold);
        c.drawText("Amount", 460, y + 17, bold);

        y += 35;
        int index = 1;

        for (Pair<String, Integer> p : incomes) {
            c.drawText(String.valueOf(index), 50, y, incomePaint);
            c.drawText(p.first, 100, y, incomePaint);
            c.drawText("₹ " + p.second, 460, y, incomePaint);
            y += 18;
            index++;
        }

        // TOTAL INCOME
        c.drawRect(40, y - 12, 555, y + 10, totalBg);
        bold.setTextSize(12);
        c.drawText("TOTAL INCOME", 100, y, bold);
        c.drawText("₹ " + totalIncome, 460, y, bold);

        y += 40;

        // ================= EXPENSE DETAILS =================
        bold.setTextSize(14);
        c.drawText("Expense Details :", 40, y, bold);

        y += 10;
        c.drawRect(40, y, 555, y + 25, headerBg);
        c.drawText("No.", 50, y + 17, bold);
        c.drawText("Expense Description", 100, y + 17, bold);
        c.drawText("Amount", 460, y + 17, bold);

        y += 35;
        index = 1;

        for (Pair<String, Integer> p : expenses) {
            c.drawText(String.valueOf(index), 50, y, expensePaint);
            c.drawText(p.first, 100, y, expensePaint);
            c.drawText("₹ " + p.second, 460, y, expensePaint);
            y += 18;
            index++;
        }

        // TOTAL EXPENSE
        c.drawRect(40, y - 12, 555, y + 10, totalBg);
        bold.setTextSize(12);
        c.drawText("TOTAL EXPENSE", 100, y, bold);
        c.drawText("₹ " + totalExpense, 460, y, bold);

        // ===== FOOTER =====
        y = 800;
        c.drawLine(40, y, 555, y, line);
        text.setTextSize(10);
        c.drawText("Digitally generated by ExMate App", 40, y + 20, text);
        c.drawText("Page 1", 520, y + 20, text);

        pdf.finishPage(page);
        savePdf(pdf);
    }


    // ================= SAVE PDF =================
    // ================= SAVE PDF (FINAL FIX) =================
    private void savePdf(PdfDocument pdf) {

        try {
            File dir = new File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "ExMate"
            );
            if (!dir.exists()) dir.mkdirs();

            // 🔥 UNIQUE FILE NAME (NO CACHE ISSUE)
            String fileName =
                    "ExMate_Report_" + System.currentTimeMillis() + ".pdf";

            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            // 🔥 ALWAYS OPEN NEW FILE
            openFile(file, "application/pdf");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF export failed", Toast.LENGTH_LONG).show();
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
                new ItemTouchHelper.SimpleCallback(0,
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
    // ================= FINAL PDF GENERATION =================
    private void generatePdfFinal(
            List<Pair<String, Integer>> incomes,
            List<Pair<String, Integer>> expenses,
            int totalIncome,
            int totalExpense
    ) {

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas c = page.getCanvas();

        Paint title = new Paint();
        Paint text = new Paint();
        Paint bold = new Paint();
        Paint line = new Paint();

        title.setFakeBoldText(true);
        title.setTextSize(22);

        bold.setFakeBoldText(true);
        bold.setTextSize(14);

        text.setTextSize(12);
        line.setStrokeWidth(1);

        int y = 40;

        // ===== HEADER =====
        c.drawText("ExMate", 40, y, title);
        y += 30;
        c.drawText("Income Expense Report", 40, y, bold);

        y += 20;
        c.drawLine(40, y, 555, y, line);

        y += 20;
        c.drawText(
                "Period : " + txtDateRange.getText().toString(),
                40, y, text
        );

        y += 30;

        // ===== INCOME =====
        bold.setTextSize(16);
        c.drawText("Income Details", 40, y, bold);
        y += 20;

        int index = 1;
        for (Pair<String, Integer> p : incomes) {
            c.drawText(index + ". " + p.first, 50, y, text);
            c.drawText("₹ " + p.second, 450, y, text);
            y += 18;
            index++;
        }

        y += 10;
        bold.setTextSize(14);
        c.drawText("Total Income : ₹ " + totalIncome, 50, y, bold);

        y += 30;
        c.drawLine(40, y, 555, y, line);
        y += 20;

        // ===== EXPENSE =====
        bold.setTextSize(16);
        c.drawText("Expense Details", 40, y, bold);
        y += 20;

        index = 1;
        for (Pair<String, Integer> p : expenses) {
            c.drawText(index + ". " + p.first, 50, y, text);
            c.drawText("₹ " + p.second, 450, y, text);
            y += 18;
            index++;
        }

        y += 10;
        bold.setTextSize(14);
        c.drawText("Total Expense : ₹ " + totalExpense, 50, y, bold);

        // ===== FOOTER =====
        y = 800;
        c.drawLine(40, y, 555, y, line);
        y += 20;
        text.setTextSize(10);
        c.drawText("Digitally generated by ExMate App", 40, y, text);
        c.drawText("Page 1", 520, y, text);

        pdf.finishPage(page);
        savePdf(pdf);
    }

}
