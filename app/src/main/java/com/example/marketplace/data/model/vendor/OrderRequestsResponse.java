package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/orders: the vendor's order requests, sample rows removed. */
public class OrderRequestsResponse {

    @SerializedName("requests")
    private List<OrderRequestItem> requests;

    /** The real requests only. */
    @NonNull
    public List<OrderRequestItem> getRequests() {
        List<OrderRequestItem> real = new ArrayList<>();
        if (requests != null) {
            for (OrderRequestItem item : requests) {
                if (item != null && !item.isSample()) {
                    real.add(item);
                }
            }
        }
        return real;
    }
}
