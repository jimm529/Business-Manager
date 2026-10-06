package com.example.marketplace.data.model.market;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET lots: the listings, how many there are, and the choices for the filter sheet. */
public class MarketplaceResponse {

    @SerializedName("count")
    private Integer count;
    @SerializedName("lots")
    private List<MarketProduct> lots;
    @SerializedName("filters")
    private Filters filters;

    /** Never null. */
    @NonNull
    public List<MarketProduct> getProducts() {
        return lots == null ? new ArrayList<>() : lots;
    }

    public int getTotalCount() {
        return count == null ? getProducts().size() : count;
    }

    /** Material names that exist in the marketplace. Never null. */
    @NonNull
    public List<String> getMaterials() {
        return filters == null || filters.materials == null ? new ArrayList<>() : filters.materials;
    }

    /** Location (region) choices. Never null. */
    @NonNull
    public List<Region> getRegions() {
        return filters == null || filters.regions == null ? new ArrayList<>() : filters.regions;
    }

    /** One location choice: the value sent to the server and the name shown to people. */
    public static class Region {
        @SerializedName("value")
        private String value;
        @SerializedName("label")
        private String label;

        public String getValue() { return value; }

        /** The label, or the value when there is no label. */
        public String getLabel() { return label == null || label.trim().isEmpty() ? value : label; }
    }

    private static class Filters {
        @SerializedName("materials")
        List<String> materials;
        @SerializedName("regions")
        List<Region> regions;
    }
}
