package com.example.marketplace.data.model.dashboard;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Answer of GET vendor-portal/order-approvals: the vendor's approval queue.
 * The Home screen only needs how many items are waiting, so the items are kept untyped.
 */
public class OrderApprovalsResponse {

    @SerializedName("success")
    private Boolean success;
    @SerializedName("data")
    private Data data;

    /** Order requests plus orders waiting for documents. Never null. */
    public int getPendingCount() {
        if (data == null) {
            return 0;
        }
        return size(data.pendingBids) + size(data.awaitingDocOrders);
    }

    private static int size(List<JsonElement> list) {
        return list == null ? 0 : list.size();
    }

    private static class Data {
        @SerializedName("pendingBids")
        List<JsonElement> pendingBids;
        @SerializedName("awaitingDocOrders")
        List<JsonElement> awaitingDocOrders;
    }
}
