package com.example.exmate;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsFragment extends Fragment {

    // ===== FILTER ENUM =====
    private enum TxnFilterType { ALL, INCOME, EXPENSE }
    private TxnFilterType selectedFilterType = TxnFilterType.ALL;

    // ================= UI =================
    private RecyclerView recyclerReports;
    private TextView tvEmpty, txtDateRange;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipIncome, chipExpense;
    private ExtendedFloatingActionButton fabExport;

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

    // ================= FRAGMENT =================
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.reports_fragment, container, false);

        initViews(view);
        setupFirebase();
        setupRecycler();
        setupChips();
        attachSwipe();
        loadTransactions();

        txtDateRange.setOnClickListener(v -> pickDateRange());
        fabExport.setOnClickListener(v -> showExportSheet());

        return view;
    }

    // ================= INIT =================
    private void initViews(View view) {
        recyclerReports = view.findViewById(R.id.recyclerReports);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        txtDateRange = view.findViewById(R.id.txtDateRange);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipAll = view.findViewById(R.id.chipAll);
        chipIncome = view.findViewById(R.id.chipIncome);
        chipExpense = view.findViewById(R.id.chipExpense);
        fabExport = view.findViewById(R.id.fabExport);
    }

    private void setupRecycler() {
        recyclerReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transactionList);
        recyclerReports.setAdapter(adapter);
    }

    private void setupChips() {
        chipAll.setChecked(true);
        chipGroupFilter.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.chipIncome) selectedFilterType = TxnFilterType.INCOME;
            else if (id == R.id.chipExpense) selectedFilterType = TxnFilterType.EXPENSE;
            else selectedFilterType = TxnFilterType.ALL;
            applyDateFilter();
        });
    }

    // ================= FIREBASE =================
    private void setupFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);
    }

    // ================= LOAD TRANSACTIONS =================
    private void loadTransactions() {

        masterList.clear();
        transactionList.clear();

        Map<String, List<TransactionListItem>> grouped =
                new TreeMap<>(Collections.reverseOrder());

        DatabaseReference incomeRef = userRef.child("incomes");
        DatabaseReference expenseRef = userRef.child("expenses");

        incomeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

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

                    item.setTransaction(
                            source == null ? "Income" : source,
                            note == null ? "" : note,
                            "₹ " + amount.intValue(),
                            dateHeaderFormat.format(new Date(time)) + " • " +
                                    timeFormat.format(new Date(time)) + " • Income",
                            amount >= 1000
                    );

                    item.setTimeMillis(time);
                    grouped.get(key).add(item);
                }

                expenseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot snap : snapshot.getChildren()) {

                            Long time = snap.child("time").getValue(Long.class);
                            Double amount = snap.child("amount").getValue(Double.class);
                            String category = snap.child("category").getValue(String.class);
                            String note = snap.child("note").getValue(String.class);

                            if (time == null || amount == null) continue;

                            String key = dateKeyFormat.format(new Date(time));
                            grouped.putIfAbsent(key, new ArrayList<>());

                            TransactionListItem item =
                                    new TransactionListItem(TransactionListItem.TYPE_TRANSACTION);

                            item.setTransaction(
                                    category == null ? "Expense" : category,
                                    note == null ? "" : note,
                                    "- ₹" + amount.intValue(),
                                    dateHeaderFormat.format(new Date(time)) + " • " +
                                            timeFormat.format(new Date(time)) + " • Expense",
                                    amount >= 1000
                            );

                            item.setTimeMillis(time);
                            grouped.get(key).add(item);
                        }

                        masterList.clear();
                        for (String key : grouped.keySet()) {
                            TransactionListItem header =
                                    new TransactionListItem(TransactionListItem.TYPE_DATE);
                            header.setDateTitle(dateHeaderFormat.format(parseDateKey(key)));
                            masterList.add(header);
                            masterList.addAll(grouped.get(key));
                        }

                        applyDateFilter();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        showEmpty(true);
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                showEmpty(true);
            }
        });
    }

    // ================= FILTER =================
    private void applyDateFilter() {

        transactionList.clear();
        String header = null;
        boolean headerAdded = false;

        for (TransactionListItem item : masterList) {

            if (item.getType() == TransactionListItem.TYPE_DATE) {
                header = item.getDateTitle();
                headerAdded = false;
                continue;
            }

            long time = item.getTimeMillis();

            boolean dateMatch =
                    (startDateMillis == -1 || endDateMillis == -1) ||
                            (time >= startDateMillis && time <= endDateMillis);

            if (!dateMatch) continue;

            boolean isIncome = !item.getAmount().startsWith("-");
            boolean typeMatch =
                    selectedFilterType == TxnFilterType.ALL ||
                            (selectedFilterType == TxnFilterType.INCOME && isIncome) ||
                            (selectedFilterType == TxnFilterType.EXPENSE && !isIncome);

            if (!typeMatch) continue;

            if (!headerAdded && header != null) {
                TransactionListItem h =
                        new TransactionListItem(TransactionListItem.TYPE_DATE);
                h.setDateTitle(header);
                transactionList.add(h);
                headerAdded = true;
            }

            transactionList.add(item);
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

        picker.show(getParentFragmentManager(), "DATE_RANGE");
    }

    private long getStartOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(11,0); c.set(12,0); c.set(13,0); c.set(14,0);
        return c.getTimeInMillis();
    }

    private long getEndOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(11,23); c.set(12,59); c.set(13,59); c.set(14,999);
        return c.getTimeInMillis();
    }

    // ================= EXPORT =================
    private void showExportSheet() {
        BottomSheetDialog d = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_export, null);
        d.setContentView(v);

        v.findViewById(R.id.optionPdf).setOnClickListener(b -> {
            d.dismiss();
            exportPdf();
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(b -> d.dismiss());
        d.show();
    }

    private void exportPdf() {

        if (startDateMillis == -1 || endDateMillis == -1) {
            Toast.makeText(getContext(), "Select date range first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (transactionList.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
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
                expenses.add(new Pair<>(item.getCategory(), amount));
                totalExpense += amount;
            } else {
                incomes.add(new Pair<>(item.getCategory(), amount));
                totalIncome += amount;
            }
        }

        // 🔥 SAME OLD CORPORATE PDF
        generateCorporatePdf(
                incomes,
                expenses,
                totalIncome,
                totalExpense
        );
    }


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
        Paint incomePaint = new Paint();
        Paint expensePaint = new Paint();

        title.setFakeBoldText(true);
        title.setTextSize(26);

        bold.setFakeBoldText(true);
        bold.setTextSize(12);

        text.setTextSize(11);

        incomePaint.setTextSize(11);
        incomePaint.setColor(0xFF2E7D32);   // GREEN

        expensePaint.setTextSize(11);
        expensePaint.setColor(0xFFC62828);  // RED

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

        // ===== INCOME TABLE =====
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

        // ===== EXPENSE TABLE =====
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


    private void savePdf(PdfDocument pdf) {
        try {
            File dir = new File(
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "ExMate"
            );
            if (!dir.exists()) dir.mkdirs();

            File file = new File(
                    dir,
                    "ExMate_Report_" + System.currentTimeMillis() + ".pdf"
            );

            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            openFile(file);

        } catch (Exception e) {
            Toast.makeText(getContext(), "PDF export failed", Toast.LENGTH_LONG).show();
        }
    }

    private void openFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/pdf");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerReports.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private Date parseDateKey(String key) {
        try { return dateKeyFormat.parse(key); }
        catch (Exception e) { return new Date(); }
    }

    private void attachSwipe() {
        ItemTouchHelper.SimpleCallback cb =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override public boolean onMove(
                            RecyclerView r, RecyclerView.ViewHolder v, RecyclerView.ViewHolder t) {
                        return false;
                    }
                    @Override public void onSwiped(
                            RecyclerView.ViewHolder vh, int d) {
                        adapter.notifyItemChanged(vh.getAdapterPosition());
                    }
                };
        new ItemTouchHelper(cb).attachToRecyclerView(recyclerReports);
    }
}
