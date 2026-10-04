package com.example.marketplace.data.model.dashboard;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** The numbers on the top cards plus the six bars of the Monthly Purchase chart. */
public class DashboardSummary {

    private final double totalSpend;
    private final int activeOrdersCount;
    /** Metric tons bought in the current calendar month. */
    private final double monthlyVolume;
    private final List<MonthlyPurchaseData> monthlyPurchases;

    public DashboardSummary(double totalSpend, int activeOrdersCount, double monthlyVolume,
                            @NonNull List<MonthlyPurchaseData> monthlyPurchases) {
        this.totalSpend = totalSpend;
        this.activeOrdersCount = activeOrdersCount;
        this.monthlyVolume = monthlyVolume;
        this.monthlyPurchases = new ArrayList<>(monthlyPurchases);
    }

    public double getTotalSpend() { return totalSpend; }
    public int getActiveOrdersCount() { return activeOrdersCount; }
    public double getMonthlyVolume() { return monthlyVolume; }

    /** Oldest month first, always six entries. */
    @NonNull
    public List<MonthlyPurchaseData> getMonthlyPurchases() { return monthlyPurchases; }

    /** True when nothing was bought in any of the months, so the chart has nothing to draw. */
    public boolean hasNoPurchases() {
        for (MonthlyPurchaseData month : monthlyPurchases) {
            if (month.getAmount() > 0) {
                return false;
            }
        }
        return true;
    }
}
