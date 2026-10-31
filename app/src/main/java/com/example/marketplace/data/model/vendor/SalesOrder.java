package com.example.marketplace.data.model.vendor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.Objects;

/** One row of the vendor's Sales History: an order with its buyer, value, purity and status. */
public class SalesOrder {

    @SerializedName("_id")
    private String id;
    @SerializedName("orderId")
    private String orderId;
    /** When the order was created (ISO time). */
    @SerializedName("date")
    private String date;
    @SerializedName("materialType")
    private String materialType;
    @SerializedName("quantity")
    private Double quantity;
    @SerializedName("unit")
    private String unit;
    @SerializedName("purity")
    private Double purity;
    @SerializedName("buyer")
    private String buyer;
    @SerializedName("value")
    private Double value;
    @SerializedName("currency")
    private String currency;
    /** Completed, In-Transit, Dispatched, Cancelled or Awaiting Docs. */
    @SerializedName("status")
    private String status;
    @SerializedName("hasDocument")
    private Boolean hasDocument;

    @Nullable public String getId() { return id; }
    @Nullable public String getOrderId() { return orderId; }
    @Nullable public String getDate() { return date; }
    @Nullable public String getMaterial() { return materialType; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public String getUnit() { return unit; }
    @Nullable public Double getPurity() { return purity; }
    @Nullable public String getBuyer() { return buyer; }
    @Nullable public Double getValue() { return value; }
    @Nullable public String getCurrency() { return currency; }
    @Nullable public String getStatus() { return status; }

    public boolean hasDocument() { return Boolean.TRUE.equals(hasDocument); }

    public boolean isCompleted() { return statusIs("completed"); }

    public boolean isInTransit() { return statusIs("in-transit"); }

    public boolean isCancelled() { return statusIs("cancelled"); }

    private boolean statusIs(String expected) {
        return status != null && status.trim().toLowerCase(Locale.ROOT).equals(expected);
    }

    public boolean sameContentAs(SalesOrder other) {
        return Objects.equals(id, other.id) && Objects.equals(status, other.status)
                && Objects.equals(value, other.value) && Objects.equals(purity, other.purity)
                && Objects.equals(quantity, other.quantity) && Objects.equals(buyer, other.buyer)
                && hasDocument() == other.hasDocument();
    }
}
