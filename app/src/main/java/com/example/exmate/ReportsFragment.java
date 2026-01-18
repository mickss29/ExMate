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
    private boolean isCustomRangeSelected = false;
    private final Map<String, Double> budgetMap = new HashMap<>();


    // ================= UI =================
    private RecyclerView recyclerReports;
    private TextView tvEmpty, txtDateRange;
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

    private final SimpleDateFormat dateKeyFormat =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat dateHeaderFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    class ExportTxn {
        String dateTime;
        String category;
        double amount;

        ExportTxn(String dateTime, String category, double amount) {
            this.dateTime = dateTime;
            this.category = category;
            this.amount = amount;
        }
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.reports_fragment, container, false);

        recyclerReports = view.findViewById(R.id.recyclerReports);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        txtDateRange = view.findViewById(R.id.txtDateRange);
        chipAll = view.findViewById(R.id.chipAll);
        chipIncome = view.findViewById(R.id.chipIncome);
        chipExpense = view.findViewById(R.id.chipExpense);
        fabExport = view.findViewById(R.id.fabExport);

        recyclerReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transactionList);
        recyclerReports.setAdapter(adapter);

        setupFirebase();
        setupChips();
        attachSwipe();
        loadTransactions();
        loadBudgets();


        txtDateRange.setOnClickListener(v -> pickDateRange());
        fabExport.setOnClickListener(v -> showExportSheet());

        return view;
    }

    private void setupFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);
    }

    private void setupChips() {
        chipAll.setChecked(true);
        chipAll.setOnClickListener(v -> { selectedFilterType = TxnFilterType.ALL; applyFilter(); });
        chipIncome.setOnClickListener(v -> { selectedFilterType = TxnFilterType.INCOME; applyFilter(); });
        chipExpense.setOnClickListener(v -> { selectedFilterType = TxnFilterType.EXPENSE; applyFilter(); });
    }

    // ================= LOAD DATA =================
    private void loadTransactions() {

        DatabaseReference incomeRef = userRef.child("incomes");
        DatabaseReference expenseRef = userRef.child("expenses");

        incomeRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot iSnap) {
                expenseRef.addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot eSnap) {

                        masterList.clear();
                        Map<String, List<TransactionListItem>> map =
                                new TreeMap<>(Collections.reverseOrder());

                        for (DataSnapshot s : iSnap.getChildren()) {
                            addItem(map, s, true);
                        }
                        for (DataSnapshot s : eSnap.getChildren()) {
                            addItem(map, s, false);
                        }

                        for (String k : map.keySet()) {
                            TransactionListItem h =
                                    new TransactionListItem(TransactionListItem.TYPE_DATE);
                            h.setDateTitle(dateHeaderFormat.format(parseDateKey(k)));
                            masterList.add(h);
                            masterList.addAll(map.get(k));
                        }

                        applyFilter();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void addItem(Map<String, List<TransactionListItem>> map,
                         DataSnapshot snap, boolean income) {

        Long time = snap.child("time").getValue(Long.class);
        Double amount = snap.child("amount").getValue(Double.class);
        if (time == null || amount == null) return;

        String title = income
                ? snap.child("source").getValue(String.class)
                : snap.child("category").getValue(String.class);

        String note = snap.child("note").getValue(String.class);

        String key = dateKeyFormat.format(new Date(time));
        map.putIfAbsent(key, new ArrayList<>());

        TransactionListItem item =
                new TransactionListItem(TransactionListItem.TYPE_TRANSACTION);

        item.setTransaction(
                title == null ? (income ? "Income" : "Expense") : title,
                note == null ? "" : note,
                (income ? "₹ " : "- ₹ ") + Math.round(amount),
                dateHeaderFormat.format(new Date(time)) + " • " +
                        timeFormat.format(new Date(time)),
                income
        );

        item.setTimeMillis(time);
        map.get(key).add(item);
    }

    // ================= FILTER =================
    private void applyFilter() {

        transactionList.clear();
        String lastHeader = null;

        for (TransactionListItem item : masterList) {

            if (item.getType() == TransactionListItem.TYPE_DATE) {
                lastHeader = item.getDateTitle();
                continue;
            }

            long t = item.getTimeMillis();
            if (startDateMillis != -1 &&
                    (t < startDateMillis || t > endDateMillis)) continue;

            boolean match =
                    selectedFilterType == TxnFilterType.ALL ||
                            (selectedFilterType == TxnFilterType.INCOME && item.isIncome()) ||
                            (selectedFilterType == TxnFilterType.EXPENSE && !item.isIncome());

            if (!match) continue;

            if (lastHeader != null) {
                TransactionListItem h =
                        new TransactionListItem(TransactionListItem.TYPE_DATE);
                h.setDateTitle(lastHeader);
                transactionList.add(h);
                lastHeader = null;
            }

            transactionList.add(item);
        }

        adapter.notifyDataSetChanged();
        showEmpty(transactionList.isEmpty());
    }

    // ================= DATE PICKER =================
    private void pickDateRange() {

        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;

            startDateMillis = getStartOfDay(selection.first);
            endDateMillis = getEndOfDay(selection.second);

            txtDateRange.setText(
                    dateHeaderFormat.format(new Date(startDateMillis)) +
                            " - " +
                            dateHeaderFormat.format(new Date(endDateMillis))
            );

            // 🔥 IMPORTANT
            isCustomRangeSelected = true;

            applyFilter();
        });


        picker.show(getParentFragmentManager(), "DATE");
    }

    private long getStartOfDay(long m) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(m);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getEndOfDay(long m) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(m);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private void showExportSheet() {

        BottomSheetDialog d = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_export, null);
        d.setContentView(v);

        View btnThisMonth = v.findViewById(R.id.optionThisMonth);
        View btnLastMonth = v.findViewById(R.id.optionLastMonth);
        View btnExportPdf = v.findViewById(R.id.optionPdf);
        View btnClearRange = v.findViewById(R.id.optionClearRange);

        // ================= STATE CONTROL =================
        if (isCustomRangeSelected) {
            // Disable month buttons
            disableView(btnThisMonth);
            disableView(btnLastMonth);

            // Enable custom export + clear
            btnClearRange.setVisibility(View.VISIBLE);
        } else {
            // Disable custom export + hide clear
            disableView(btnExportPdf);
            btnClearRange.setVisibility(View.GONE);
        }

        // ================= CLICK HANDLERS =================

        btnThisMonth.setOnClickListener(v1 -> {
            if (isCustomRangeSelected) return;
            d.dismiss();
            applyThisMonth();
            exportPdf();
        });

        btnLastMonth.setOnClickListener(v1 -> {
            if (isCustomRangeSelected) return;
            d.dismiss();
            applyLastMonth();
            exportPdf();
        });

        btnExportPdf.setOnClickListener(v1 -> {
            if (!isCustomRangeSelected) return;
            d.dismiss();
            exportPdf();
        });

        btnClearRange.setOnClickListener(v1 -> {
            d.dismiss();
            clearCustomRange();
        });

        v.findViewById(R.id.btnCancel)
                .setOnClickListener(v1 -> d.dismiss());

        d.show();
    }
    private void clearCustomRange() {
        startDateMillis = -1;
        endDateMillis = -1;
        isCustomRangeSelected = false;

        txtDateRange.setText("Select date range");
        applyFilter();

        Toast.makeText(
                getContext(),
                "Custom range cleared",
                Toast.LENGTH_SHORT
        ).show();
    }
    private void autoClearRangeAfterExport() {
        if (!isCustomRangeSelected) return;

        startDateMillis = -1;
        endDateMillis = -1;
        isCustomRangeSelected = false;

        txtDateRange.setText("Select date range");
        applyFilter();
    }


    private void exportPdf() {

        if (transactionList.isEmpty()) {
            Toast.makeText(getContext(),
                    "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        List<ExportTxn> incomes = new ArrayList<>();
        List<ExportTxn> expenses = new ArrayList<>();


        double totalIncome = 0;
        double totalExpense = 0;

        for (TransactionListItem item : transactionList) {

            if (item.getType() != TransactionListItem.TYPE_TRANSACTION)
                continue;

            double amount = item.getAmountValue();
            long timeMillis = item.getTimeMillis();

            String dateTime =
                    dateHeaderFormat.format(new Date(timeMillis)) +
                            " • " +
                            timeFormat.format(new Date(timeMillis));

            String category = item.getCategory();

            if (item.isIncome()) {
                incomes.add(new ExportTxn(dateTime, category, amount));
                totalIncome += amount;
            } else {
                expenses.add(new ExportTxn(dateTime, category, amount));
                totalExpense += amount;
            }
        }


        generateCorporatePdf(
                incomes,
                expenses,

                totalIncome,
                totalExpense
        );

        // 🔄 AUTO-CLEAR ONLY IF CUSTOM RANGE WAS USED
        if (isCustomRangeSelected) {
            startDateMillis = -1;
            endDateMillis = -1;
            isCustomRangeSelected = false;
            txtDateRange.setText("Select date range");
            applyFilter();
        }
    }



    private void generateCorporatePdf(
            List<ExportTxn> incomes,
            List<ExportTxn> expenses,

            double totalIncome,
            double totalExpense
    ) {

        PdfDocument pdf = new PdfDocument();

        // =====================================================
        // PAGE 1 : SUMMARY (NO SUGGESTION HERE)
        // =====================================================
        PdfDocument.PageInfo summaryInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page1 = pdf.startPage(summaryInfo);
        Canvas c = page1.getCanvas();

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint incomePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint expensePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        title.setTextSize(26);
        title.setFakeBoldText(true);

        text.setTextSize(13);

        incomePaint.setColor(0xFF2E7D32);
        expensePaint.setColor(0xFFC62828);
        highlightPaint.setColor(0xFFFFD54F);
        highlightPaint.setTextSize(13);

        int y = 60;

        // HEADER
        c.drawText("ExMate – Financial Summary", 40, y, title);
        y += 35;
        c.drawText("Period: " + txtDateRange.getText().toString(), 40, y, text);
        y += 35;

        // TOTALS
        c.drawText("Total Income:", 40, y, text);
        c.drawText("₹ " + String.format("%.2f", totalIncome), 360, y, incomePaint);
        y += 22;

        c.drawText("Total Expense:", 40, y, text);
        c.drawText("₹ " + String.format("%.2f", totalExpense), 360, y, expensePaint);
        y += 22;

        double net = totalIncome - totalExpense;
        Paint netPaint = net >= 0 ? incomePaint : expensePaint;
        c.drawText("Net Balance:", 40, y, text);
        c.drawText("₹ " + String.format("%.2f", net), 360, y, netPaint);

        // PIE DATA
        Map<String, Double> incomeMap = new LinkedHashMap<>();
        Map<String, Double> expenseMap = new LinkedHashMap<>();

        for (ExportTxn t : incomes) {
            incomeMap.put(
                    t.category,
                    incomeMap.getOrDefault(t.category, 0.0) + t.amount
            );
        }

        for (ExportTxn t : expenses) {
            expenseMap.put(
                    t.category,
                    expenseMap.getOrDefault(t.category, 0.0) + t.amount
            );
        }


        // HIGHEST INCOME
        String topIncomeCat = "";
        double topIncomeVal = 0;
        for (String k : incomeMap.keySet()) {
            if (incomeMap.get(k) > topIncomeVal) {
                topIncomeVal = incomeMap.get(k);
                topIncomeCat = k;
            }
        }

        // HIGHEST EXPENSE
        String topExpenseCat = "";
        double topExpenseVal = 0;
        for (String k : expenseMap.keySet()) {
            if (expenseMap.get(k) > topExpenseVal) {
                topExpenseVal = expenseMap.get(k);
                topExpenseCat = k;
            }
        }

        int[] colors = {
                0xFF1E88E5, 0xFF43A047, 0xFFFDD835,
                0xFFE53935, 0xFF8E24AA, 0xFFFF7043
        };

        // INCOME PIE
        y += 45;
        c.drawText("Income Distribution", 40, y, text);
        drawPieChart(c, incomeMap, totalIncome, 60, y + 15, 220, colors, text);
        y += 260;
        c.drawText("⭐ Highest Income: " + topIncomeCat +
                " (₹ " + String.format("%.2f", topIncomeVal) + ")", 40, y, highlightPaint);

        // EXPENSE PIE
        y += 40;
        c.drawText("Expense Distribution", 40, y, text);
        drawPieChart(c, expenseMap, totalExpense, 60, y + 15, 220, colors, text);
        y += 260;
        c.drawText("⚠ Highest Expense: " + topExpenseCat +
                " (₹ " + String.format("%.2f", topExpenseVal) + ")", 40, y, highlightPaint);

        pdf.finishPage(page1);

        // =====================================================
        // PAGE 2 : DETAILS + FINAL SUGGESTION
        // =====================================================
        PdfDocument.PageInfo detailInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 2).create();
        PdfDocument.Page page2 = pdf.startPage(detailInfo);
        Canvas canvas = page2.getCanvas();

        Paint bold = new Paint();
        Paint normal = new Paint();
        Paint line = new Paint();
        Paint headerBg = new Paint();

        bold.setTextSize(12);
        bold.setFakeBoldText(true);
        normal.setTextSize(11);
        line.setStrokeWidth(1);
        headerBg.setARGB(255, 220, 220, 220);

        int yy = 50;

        canvas.drawText("Income Expense Report", 40, yy, bold);
        yy += 20;
        canvas.drawLine(40, yy, 555, yy, line);
        yy += 20;

        // INCOME TABLE
        canvas.drawText("Income Details", 40, yy, bold);
        yy += 10;
        canvas.drawRect(40, yy, 555, yy + 25, headerBg);
        canvas.drawText("Date & Time", 50, yy + 17, bold);
        canvas.drawText("Category", 220, yy + 17, bold);
        canvas.drawText("Amount", 450, yy + 17, bold);


        yy += 35;
        for (ExportTxn t : incomes) {
            canvas.drawText(t.dateTime, 50, yy, normal);
            canvas.drawText(t.category, 220, yy, normal);
            canvas.drawText("₹ " + String.format("%.2f", t.amount), 450, yy, normal);
            yy += 18;
        }


        yy += 30;

        // EXPENSE TABLE
        canvas.drawText("Expense Details", 40, yy, bold);
        yy += 10;
        canvas.drawRect(40, yy, 555, yy + 25, headerBg);
        canvas.drawText("Date & Time", 50, yy + 17, bold);
        canvas.drawText("Category", 220, yy + 17, bold);
        canvas.drawText("Amount", 450, yy + 17, bold);

        yy += 35;
        for (ExportTxn t : expenses) {
            canvas.drawText(t.dateTime, 50, yy, normal);
            canvas.drawText(t.category, 220, yy, normal);
            canvas.drawText("₹ " + String.format("%.2f", t.amount), 450, yy, normal);
            yy += 18;
        }


        // =====================================================
// MULTI CATEGORY BUDGET INSIGHTS (END OF PDF)
// =====================================================

        List<String> insightLines = new ArrayList<>();
        String dominantCategoryNote = "";
        for (String cat : expenseMap.keySet()) {
            double percent = (expenseMap.get(cat) / totalExpense) * 100;
            if (percent > 70) {
                dominantCategoryNote =
                        "⚠ " + cat + " accounts for most of your expenses (" +
                                String.format("%.1f", percent) +
                                "%). This category needs immediate attention.";
                break;
            }
        }


        for (String category : expenseMap.keySet()) {

            if (!budgetMap.containsKey(category)) continue;

            double spent = expenseMap.get(category);
            double budget = budgetMap.get(category);

            if (budget <= 0 || spent <= budget) continue;

            double overAmount = spent - budget;

            // 🔥 DECLARE IT HERE (THIS WAS MISSING)
            double overPercentRaw = (overAmount / budget) * 100;

            String percentText = overPercentRaw > 300
                    ? "300%+"
                    : String.format("%.0f", overPercentRaw) + "%";

            insightLines.add(
                    "• " + category +
                            " exceeded budget by ₹ " +
                            String.format("%.0f", overAmount) +
                            " (" + percentText + ")\n" +
                            "  " + getCategorySmartTip(category, overPercentRaw)
            );
        }

// Draw insights only if any exist
        if (!insightLines.isEmpty()) {

            // Check space to prevent cutting
            if (yy > 680) {
                pdf.finishPage(page2);
                PdfDocument.PageInfo extra =
                        new PdfDocument.PageInfo.Builder(595, 842, 3).create();
                page2 = pdf.startPage(extra);
                canvas = page2.getCanvas();
                yy = 60;
            }

            Paint boxPaint = new Paint();
            boxPaint.setColor(0xFFE3F2FD); // soft blue

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(12);
            textPaint.setColor(0xFF0D47A1);

            int boxTop = yy;
            int maxHeight = 300;
            int calculatedHeight = 40 + (insightLines.size() * 22) + 40;
            int boxHeight = Math.min(calculatedHeight, maxHeight);

            canvas.drawRect(40, boxTop, 555, boxTop + boxHeight, boxPaint);

            int textY = boxTop + 24;
            canvas.drawText("🧠 Budget Insights", 52, textY, textPaint);
            textY += 22;
            if (!dominantCategoryNote.isEmpty()) {
                canvas.drawText(dominantCategoryNote, 52, textY, textPaint);
                textY += 22;
            }


            for (String insight : insightLines) {
                canvas.drawText(insight, 52, textY, textPaint);
                textY += 20;
            }

            textY += 10;
            canvas.drawText(
                    "Suggestion: Focus on high overspending categories to improve savings.",
                    52, textY, textPaint
            );

            yy = boxTop + boxHeight + 20;
        }

        pdf.finishPage(page2);
        savePdf(pdf);
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

        try {
            startActivity(Intent.createChooser(i, "Open PDF"));
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "No PDF viewer installed", Toast.LENGTH_LONG).show();
        }
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
    private void savePdf(PdfDocument pdf) {
        try {
            File dir = new File(
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "ExMate"
            );

            if (!dir.exists()) {
                dir.mkdirs();
            }

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
            Toast.makeText(
                    getContext(),
                    "PDF export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private void disableView(View v) {
        v.setAlpha(0.4f);
        v.setEnabled(false);
    }

    private void applyThisMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        startDateMillis = getStartOfDay(c.getTimeInMillis());
        endDateMillis = getEndOfDay(System.currentTimeMillis());
        isCustomRangeSelected = false;
        txtDateRange.setText("This Month");
        applyFilter();
    }

    private void applyLastMonth() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        startDateMillis = getStartOfDay(c.getTimeInMillis());

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDateMillis = getEndOfDay(c.getTimeInMillis());

        isCustomRangeSelected = false;
        txtDateRange.setText("Last Month");
        applyFilter();
    }
    private void drawPieChart(
            Canvas c,
            Map<String, Double> data,
            double total,
            int left, int top, int size,
            int[] colors,
            Paint text
    ) {
        Paint piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float startAngle = 0f;
        int colorIndex = 0;

        int legendX = left + size + 25;
        int legendY = top + 20;

        for (String key : data.keySet()) {

            double value = data.get(key);
            float sweep = (float) ((value / total) * 360f);

            piePaint.setColor(colors[colorIndex % colors.length]);

            c.drawArc(
                    left,
                    top,
                    left + size,
                    top + size,
                    startAngle,
                    sweep,
                    true,
                    piePaint
            );

            // LEGEND
            c.drawRect(
                    legendX,
                    legendY - 10,
                    legendX + 16,
                    legendY + 6,
                    piePaint
            );

            double percent = (value / total) * 100;
            c.drawText(
                    key + " (" + String.format("%.1f", percent) + "%)",
                    legendX + 22,
                    legendY + 5,
                    text
            );

            legendY += 20;
            startAngle += sweep;
            colorIndex++;
        }
    }
    private void loadBudgets() {
        if (userRef == null) return;

        // Get current month key like "2026-01"
        SimpleDateFormat monthFormat =
                new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonthKey = monthFormat.format(new Date());

        userRef.child("budgets")
                .child(currentMonthKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        budgetMap.clear();

                        if (!snapshot.exists()) {
                            // No budgets set for this month
                            return;
                        }

                        for (DataSnapshot s : snapshot.getChildren()) {
                            Object val = s.getValue();
                            if (val instanceof Number) {
                                budgetMap.put(
                                        s.getKey(),
                                        ((Number) val).doubleValue()
                                );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // ignore safely
                    }
                });
    }

    private String generateBudgetSuggestion(
            String category,
            double spent,
            double budget
    ) {
        if (budget <= 0 || spent <= budget) return "";

        double overAmount = spent - budget;
        double overPercentRaw = (overAmount / budget) * 100;
        String percentText = overPercentRaw > 300
                ? "300%+"
                : String.format("%.0f", overPercentRaw) + "%";

        return "🧠 Budget Alert: You exceeded your "
                + category + " budget by ₹"
                + String.format(Locale.getDefault(), "%.0f", overAmount)
                + " (" + percentText + ")"
                + "%). Consider reducing spending next month.";
    }

    private String getCategorySmartTip(String category, double overPercent) {

        boolean extreme = overPercent > 200;

        switch (category.toLowerCase(Locale.getDefault())) {

            case "food":
                return extreme
                        ? "Tip: Food spending is extremely high. Reduce outside meals immediately and set weekly limits."
                        : "Tip: Frequent outside meals add up. Try planning weekly meals.";

            case "transport":
            case "travel":
                return extreme
                        ? "Tip: Travel costs are far above normal. Avoid non-essential trips this month."
                        : "Tip: Reduce unnecessary travel and prefer cost-effective options.";

            case "bills":
                return extreme
                        ? "Tip: Bills are unusually high. Review subscriptions and energy usage urgently."
                        : "Tip: Check for cheaper plans or reduce unnecessary usage.";

            case "shopping":
                return extreme
                        ? "Tip: Shopping overspend is severe. Pause non-essential purchases this month."
                        : "Tip: Avoid impulse purchases. Waiting 24 hours helps.";

            default:
                return extreme
                        ? "Tip: This category needs strict control due to heavy overspending."
                        : "Tip: Monitor this category closely to stay within budget.";
        }
    }






}
