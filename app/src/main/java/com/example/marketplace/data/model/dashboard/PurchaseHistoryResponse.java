package com.example.marketplace.data.model.dashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Answer of GET purchase-history (buyers only): totals, one page of past orders and the paging
 * numbers. The totals always describe the whole filtered list, not just this page. Any field can be null.
 */
public class PurchaseHistoryResponse {

    @SerializedName("metrics")
    private Metrics metrics;
    @SerializedName("orders")
    private List<Row> orders;
    @SerializedName("pagination")
    private Pagination pagination;

    @Nullable public Metrics getMetrics() { return metrics; }

    /** Never null. */
    public List<Row> getOrders() { return orders == null ? new ArrayList<>() : orders; }

    public int getCurrentPage() {
        return pagination == null || pagination.currentPage == null ? 1 : Math.max(1, pagination.currentPage);
    }

    public int getTotalPages() {
        return pagination == null || pagination.totalPages == null ? 1 : Math.max(1, pagination.totalPages);
    }

    public int getTotalOrders() {
        return pagination == null || pagination.totalOrders == null ? getOrders().size() : pagination.totalOrders;
    }

    public int getPageSize() {
        return pagination == null || pagination.pageSize == null ? Math.max(1, getOrders().size()) : pagination.pageSize;
    }

    private static class Pagination {
        @SerializedName("totalOrders")
        Integer totalOrders;
        @SerializedName("totalPages")
        Integer totalPages;
        @SerializedName("currentPage")
        Integer currentPage;
        @SerializedName("pageSize")
        Integer pageSize;
    }

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
        @SerializedName("orderId")
        private String orderId;
        /** Order date as YYYY-MM-DD. */
        @SerializedName("date")
        private String date;
        @SerializedName("materialType")
        private String materialType;
        @SerializedName("quantityMT")
        private Double quantityMT;
        @SerializedName("purity")
        private Double purity;
        @SerializedName("vendorName")
        private String vendorName;
        @SerializedName("valueINR")
        private Double valueINR;
        @SerializedName("currency")
        private String currency;
        /** Display label such as COMPLETED, ON THE WAY, PROCESSING or CANCELLED. */
        @SerializedName("status")
        private String status;

        @Nullable public String getOrderId() { return orderId; }
        @Nullable public String getDate() { return date; }
        @Nullable public String getMaterial() { return materialType; }
        @Nullable public Double getQuantityMT() { return quantityMT; }
        @Nullable public Double getPurity() { return purity; }
        @Nullable public String getVendorName() { return vendorName; }
        @Nullable public Double getValueINR() { return valueINR; }
        @Nullable public String getCurrency() { return currency; }
        @Nullable public String getStatus() { return status; }

        public boolean isCompleted() { return statusIs("COMPLETED"); }
        public boolean isOnTheWay() { return statusIs("ON THE WAY"); }
        public boolean isCancelled() { return statusIs("CANCELLED"); }

        private boolean statusIs(@NonNull String expected) {
            return status != null && status.trim().toUpperCase(Locale.ROOT).equals(expected);
        }

        public boolean sameContentAs(Row other) {
            return Objects.equals(orderId, other.orderId) && Objects.equals(date, other.date)
                    && Objects.equals(status, other.status) && Objects.equals(quantityMT, other.quantityMT)
                    && Objects.equals(valueINR, other.valueINR) && Objects.equals(vendorName, other.vendorName)
                    && Objects.equals(materialType, other.materialType) && Objects.equals(purity, other.purity);
        }
    }
}
