package com.example.exmate;

import com.example.exmate.ExpenseModel;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceExpenseParser {

    public static ExpenseModel parse(String rawText) {

        ExpenseModel expense = new ExpenseModel();
        expense.rawText = rawText;

        if (rawText == null || rawText.trim().isEmpty()) {
            expense.confidence = 0.0;
            return expense;
        }

        String text = rawText.toLowerCase(Locale.ROOT).trim();

        // 1) Detect date
        detectDate(expense, text);

        // 2) Detect payment mode
        detectPaymentMode(expense, text);

        // 3) Detect recurring
        detectRecurring(expense, text);

        // 4) Detect amount
        expense.amount = detectAmount(text);

        // 5) Detect note
        expense.note = detectNote(text);

        // 6) Category detection
        expense.category = CategoryMapper.detectCategory(text);


        // 7) Confidence
        expense.confidence = calculateConfidence(expense);

        return expense;
    }

    // Amount detection
    private static double detectAmount(String text) {
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    // Date detection
    private static void detectDate(ExpenseModel expense, String text) {

        Calendar cal = Calendar.getInstance();

        if (text.contains("parso") || text.contains("day before yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -2);
            expense.timestamp = cal.getTimeInMillis();
            expense.dateLabel = "day_before_yesterday";
            return;
        }

        if (text.contains("kal") || text.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
            expense.timestamp = cal.getTimeInMillis();
            expense.dateLabel = "yesterday";
            return;
        }

        if (text.contains("aaj") || text.contains("today")) {
            expense.timestamp = cal.getTimeInMillis();
            expense.dateLabel = "today";
        }
    }

    // Payment detection
    private static void detectPaymentMode(ExpenseModel expense, String text) {

        if (text.contains("upi") ||
                text.contains("gpay") ||
                text.contains("google pay") ||
                text.contains("phonepe") ||
                text.contains("paytm")) {
            expense.paymentMode = "UPI";
            return;
        }

        if (text.contains("cash")) {
            expense.paymentMode = "Cash";
            return;
        }

        if (text.contains("card") ||
                text.contains("debit") ||
                text.contains("credit")) {
            expense.paymentMode = "Card";
        }
    }

    // Recurring detection
    private static void detectRecurring(ExpenseModel expense, String text) {

        if (text.contains("monthly") || text.contains("every month")) {
            expense.isRecurring = true;
            expense.recurringType = "monthly";
            return;
        }

        if (text.contains("weekly") || text.contains("every week")) {
            expense.isRecurring = true;
            expense.recurringType = "weekly";
            return;
        }

        if (text.contains("daily") || text.contains("every day")) {
            expense.isRecurring = true;
            expense.recurringType = "daily";
        }
    }

    // Note detection
    private static String detectNote(String text) {

        String clean = text;

        // Remove date words
        clean = clean.replace("today", "")
                .replace("aaj", "")
                .replace("yesterday", "")
                .replace("kal", "")
                .replace("parso", "")
                .replace("day before yesterday", "");

        // Remove payment words
        clean = clean.replace("upi", "")
                .replace("gpay", "")
                .replace("google pay", "")
                .replace("phonepe", "")
                .replace("paytm", "")
                .replace("cash", "")
                .replace("card", "")
                .replace("debit", "")
                .replace("credit", "");

        // Remove recurring words
        clean = clean.replace("monthly", "")
                .replace("every month", "")
                .replace("weekly", "")
                .replace("every week", "")
                .replace("daily", "")
                .replace("every day", "");

        // Remove currency words
        clean = clean.replace("rupees", "")
                .replace("rupaye", "")
                .replace("rs", "")
                .replace("₹", "");

        // Remove numbers
        clean = clean.replaceAll("(\\d+(?:\\.\\d{1,2})?)", "");

        // Clean spaces
        clean = clean.replaceAll("\\s+", " ").trim();

        if (clean.isEmpty()) return "Expense";

        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    // Confidence
    private static double calculateConfidence(ExpenseModel e) {

        double score = 0.0;

        if (e.amount > 0) score += 0.55;
        if (e.note != null && !e.note.equalsIgnoreCase("Expense")) score += 0.20;
        if (e.category != null && !e.category.equalsIgnoreCase("Other")) score += 0.15;
        if (e.paymentMode != null && !e.paymentMode.equalsIgnoreCase("Unknown")) score += 0.10;

        if (score > 1.0) score = 1.0;
        return score;
    }
}
