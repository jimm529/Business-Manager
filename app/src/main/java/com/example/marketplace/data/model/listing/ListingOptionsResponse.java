package com.example.marketplace.data.model.listing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/listings/options: the choices for the New Listing dropdowns. */
public class ListingOptionsResponse {

    @SerializedName("data")
    private Data data;

    /** One choice: the value the server stores, the text shown, and (for materials) a usual purity. */
    public static class Option {
        @SerializedName("value")
        private String value;
        @SerializedName("label")
        private String label;
        @SerializedName("defaultPurity")
        private Double defaultPurity;

        @Nullable
        public String getValue() { return value; }

        /** The text to show; falls back to the value. */
        @NonNull
        public String getLabel() {
            if (label != null && !label.trim().isEmpty()) {
                return label;
            }
            return value == null ? "" : value;
        }

        @Nullable
        public Double getDefaultPurity() { return defaultPurity; }
    }

    private static class Data {
        @SerializedName("materialTypes")
        List<Option> materialTypes;
        @SerializedName("packagingTypes")
        List<Option> packagingTypes;
        @SerializedName("incoterms")
        List<Option> incoterms;
        @SerializedName("regions")
        List<Option> regions;
    }

    @NonNull
    public List<Option> getMaterialTypes() {
        return data == null || data.materialTypes == null ? new ArrayList<>() : data.materialTypes;
    }

    @NonNull
    public List<Option> getPackagingTypes() {
        return data == null || data.packagingTypes == null ? new ArrayList<>() : data.packagingTypes;
    }

    @NonNull
    public List<Option> getIncoterms() {
        return data == null || data.incoterms == null ? new ArrayList<>() : data.incoterms;
    }

    @NonNull
    public List<Option> getRegions() {
        return data == null || data.regions == null ? new ArrayList<>() : data.regions;
    }
}
