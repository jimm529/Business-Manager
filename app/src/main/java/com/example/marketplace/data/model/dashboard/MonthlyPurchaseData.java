package com.example.marketplace.data.model.dashboard;

/** How much was bought in one calendar month: one bar of the "Monthly Purchase" chart. */
public class MonthlyPurchaseData {

    private final int year;
    /** 0 = January ... 11 = December (same numbering as java.util.Calendar). */
    private final int month;
    private final double amount;

    public MonthlyPurchaseData(int year, int month, double amount) {
        this.year = year;
        this.month = month;
        this.amount = amount;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public double getAmount() { return amount; }
}
