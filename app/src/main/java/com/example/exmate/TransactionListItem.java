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
    private String amount;
    private String meta;
    private boolean highSpend;

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

    public String getMeta() {
        return meta;
    }

    public boolean isHighSpend() {
        return highSpend;
    }

    // 🔥 NEW GETTER
    public long getTimeMillis() {
        return timeMillis;
    }

    // ---------- SETTERS ----------
    public void setDateTitle(String dateTitle) {
        this.dateTitle = dateTitle;
    }

    public void setTransaction(
            String category,
            String note,
            String amount,
            String meta,
            boolean highSpend
    ) {
        this.category = category;
        this.note = note;
        this.amount = amount;
        this.meta = meta;
        this.highSpend = highSpend;
    }

    // 🔥 NEW SETTER
    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }
}
