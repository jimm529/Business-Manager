package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Answer of GET vendor-portal/overview (the vendor dashboard). Only the parts the Home screen
 * shows are listed; the backend sends more (market chart, negotiations, ...) which is ignored.
 * Every field can be null.
 */
public class VendorOverviewResponse {

    @SerializedName("success")
    private Boolean success;
    @SerializedName("data")
    private Data data;

    @Nullable
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("vendor")
        private VendorInfo vendor;
        @SerializedName("stats")
        private Stats stats;

        @Nullable
        public VendorInfo getVendor() { return vendor; }

        @Nullable
        public Stats getStats() { return stats; }
    }

    public static class VendorInfo {
        @SerializedName("id")
        private String id;
        @SerializedName("companyName")
        private String companyName;
        @SerializedName("location")
        private String location;

        @Nullable
        public String getCompanyName() { return companyName; }

        @Nullable
        public String getLocation() { return location; }
    }

    public static class Stats {
        /** Total revenue in rupees. */
        @SerializedName("totalRevenueRaw")
        private Double totalRevenueRaw;
        @SerializedName("revenueGrowthPct")
        private Double revenueGrowthPct;
        /** Number of lots currently listed. */
        @SerializedName("activeBatchesCount")
        private Integer activeBatchesCount;
        @SerializedName("activeBatchesMT")
        private Double activeBatchesMT;

        @Nullable
        public Double getTotalRevenue() { return totalRevenueRaw; }

        @Nullable
        public Double getRevenueGrowthPct() { return revenueGrowthPct; }

        @Nullable
        public Integer getActiveListings() { return activeBatchesCount; }

        @Nullable
        public Double getActiveVolumeMt() { return activeBatchesMT; }
    }
}
