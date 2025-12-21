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

    // ================= PDF EXPORT =================
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
                final int[] totalIncome = {0};   // ✅ FIX

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
                        final int[] totalExpense = {0};   // ✅ FIX

                        for (DataSnapshot s : expenseSnap.getChildren()) {
                            Long time = s.child("time").getValue(Long.class);
                            Double amt = s.child("amount").getValue(Double.class);
                            String cat = s.child("category").getValue(String.class);

                            if (time == null || amt == null) continue;
                            if (time < startDateMillis || time > endDateMillis) continue;

                            expenses.add(new Pair<>(cat == null ? "Other" : cat, amt.intValue()));
                            totalExpense[0] += amt.intValue();
                        }

                        // ✅ PASS VALUES CORRECTLY
                        generateCorporatePdf(
                                incomes,
                                expenses,
                                totalIncome[0],
                                totalExpense[0]
                        );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }


    // ================= PDF DRAW =================
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

        bold.setFakeBoldText(true);
        int y = 60;

        bold.setTextSize(22);
        c.drawText("Income Expense Report", 40, y, bold);

        y += 40;
        text.setTextSize(12);
        c.drawText("Period : " + txtDateRange.getText().toString(), 40, y, text);

        y += 30;
        bold.setTextSize(16);
        c.drawText("Total Income : ₹ " + totalIncome, 40, y, bold);

        y += 25;
        c.drawText("Total Expense : ₹ " + totalExpense, 40, y, bold);

        pdf.finishPage(page);
        savePdf(pdf);
    }

    // ================= SAVE PDF =================
    private void savePdf(PdfDocument pdf) {

        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ExMate");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(
                    dir,
                    "ExMate_Report_" + monthFileFormat.format(new Date(startDateMillis)) + ".pdf"
            );

            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
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
}
