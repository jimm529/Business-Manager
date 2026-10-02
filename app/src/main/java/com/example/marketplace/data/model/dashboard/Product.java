package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * One marketplace listing (the backend calls it a "lot"), as shown on the Operator home feed.
 * Any field can be null.
 */
public class Product {
    @SerializedName("_id")
    private String id;
    @SerializedName("lotId")
    private String lotId;
    @SerializedName("title")
    private String title;
    @SerializedName("material")
    private String material;
    /** Quantity in the given unit (normally metric tons). */
    @SerializedName("quantity")
    private Double quantity;
    @SerializedName("unit")
    private String unit;
    @SerializedName("pricePerKg")
    private Double pricePerKg;
    @SerializedName("leadPurity")
    private Double leadPurity;
    @SerializedName("location")
    private String location;
    @SerializedName("status")
    private String status;
    @SerializedName("photos")
    private List<String> photos;
    @SerializedName("vendor")
    private Seller vendor;

    @Nullable public String getId() { return id; }
    @Nullable public String getLotId() { return lotId; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getMaterial() { return material; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public String getUnit() { return unit; }
    @Nullable public Double getPricePerKg() { return pricePerKg; }
    @Nullable public Double getLeadPurity() { return leadPurity; }
    @Nullable public String getLocation() { return location; }
    @Nullable public String getStatus() { return status; }
    @Nullable public Seller getVendor() { return vendor; }

    /** First photo URL, or null when the listing has none. */
    @Nullable
    public String getFirstPhoto() {
        return (photos == null || photos.isEmpty()) ? null : photos.get(0);
    }

    /** The vendor who owns the listing. */
    public static class Seller {
        @SerializedName("companyName")
        private String companyName;
        @SerializedName("vendorName")
        private String vendorName;

        /** Company name, falling back to the vendor name. */
        @Nullable
        public String getDisplayName() {
            if (companyName != null && !companyName.trim().isEmpty()) {
                return companyName;
            }
            return (vendorName == null || vendorName.trim().isEmpty()) ? null : vendorName;
        }
    }
}
