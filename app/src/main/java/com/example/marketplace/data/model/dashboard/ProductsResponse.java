package com.example.marketplace.data.model.dashboard;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET lots: the listings plus the list of material categories used for the filter chips. */
public class ProductsResponse {
    @SerializedName("success")
    private Boolean success;
    @SerializedName("count")
    private Integer count;
    @SerializedName("lots")
    private List<Product> lots;
    @SerializedName("categories")
    private List<String> categories;

    /** Never null. */
    public List<Product> getProducts() {
        return lots == null ? new ArrayList<>() : lots;
    }

    /** Material names that exist in the marketplace. Never null. */
    public List<String> getCategories() {
        return categories == null ? new ArrayList<>() : categories;
    }
}
