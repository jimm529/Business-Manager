package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET purchase-history (buyers only): totals plus a page of past orders. Any field can be null. */
public class PurchaseHistoryResponse {

    @SerializedName("metrics")
    private Metrics metrics;
    @SerializedName("orders")
    private List<Row> orders;

    @Nullable public Metrics getMetrics() { return metrics; }

    /** Never null. */
    public List<Row> getOrders() { return orders == null ? new ArrayList<>() : orders; }

    public static class Metrics {
        @SerializedName("totalOrders")
        private Integer totalOrders;
        @SerializedName("totalSpend")
        private Double totalSpend;
        @SerializedName("materialsProcuredMT")
        private Double materialsProcuredMT;

        @Nullable public Integer getTotalOrders() { return totalOrders; }
        @Nullable public Double getTotalSpend() { return totalSpend; }
        @Nullable public Double getMaterialsProcuredMT() { return materialsProcuredMT; }
    }

    public static class Row {
        /** Order date as YYYY-MM-DD. */
        @SerializedName("date")
        private String date;
        @SerializedName("quantityMT")
        private Double quantityMT;
        @SerializedName("valueINR")
        private Double valueINR;
        /** Display label such as COMPLETED, PROCESSING or CANCELLED. */
        @SerializedName("status")
        private String status;

        @Nullable public String getDate() { return date; }
        @Nullable public Double getQuantityMT() { return quantityMT; }
        @Nullable public Double getValueINR() { return valueINR; }
        @Nullable public String getStatus() { return status; }
    }
}
