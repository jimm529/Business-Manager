package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendors: {"success", "count", "data": [vendors]}. */
public class RegisteredVendorsResponse {

    @SerializedName("data")
    private List<RegisteredVendor> data;

    /** Never null. */
    @NonNull
    public List<RegisteredVendor> getVendors() {
        return data == null ? new ArrayList<>() : data;
    }
}
