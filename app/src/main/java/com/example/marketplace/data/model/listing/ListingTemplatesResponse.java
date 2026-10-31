package com.example.marketplace.data.model.listing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/listings/templates: the vendor's earlier listings, to copy into the form. */
public class ListingTemplatesResponse {

    @SerializedName("data")
    private List<Template> data;

    public static class Template {
        @SerializedName("_id")
        private String id;
        @SerializedName("materialType")
        private String materialType;
        @SerializedName("purityGrade")
        private Double purityGrade;
        @SerializedName("quantityMT")
        private Double quantityMT;
        @SerializedName("packagingType")
        private String packagingType;
        @SerializedName("pickupLocation")
        private String pickupLocation;
        @SerializedName("shippingTerms")
        private String shippingTerms;
        @SerializedName("askingPricePerKg")
        private Double askingPricePerKg;

        @Nullable
        public String getId() { return id; }

        @Nullable
        public String getMaterialType() { return materialType; }

        @Nullable
        public Double getPurityGrade() { return purityGrade; }

        @Nullable
        public Double getQuantityMT() { return quantityMT; }

        @Nullable
        public String getPackagingType() { return packagingType; }

        @Nullable
        public String getPickupLocation() { return pickupLocation; }

        @Nullable
        public String getShippingTerms() { return shippingTerms; }

        @Nullable
        public Double getAskingPricePerKg() { return askingPricePerKg; }
    }

    @NonNull
    public List<Template> getTemplates() {
        return data == null ? new ArrayList<>() : data;
    }
}
