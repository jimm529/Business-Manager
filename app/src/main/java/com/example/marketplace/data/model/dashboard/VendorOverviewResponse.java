package com.example.marketplace.data.model.dashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Answer of GET vendor-portal/overview (the vendor dashboard): the four stat cards, the monthly
 * revenue chart, open negotiations, the logistics pipeline and the inventory table.
 * Every field can be null and every list can be missing.
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
        @SerializedName("marketPerformance")
        private List<ChartBar> marketPerformance;
        @SerializedName("activeNegotiations")
        private List<Negotiation> activeNegotiations;
        @SerializedName("logisticsPipeline")
        private List<PipelineStage> logisticsPipeline;
        @SerializedName("inventoryStatus")
        private List<InventoryItem> inventoryStatus;

        @Nullable
        public VendorInfo getVendor() { return vendor; }

        @Nullable
        public Stats getStats() { return stats; }

        @NonNull
        public List<ChartBar> getMarketPerformance() {
            return marketPerformance == null ? new ArrayList<>() : marketPerformance;
        }

        @NonNull
        public List<Negotiation> getActiveNegotiations() {
            return activeNegotiations == null ? new ArrayList<>() : activeNegotiations;
        }

        @NonNull
        public List<PipelineStage> getLogisticsPipeline() {
            return logisticsPipeline == null ? new ArrayList<>() : logisticsPipeline;
        }

        @NonNull
        public List<InventoryItem> getInventoryStatus() {
            return inventoryStatus == null ? new ArrayList<>() : inventoryStatus;
        }
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
        @SerializedName("extractionEfficiencyValue")
        private Double extractionEfficiencyValue;
        @SerializedName("complianceHealth")
        private ComplianceHealth complianceHealth;

        @Nullable
        public Double getTotalRevenue() { return totalRevenueRaw; }

        @Nullable
        public Double getRevenueGrowthPct() { return revenueGrowthPct; }

        @Nullable
        public Integer getActiveListings() { return activeBatchesCount; }

        @Nullable
        public Double getActiveVolumeMt() { return activeBatchesMT; }

        /** Average purity of the active lots, in percent. */
        @Nullable
        public Double getExtractionEfficiency() { return extractionEfficiencyValue; }

        @Nullable
        public ComplianceHealth getComplianceHealth() { return complianceHealth; }
    }

    public static class ComplianceHealth {
        /** "compliant", "pending" or something else (expired). */
        @SerializedName("status")
        private String status;
        @SerializedName("nextAudit")
        private String nextAudit;

        @Nullable
        public String getStatus() { return status; }

        @Nullable
        public String getNextAudit() { return nextAudit; }
    }

    /** One month of the revenue chart. */
    public static class ChartBar {
        @SerializedName("monthName")
        private String monthName;
        @SerializedName("value")
        private Double value;
        @SerializedName("isCurrent")
        private Boolean isCurrent;

        @Nullable
        public String getMonthName() { return monthName; }

        public double getValue() { return value == null ? 0 : value; }

        public boolean isCurrent() { return Boolean.TRUE.equals(isCurrent); }
    }

    /** An open deal on one of the vendor's lots. */
    public static class Negotiation {
        @SerializedName("id")
        private String id;
        @SerializedName("conversationId")
        private String conversationId;
        @SerializedName("batchId")
        private String batchId;
        @SerializedName("batchTitle")
        private String batchTitle;
        @SerializedName("status")
        private String status;
        @SerializedName("priceDisplay")
        private String priceDisplay;

        @Nullable
        public String getId() { return id; }

        @Nullable
        public String getConversationId() { return conversationId; }

        @Nullable
        public String getBatchId() { return batchId; }

        @Nullable
        public String getBatchTitle() { return batchTitle; }

        public boolean isCountered() { return "countered".equalsIgnoreCase(status); }

        @Nullable
        public String getPriceDisplay() { return priceDisplay; }
    }

    /** One step of the logistics timeline. */
    public static class PipelineStage {
        @SerializedName("type")
        private String type;
        @SerializedName("title")
        private String title;
        @SerializedName("subtitle")
        private String subtitle;

        @Nullable
        public String getType() { return type; }

        @Nullable
        public String getTitle() { return title; }

        @Nullable
        public String getSubtitle() { return subtitle; }
    }

    /** One row of the inventory table. */
    public static class InventoryItem {
        @SerializedName("displayId")
        private String displayId;
        @SerializedName("material")
        private String material;
        @SerializedName("purityValue")
        private Double purityValue;
        @SerializedName("purity")
        private String purity;
        @SerializedName("valuationRaw")
        private Double valuationRaw;

        @Nullable
        public String getDisplayId() { return displayId; }

        @Nullable
        public String getMaterial() { return material; }

        /** Purity in percent, 0 when unknown. */
        public double getPurityValue() { return purityValue == null ? 0 : purityValue; }

        @Nullable
        public String getPurityText() { return purity; }

        @Nullable
        public Double getValue() { return valuationRaw; }
    }
}
