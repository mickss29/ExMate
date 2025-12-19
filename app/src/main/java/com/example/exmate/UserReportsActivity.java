package com.example.exmate;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserReportsActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerReports;
    private TextView tvEmpty;

    // Firebase
    private DatabaseReference userRef;

    // Adapter & Data
    private TransactionAdapter adapter;
    private final List<TransactionListItem> transactionList = new ArrayList<>();

    // Date formatters
    private final SimpleDateFormat dateKeyFormat =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat dateHeaderFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reports);

        initViews();
        setupRecycler();
        setupFirebase();
        attachSwipe();
        setupExport();

        loadTransactions();
    }

    // ================= INIT =================

    private void initViews() {
        recyclerReports = findViewById(R.id.recyclerReports);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupRecycler() {
        adapter = new TransactionAdapter(transactionList);
        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerReports.setAdapter(adapter);
    }

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

    // ================= LOAD DATA =================

    private void loadTransactions() {

        userRef.child("expenses")
                .orderByChild("time")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        transactionList.clear();

                        if (!snapshot.exists()) {
                            showEmpty(true);
                            return;
                        }

                        Map<String, List<DataSnapshot>> grouped =
                                new LinkedHashMap<>();

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            Long time = snap.child("time").getValue(Long.class);
                            if (time == null) continue;

                            String dateKey =
                                    dateKeyFormat.format(new Date(time));

                            if (!grouped.containsKey(dateKey)) {
                                grouped.put(dateKey, new ArrayList<>());
                            }
                            grouped.get(dateKey).add(snap);
                        }

                        for (String dateKey : grouped.keySet()) {

                            long millis = parseDateKey(dateKey);
                            String headerTitle = getDateTitle(millis);

                            TransactionListItem header =
                                    new TransactionListItem(
                                            TransactionListItem.TYPE_DATE);
                            header.setDateTitle(headerTitle);
                            transactionList.add(header);

                            for (DataSnapshot snap : grouped.get(dateKey)) {

                                String category =
                                        snap.child("category")
                                                .getValue(String.class);
                                String note =
                                        snap.child("note")
                                                .getValue(String.class);
                                Double amount =
                                        snap.child("amount")
                                                .getValue(Double.class);
                                Long time =
                                        snap.child("time")
                                                .getValue(Long.class);
                                String mode =
                                        snap.child("paymentMode")
                                                .getValue(String.class);

                                if (category == null) category = "Other";
                                if (note == null) note = "";
                                if (mode == null) mode = "Cash";
                                if (amount == null || time == null) continue;

                                String meta =
                                        dateHeaderFormat.format(new Date(time))
                                                + " • "
                                                + timeFormat.format(new Date(time))
                                                + " • "
                                                + mode;

                                TransactionListItem item =
                                        new TransactionListItem(
                                                TransactionListItem.TYPE_TRANSACTION);

                                item.setTransaction(
                                        category,
                                        note,
                                        "- ₹" + amount.intValue(),
                                        meta,
                                        amount >= 1000
                                );

                                transactionList.add(item);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        showEmpty(transactionList.isEmpty());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showEmpty(true);
                    }
                });
    }

    // ================= SWIPE =================

    private void attachSwipe() {

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public int getSwipeDirs(
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {

                        int pos = viewHolder.getAdapterPosition();
                        if (transactionList.get(pos).getType()
                                == TransactionListItem.TYPE_DATE) {
                            return 0;
                        }
                        return super.getSwipeDirs(recyclerView, viewHolder);
                    }

                    @Override
                    public void onSwiped(
                            RecyclerView.ViewHolder viewHolder,
                            int direction) {

                        int pos = viewHolder.getAdapterPosition();

                        if (direction == ItemTouchHelper.LEFT) {
                            adapter.notifyItemChanged(pos);
                            Toast.makeText(
                                    UserReportsActivity.this,
                                    "Edit coming soon",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            transactionList.remove(pos);
                            adapter.notifyItemRemoved(pos);
                            showEmpty(transactionList.isEmpty());
                        }
                    }
                };

        new ItemTouchHelper(callback)
                .attachToRecyclerView(recyclerReports);
    }

    // ================= EXPORT =================

    private void setupExport() {
        findViewById(R.id.fabExport)
                .setOnClickListener(v -> showExportSheet());
    }

    private void showExportSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater()
                .inflate(R.layout.bottomsheet_export, null);
        dialog.setContentView(view);

        view.findViewById(R.id.optionPdf)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    exportPdf();
                });

        view.findViewById(R.id.optionCsv)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    exportCsv();
                });

        view.findViewById(R.id.btnCancel)
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ================= PDF =================

    private void exportPdf() {

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("ExMate - Expense Report", 40, 50, titlePaint);

        paint.setTextSize(10f);
        canvas.drawText(
                "Generated: " +
                        new SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                        ).format(new Date()),
                40,
                70,
                paint
        );

        int y = 110;
        paint.setFakeBoldText(true);
        canvas.drawText("Category", 40, y, paint);
        canvas.drawText("Note", 160, y, paint);
        canvas.drawText("Amount", 400, y, paint);
        paint.setFakeBoldText(false);
        y += 20;

        for (TransactionListItem item : transactionList) {

            if (item.getType() == TransactionListItem.TYPE_DATE)
                continue;

            canvas.drawText(item.getCategory(), 40, y, paint);
            canvas.drawText(item.getNote(), 160, y, paint);
            canvas.drawText(item.getAmount(), 400, y, paint);

            y += 18;
            if (y > 800) {
                pdf.finishPage(page);
                page = pdf.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
        }

        pdf.finishPage(page);
        saveAndSharePdf(pdf);
    }

    private void saveAndSharePdf(PdfDocument pdf) {
        try {
            File file = new File(
                    getExternalFilesDir(null),
                    "ExMate_Report_" + System.currentTimeMillis() + ".pdf"
            );
            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            shareFile(file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(this,
                    "PDF export failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ================= CSV =================

    private void exportCsv() {

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Time,Category,Note,Payment Mode,Amount\n");

        for (TransactionListItem item : transactionList) {

            if (item.getType() == TransactionListItem.TYPE_DATE)
                continue;

            String[] meta = item.getMeta().split(" • ");

            csv.append(escape(meta, 0)).append(",");
            csv.append(escape(meta, 1)).append(",");
            csv.append(escape(item.getCategory())).append(",");
            csv.append(escape(item.getNote())).append(",");
            csv.append(escape(meta, 2)).append(",");
            csv.append(escape(item.getAmount())).append("\n");
        }

        try {
            File file = new File(
                    getExternalFilesDir(null),
                    "ExMate_Report_" + System.currentTimeMillis() + ".csv"
            );
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(csv.toString().getBytes());
            fos.close();

            shareFile(file, "text/csv");

        } catch (Exception e) {
            Toast.makeText(this,
                    "CSV export failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String escape(String[] arr, int i) {
        if (arr.length <= i) return "\"\"";
        return "\"" + arr[i].replace("\"", "\"\"") + "\"";
    }

    private String escape(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    // ================= SHARE =================

    private void shareFile(File file, String type) {

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(
                Intent.createChooser(
                        intent,
                        "Share Report"
                )
        );
    }

    // ================= HELPERS =================

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerReports.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private long parseDateKey(String key) {
        try {
            return dateKeyFormat.parse(key).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private String getDateTitle(long millis) {

        Calendar today = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);

        if (isSameDay(today, date)) return "Today";

        today.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(today, date)) return "Yesterday";

        return dateHeaderFormat.format(new Date(millis));
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR)
                == b.get(Calendar.DAY_OF_YEAR);
    }
}
