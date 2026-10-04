package com.example.marketplace.data.model.order;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** One order, as returned by GET orders. Any field can be null. */
public class Order {
    @SerializedName("_id")
    private String id;
    /** Human order number, e.g. "#ORD-1234". */
    @SerializedName("orderId")
    private String orderId;
    @SerializedName("commodity")
    private String commodity;
    @SerializedName("quantity")
    private Double quantity;
    @SerializedName("unit")
    private String unit;
    /** Order value (in the given currency). */
    @SerializedName("value")
    private Double value;
    @SerializedName("currency")
    private String currency;
    /** Processing, Awaiting Docs, On the Way, Delivered, ... */
    @SerializedName("status")
    private String status;
    @SerializedName("buyerName")
    private String buyerName;
    @SerializedName("operator")
    private Person operator;
    /** Expected delivery, an ISO-8601 date-time string such as "2026-10-02T10:49:00.000Z". */
    @SerializedName("expectedDate")
    private String expectedDate;
    /** When the order was placed (same format). */
    @SerializedName("createdAt")
    private String createdAt;

    @Nullable public String getId() { return id; }
    @Nullable public String getOrderId() { return orderId; }
    @Nullable public String getCommodity() { return commodity; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public String getUnit() { return unit; }
    @Nullable public Double getValue() { return value; }
    @Nullable public String getCurrency() { return currency; }
    @Nullable public String getStatus() { return status; }
    @Nullable public String getExpectedDate() { return expectedDate; }
    @Nullable public String getCreatedAt() { return createdAt; }

    /** Name of the buyer: the operator's name, or the buyer name stored on the order. */
    @Nullable
    public String getBuyerDisplayName() {
        if (operator != null && operator.fullName != null && !operator.fullName.trim().isEmpty()) {
            return operator.fullName;
        }
        return (buyerName == null || buyerName.trim().isEmpty()) ? null : buyerName;
    }

    private static class Person {
        @SerializedName("fullName")
        String fullName;
    }
}
