package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendors (buyers only): the suppliers this buyer works with. */
public class VendorsResponse {

    @SerializedName("data")
    private List<Vendor> data;

    /** Never null. */
    public List<Vendor> getVendors() { return data == null ? new ArrayList<>() : data; }

    public static class Vendor {
        @SerializedName("_id")
        private String id;
        @SerializedName("companyName")
        private String companyName;
        @SerializedName("vendorName")
        private String vendorName;
        @SerializedName("brandLogoUrl")
        private String brandLogoUrl;
        @SerializedName("facilityImages")
        private List<String> facilityImages;

        @Nullable public String getId() { return id; }

        @Nullable
        public String getDisplayName() {
            if (companyName != null && !companyName.trim().isEmpty()) {
                return companyName.trim();
            }
            return (vendorName == null || vendorName.trim().isEmpty()) ? null : vendorName.trim();
        }

        /** The brand logo, else the first facility photo, else null. */
        @Nullable
        public String getImageUrl() {
            if (brandLogoUrl != null && !brandLogoUrl.trim().isEmpty()) {
                return brandLogoUrl.trim();
            }
            if (facilityImages != null) {
                for (String url : facilityImages) {
                    if (url != null && !url.trim().isEmpty()) {
                        return url.trim();
                    }
                }
            }
            return null;
        }
    }
}
