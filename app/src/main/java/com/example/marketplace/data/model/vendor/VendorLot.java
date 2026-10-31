package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** One lot from GET lots, as shown on a vendor's Inventory page. Any field can be null. */
public class VendorLot {

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
    @SerializedName("photos")
    private List<String> photos;
    @SerializedName("vendor")
    private Owner vendor;

    /** The seller, as the server sends it inside a lot: only its id is needed to group lots by seller. */
    private static class Owner {
        @SerializedName("_id")
        String id;
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getLotId() { return lotId; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getMaterial() { return material; }
    @Nullable public String getStatus() { return status; }
    @Nullable public String getLocation() { return location; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public String getUnit() { return unit; }
    @Nullable public Double getPurity() { return leadPurity; }
    @Nullable public Double getPricePerKg() { return pricePerKg; }
    @Nullable public String getCurrency() { return currency; }

    /** The id of the vendor who owns the lot. */
    @Nullable
    public String getVendorId() { return vendor == null ? null : vendor.id; }

    /** The first photo address, or null when the lot has no photo. */
    @Nullable
    public String getFirstPhoto() {
        if (photos == null) {
            return null;
        }
        for (String photo : photos) {
            if (photo != null && !photo.trim().isEmpty()) {
                return photo.trim();
            }
        }
        return null;
    }

    /** Held for a buyer or sold: shown with the amber "reserve held" badge instead of "available". */
    public boolean isReserved() {
        return status != null && status.trim().toLowerCase(Locale.ROOT).equals("reserved");
    }

    public boolean isSold() {
        return status != null && status.trim().toLowerCase(Locale.ROOT).equals("sold");
    }

    public boolean matches(@NonNull String needleLower) {
        return contains(title, needleLower) || contains(lotId, needleLower) || contains(material, needleLower)
                || contains(location, needleLower);
    }

    private static boolean contains(@Nullable String text, String needle) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(needle);
    }

    public boolean sameContentAs(VendorLot other) {
        return Objects.equals(id, other.id) && Objects.equals(status, other.status)
                && Objects.equals(quantity, other.quantity) && Objects.equals(leadPurity, other.leadPurity)
                && Objects.equals(title, other.title) && Objects.equals(getFirstPhoto(), other.getFirstPhoto());
    }
}
