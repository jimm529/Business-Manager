package com.example.marketplace.data.model.order;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET orders. The backend sends every order of the signed-in user, newest first. */
public class OrdersResponse {
    @SerializedName("success")
    private Boolean success;
    @SerializedName("count")
    private Integer count;
    @SerializedName("orders")
    private List<Order> orders;

    /** Never null. */
    public List<Order> getOrders() {
        return orders == null ? new ArrayList<>() : orders;
    }
}
