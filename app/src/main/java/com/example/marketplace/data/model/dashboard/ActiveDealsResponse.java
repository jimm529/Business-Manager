package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET bids/active-deals (buyers only): every open negotiation, newest activity first. */
public class ActiveDealsResponse {

    @SerializedName("data")
    private List<Deal> data;

    /** Never null. */
    public List<Deal> getDeals() { return data == null ? new ArrayList<>() : data; }

    public static class Deal {
        @SerializedName("id")
        private String id;
        @SerializedName("_id")
        private String mongoId;
        @SerializedName("lotId")
        private String lotId;
        @SerializedName("batchTitle")
        private String batchTitle;
        @SerializedName("material")
        private String material;
        @SerializedName("quantityMT")
        private Double quantityMT;
        /** Price per metric ton, already converted by the server (rupees). */
        @SerializedName("pricePerMT")
        private Double pricePerMT;
        @SerializedName("status")
        private String status;
        /** "operator" (the buyer) or "vendor". */
        @SerializedName("currentTurn")
        private String currentTurn;
        @SerializedName("lastActionBy")
        private String lastActionBy;

        @Nullable public String getId() { return id != null ? id : mongoId; }
        @Nullable public String getLotId() { return lotId; }
        @Nullable public String getBatchTitle() { return batchTitle; }
        @Nullable public String getMaterial() { return material; }
        @Nullable public Double getQuantityMT() { return quantityMT; }
        @Nullable public Double getPricePerMT() { return pricePerMT; }
        @Nullable public String getStatus() { return status; }
        @Nullable public String getCurrentTurn() { return currentTurn; }
        @Nullable public String getLastActionBy() { return lastActionBy; }
    }
}
