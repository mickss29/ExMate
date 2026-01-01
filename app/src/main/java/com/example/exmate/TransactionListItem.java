package com.example.exmate;

public class TransactionListItem {

    // ---------- VIEW TYPES ----------
    public static final int TYPE_DATE = 0;
    public static final int TYPE_TRANSACTION = 1;

    private int type;

    // ---------- DATE HEADER ----------
    private String dateTitle;

    // ---------- TRANSACTION ----------
    private String category;
    private String note;

    // ⚠️ DISPLAY VALUE (unchanged)
    private String amount;

    // ✅ REAL NUMERIC VALUE (NEW – PREMIUM)
    private double amountValue;

    private String meta;
    private boolean highSpend;

    // ✅ REAL TRANSACTION TYPE (FIXED)
    private boolean income;   // true = income, false = expense

    // 🔥 IMPORTANT: REAL TRANSACTION TIME
    private long timeMillis;

    // ---------- CONSTRUCTOR ----------
    public TransactionListItem(int type) {
        this.type = type;
    }

    // ---------- GETTERS ----------
    public int getType() {
        return type;
    }

    public String getDateTitle() {
        return dateTitle;
    }

    public String getCategory() {
        return category;
    }

    public String getNote() {
        return note;
    }

    public String getAmount() {
        return amount;
    }

    public double getAmountValue() {
        return amountValue;
    }

    public String getMeta() {
        return meta;
    }

    public boolean isHighSpend() {
        return highSpend;
    }

    // ✅ CORRECT INCOME / EXPENSE CHECK
    public boolean isIncome() {
        return income;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    // ---------- SETTERS ----------
    public void setDateTitle(String dateTitle) {
        this.dateTitle = dateTitle;
    }

    /**
     * MAIN TRANSACTION SETTER
     * (Backward compatible + future ready)
     */
    public void setTransaction(
            String category,
            String note,
            String amount,
            String meta,
            boolean isIncome
    ) {
        this.category = category;
        this.note = note;
        this.amount = amount;
        this.meta = meta;
        this.income = isIncome;

        // ✅ SAFE numeric extraction
        try {
            this.amountValue = Double.parseDouble(
                    amount.replace("₹", "")
                            .replace("+", "")
                            .replace("-", "")
                            .trim()
            );
        } catch (Exception e) {
            this.amountValue = 0;
        }

        // ⭐ Premium rule (can be changed)
        this.highSpend = amountValue >= 1000;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }
}
