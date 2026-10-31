package com.example.marketplace.data.model.listing;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/listings/locations: pickup places this vendor used before. */
public class LocationsResponse {

    @SerializedName("data")
    private List<String> data;

    @NonNull
    public List<String> getLocations() {
        return data == null ? new ArrayList<>() : data;
    }
}
