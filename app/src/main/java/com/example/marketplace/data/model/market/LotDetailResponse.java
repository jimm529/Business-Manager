package com.example.marketplace.data.model.market;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

/** Answer of GET lots/{id}: one listing and a short summary of its seller. Any field can be null. */
public class LotDetailResponse {

    @SerializedName("lot")
    private Lot lot;
    @SerializedName("sellerSummary")
    private Seller sellerSummary;

    @Nullable
    public Lot getLot() { return lot; }

    @Nullable
    public Seller getSeller() { return sellerSummary; }

    public static class Lot {
        @SerializedName("_id")
        private String id;
        @SerializedName("lotId")
        private String lotId;
        @SerializedName("title")
        private String title;
        @SerializedName("material")
        private String material;
        @SerializedName("status")
        private String status;
        @SerializedName("location")
        private String location;
        @SerializedName("pickupLocation")
        private String pickupLocation;
        @SerializedName("quantity")
        private Double quantity;
        @SerializedName("unit")
        private String unit;
        @SerializedName("leadPurity")
        private Double leadPurity;
        @SerializedName("pricePerKg")
        private Double pricePerKg;
        @SerializedName("currency")
        private String currency;

        @Nullable public String getId() { return id; }
        @Nullable public String getLotId() { return lotId; }
        @Nullable public String getTitle() { return title; }
        @Nullable public String getMaterial() { return material; }
        @Nullable public String getStatus() { return status; }
        @Nullable public Double getQuantity() { return quantity; }
        @Nullable public String getUnit() { return unit; }
        @Nullable public Double getPurity() { return leadPurity; }
        @Nullable public Double getPricePerKg() { return pricePerKg; }
        @Nullable public String getCurrency() { return currency; }

        @Nullable
        public String getLocation() {
            return pickupLocation != null && !pickupLocation.trim().isEmpty() ? pickupLocation : location;
        }

        /** Sold or reserved lots cannot be ordered any more. */
        public boolean isClosed() {
            if (status == null) {
                return false;
            }
            String clean = status.trim().toLowerCase(Locale.ROOT);
            return clean.equals("sold") || clean.equals("reserved");
        }
    }

    public static class Seller {
        @SerializedName("sellerName")
        private String sellerName;
        @SerializedName("isVerified")
        private Boolean isVerified;

        @Nullable public String getName() { return sellerName; }
        public boolean isVerified() { return Boolean.TRUE.equals(isVerified); }
    }
}
