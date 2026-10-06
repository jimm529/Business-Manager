package com.example.marketplace.data.model.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * One marketplace listing (the backend calls it a "lot") as shown on a Market card.
 * Read from GET lots; any field can be null. Never changed after it is created:
 * {@link #withBookmarked(boolean)} returns a changed copy.
 */
public class MarketProduct {

    @SerializedName("_id")
    private String id;
    @SerializedName("lotId")
    private String lotId;
    @SerializedName("title")
    private String title;
    @SerializedName("material")
    private String material;
    /** Lot state: Available, Ready for Pickup, In Extraction, Reserved or Sold. */
    @SerializedName("status")
    private String status;
    @SerializedName("location")
    private String location;
    @SerializedName("quantity")
    private Double quantity;
    /** Normally "MT" (metric tons). */
    @SerializedName("unit")
    private String unit;
    @SerializedName("leadPurity")
    private Double leadPurity;
    /** Asking price per kg. */
    @SerializedName("pricePerKg")
    private Double pricePerKg;
    @SerializedName("currency")
    private String currency;
    @SerializedName("isBookmarked")
    private Boolean isBookmarked;
    /** This buyer's own newest bid on the lot, or null when there is none. */
    @SerializedName("myLatestBid")
    private LatestBid myLatestBid;
    @SerializedName("vendor")
    private Seller vendor;

    /** Needed by Gson. */
    public MarketProduct() { }

    private MarketProduct(MarketProduct other, boolean bookmarked) {
        this.id = other.id;
        this.lotId = other.lotId;
        this.title = other.title;
        this.material = other.material;
        this.status = other.status;
        this.location = other.location;
        this.quantity = other.quantity;
        this.unit = other.unit;
        this.leadPurity = other.leadPurity;
        this.pricePerKg = other.pricePerKg;
        this.currency = other.currency;
        this.isBookmarked = bookmarked;
        this.myLatestBid = other.myLatestBid;
        this.vendor = other.vendor;
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getLotId() { return lotId; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getMaterial() { return material; }
    @Nullable public String getStatus() { return status; }
    @Nullable public String getLocation() { return location; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public String getUnit() { return unit; }
    @Nullable public String getCurrency() { return currency; }
    public boolean isBookmarked() { return Boolean.TRUE.equals(isBookmarked); }

    /** Purity of the lead in percent, e.g. 97.8. */
    @Nullable
    public Double getPurityGrade() { return leadPurity; }

    /** The buyer's own latest bid per kg when there is one, otherwise the asking price per kg. */
    @Nullable
    public Double getCurrentBidPrice() {
        if (myLatestBid != null && myLatestBid.unitPricePerKg != null) {
            return myLatestBid.unitPricePerKg;
        }
        return pricePerKg;
    }

    /** The seller counts as verified when it lists at least one compliance certification. */
    public boolean isVerified() {
        List<String> certifications = vendor == null ? null : vendor.complianceCertifications;
        if (certifications == null) {
            return false;
        }
        for (String certification : certifications) {
            if (certification != null && !certification.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Quantity converted to kilograms (metric tons and kg are understood); null when unknown. */
    @Nullable
    public Double getQuantityKg() {
        if (quantity == null) {
            return null;
        }
        String clean = unit == null ? "mt" : unit.trim().toLowerCase(Locale.ROOT);
        if (clean.equals("kg") || clean.equals("kgs")) {
            return quantity;
        }
        return quantity * 1000d;
    }

    /** A copy of this listing with the saved state changed. */
    @NonNull
    public MarketProduct withBookmarked(boolean bookmarked) {
        return new MarketProduct(this, bookmarked);
    }

    public boolean sameContentAs(@NonNull MarketProduct other) {
        return Objects.equals(title, other.title)
                && Objects.equals(status, other.status)
                && Objects.equals(location, other.location)
                && Objects.equals(quantity, other.quantity)
                && Objects.equals(unit, other.unit)
                && Objects.equals(leadPurity, other.leadPurity)
                && Objects.equals(getCurrentBidPrice(), other.getCurrentBidPrice())
                && isBookmarked() == other.isBookmarked();
    }

    private static class LatestBid {
        @SerializedName("unitPricePerKg")
        Double unitPricePerKg;
    }

    private static class Seller {
        @SerializedName("complianceCertifications")
        List<String> complianceCertifications;
    }
}
