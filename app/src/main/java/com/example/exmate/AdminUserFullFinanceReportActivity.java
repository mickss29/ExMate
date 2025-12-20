package com.example.exmate;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminUserFullFinanceReportActivity extends AppCompatActivity {

    private RecyclerView rvIncome, rvExpense;
    private FloatingActionButton fabExport;

    private String uid, name, email;

    private DatabaseReference userRef;

    private final List<AdminTransactionModel> incomeList = new ArrayList<>();
    private final List<AdminTransactionModel> expenseList = new ArrayList<>();

    private double totalIncome = 0;
    private double totalExpense = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_finance_full);

        uid = getIntent().getStringExtra("uid");
        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");

        rvIncome = findViewById(R.id.recyclerIncome);
        rvExpense = findViewById(R.id.recyclerExpense);
        fabExport = findViewById(R.id.fabExportPdf);

        rvIncome.setLayoutManager(new LinearLayoutManager(this));
        rvExpense.setLayoutManager(new LinearLayoutManager(this));

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadIncome();
        loadExpense();

        fabExport.setOnClickListener(v -> showExportOptions());
    }

    // ================= LOAD DATA =================

    private void loadIncome() {
        userRef.child("incomes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        incomeList.clear();
                        totalIncome = 0;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            double amt = s.child("amount").getValue(Double.class) == null ? 0
                                    : s.child("amount").getValue(Double.class);

                            totalIncome += amt;

                            incomeList.add(new AdminTransactionModel(
                                    amt,
                                    safe(s.child("source").getValue(String.class)),
                                    safe(s.child("paymentMode").getValue(String.class)),
                                    safe(s.child("date").getValue(String.class)),
                                    true
                            ));
                        }

                        rvIncome.setAdapter(new AdminTransactionAdapter(incomeList));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast(error.getMessage());
                    }
                });
    }

    private void loadExpense() {
        userRef.child("expenses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        expenseList.clear();
                        totalExpense = 0;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            double amt = s.child("amount").getValue(Double.class) == null ? 0
                                    : s.child("amount").getValue(Double.class);

                            totalExpense += amt;

                            expenseList.add(new AdminTransactionModel(
                                    amt,
                                    safe(s.child("category").getValue(String.class)),
                                    safe(s.child("paymentMode").getValue(String.class)),
                                    safe(s.child("date").getValue(String.class)),
                                    false
                            ));
                        }

                        rvExpense.setAdapter(new AdminTransactionAdapter(expenseList));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toast(error.getMessage());
                    }
                });
    }

    // ================= EXPORT OPTIONS =================

    private void showExportOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_export, null);
        dialog.setContentView(v);

        v.findViewById(R.id.btnExportPDF).setOnClickListener(x -> {
            exportProfessionalPDF();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnExportCSV).setOnClickListener(x -> {
            exportCSV();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ================= CSV =================

    private void exportCSV() {
        try {
            File file = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "User_Finance_Report.csv"
            );

            FileWriter w = new FileWriter(file);
            w.append("Type,Title,Mode,Date,Amount\n");

            for (AdminTransactionModel i : incomeList) {
                w.append("Income,")
                        .append(i.getTitle()).append(",")
                        .append(i.getMode()).append(",")
                        .append(i.getDate()).append(",")
                        .append(String.valueOf(i.getAmount())).append("\n");
            }

            for (AdminTransactionModel e : expenseList) {
                w.append("Expense,")
                        .append(e.getTitle()).append(",")
                        .append(e.getMode()).append(",")
                        .append(e.getDate()).append(",")
                        .append(String.valueOf(e.getAmount())).append("\n");
            }

            w.close();
            openFile(file, "text/csv");

        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    // ================= PDF (PROFESSIONAL) =================

    private void exportProfessionalPDF() {

        try {
            PdfDocument pdf = new PdfDocument();
            Paint p = new Paint();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            int pageNo = 1;
            PdfDocument.Page page = pdf.startPage(info);
            Canvas c = page.getCanvas();

            int startX = 40;
            int endX = 555;
            int y = 50;

            // ===== TITLE =====
            p.setTextSize(20);
            p.setFakeBoldText(true);
            c.drawText("ExMate – User Financial Report", startX, y, p);

            y += 28;
            p.setTextSize(12);
            p.setFakeBoldText(false);
            c.drawText("Name: " + name, startX, y, p);
            y += 16;
            c.drawText("Email: " + email, startX, y, p);
            y += 16;
            c.drawText("Generated: " +
                            android.text.format.DateFormat
                                    .format("dd MMM yyyy", new Date()),
                    startX, y, p);

            // ===== SUMMARY =====
            y += 28;
            p.setFakeBoldText(true);
            c.drawText("Summary", startX, y, p);
            y += 16;
            p.setFakeBoldText(false);

            c.drawText("Total Income: ₹" + totalIncome, startX, y, p);
            y += 14;
            c.drawText("Total Expense: ₹" + totalExpense, startX, y, p);
            y += 14;
            c.drawText("Balance: ₹" + (totalIncome - totalExpense), startX, y, p);

            // ===== INCOME TABLE =====
            y += 30;
            p.setFakeBoldText(true);
            c.drawText("INCOME DETAILS", startX, y, p);
            y += 14;

            drawTableRow(c, p, startX, endX, y);
            y += 14;

            p.setFakeBoldText(true);
            c.drawText("Date", 45, y, p);
            c.drawText("Source", 160, y, p);
            c.drawText("Mode", 300, y, p);
            c.drawText("Amount", 430, y, p);

            y += 10;
            drawTableRow(c, p, startX, endX, y);
            y += 14;
            p.setFakeBoldText(false);

            for (AdminTransactionModel i : incomeList) {

                if (y > 760) {
                    drawPageFooter(c, p, pageNo);
                    pdf.finishPage(page);

                    pageNo++;
                    page = pdf.startPage(info);
                    c = page.getCanvas();
                    y = 50;
                }

                c.drawText(i.getDate(), 45, y, p);
                c.drawText(i.getTitle(), 160, y, p);
                c.drawText(i.getMode(), 300, y, p);
                c.drawText("₹" + i.getAmount(), 430, y, p);

                y += 12;
                drawTableRow(c, p, startX, endX, y);
                y += 10;
            }
            // ===== TOTAL INCOME ROW =====
            y += 6;
            drawTableRow(c, p, startX, endX, y);
            y += 16;

            p.setFakeBoldText(true);
            c.drawText("TOTAL INCOME", startX + 10, y, p);
            c.drawText("₹ " + totalIncome, endX - 120, y, p);

            y += 14;
            drawTableRow(c, p, startX, endX, y);
            p.setFakeBoldText(false);


            // ===== EXPENSE TABLE =====
            y += 24;
            p.setFakeBoldText(true);
            c.drawText("EXPENSE DETAILS", startX, y, p);
            y += 14;

            drawTableRow(c, p, startX, endX, y);
            y += 14;

            c.drawText("Date", 45, y, p);
            c.drawText("Category", 160, y, p);
            c.drawText("Mode", 300, y, p);
            c.drawText("Amount", 430, y, p);

            y += 10;
            drawTableRow(c, p, startX, endX, y);
            y += 14;
            p.setFakeBoldText(false);

            for (AdminTransactionModel e : expenseList) {

                if (y > 760) {
                    drawPageFooter(c, p, pageNo);
                    pdf.finishPage(page);

                    pageNo++;
                    page = pdf.startPage(info);
                    c = page.getCanvas();
                    y = 50;
                }

                c.drawText(e.getDate(), 45, y, p);
                c.drawText(e.getTitle(), 160, y, p);
                c.drawText(e.getMode(), 300, y, p);
                c.drawText("₹" + e.getAmount(), 430, y, p);

                y += 12;
                drawTableRow(c, p, startX, endX, y);
                y += 10;
            }
            // ===== TOTAL EXPENSE ROW =====
            y += 6;
            drawTableRow(c, p, startX, endX, y);
            y += 16;

            p.setFakeBoldText(true);
            c.drawText("TOTAL EXPENSE", startX + 10, y, p);
            c.drawText("₹ " + totalExpense, endX - 120, y, p);

            y += 14;
            drawTableRow(c, p, startX, endX, y);
            p.setFakeBoldText(false);



// ===== DIGITAL SIGNATURE =====
            y += 30;
            drawSignature(c, p, startX, y);


            // ===== FOOTER =====
            drawPageFooter(c, p, pageNo);
            pdf.finishPage(page);

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "User_Finance_Report.pdf"
            );

            pdf.writeTo(new FileOutputStream(file));
            pdf.close();

            openFile(file, "application/pdf");

        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    // ================= OPEN FILE =================

    private void openFile(File file, String type) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, type);
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);

        } catch (Exception e) {
            toast("No app found to open file");
        }
    }

    private String safe(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }
    private void drawTableRow(Canvas c, Paint p,
                              int startX, int endX,
                              int y) {
        c.drawLine(startX, y, endX, y, p);
    }

    private void drawPageFooter(Canvas c, Paint p, int pageNo) {
        p.setTextSize(10);
        p.setFakeBoldText(false);

        c.drawLine(40, 810, 555, 810, p);
        c.drawText("Generated by ExMate Admin Panel", 180, 828, p);
        c.drawText("Page " + pageNo, 500, 828, p);
    }
    private void drawSignature(Canvas c, Paint p, int startX, int y) {

        String signedTime = android.text.format.DateFormat
                .format("dd MMM yyyy, HH:mm", new Date())
                .toString();

        // Signature line
        c.drawLine(startX, y, startX + 220, y, p);

        y += 14;
        p.setTextSize(11);
        p.setFakeBoldText(false);
        c.drawText("Digitally signed by", startX, y, p);

        y += 14;
        p.setFakeBoldText(true);
        c.drawText("ExMate Admin Panel", startX, y, p);

        y += 14;
        p.setFakeBoldText(false);
        c.drawText("On: " + signedTime, startX, y, p);
    }



}
