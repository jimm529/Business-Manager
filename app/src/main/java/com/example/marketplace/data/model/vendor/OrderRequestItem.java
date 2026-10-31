package com.example.marketplace.data.model.vendor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.Objects;

/** One row of the vendor's Order Requests Ledger: a buyer's bid or order on one of the vendor's lots. */
public class OrderRequestItem {

    /** The server sends sample rows with ids like this when a vendor has no requests; they are not real. */
    private static final String SAMPLE_ID_PREFIX = "req-sample";

    @SerializedName("id")
    private String id;
    @SerializedName("orderId")
    private String orderId;
    @SerializedName("buyerName")
    private String buyerName;
    @SerializedName("material")
    private String material;
    @SerializedName("quantityMT")
    private Double quantityMT;
    @SerializedName("offeredRate")
    private Double offeredRate;
    @SerializedName("status")
    private String status;
    @SerializedName("createdAt")
    private String createdAt;

    @Nullable
    public String getId() { return id; }

    @Nullable
    public String getOrderId() { return orderId; }

    @Nullable
    public String getBuyerName() { return buyerName; }

    @Nullable
    public String getMaterial() { return material; }

    @Nullable
    public Double getQuantityMT() { return quantityMT; }

    /** The offered price per kilogram, in rupees. */
    @Nullable
    public Double getOfferedRate() { return offeredRate; }

    @Nullable
    public String getCreatedAt() { return createdAt; }

    /** Waiting for the vendor to accept or reject. */
    public boolean isPending() { return statusIs("PENDING REVIEW"); }

    /** The vendor answered with a new price and the buyer has to respond. */
    public boolean isCounter() { return statusIs("COUNTER OFFER"); }

    public boolean isAccepted() { return statusIs("ACCEPTED"); }

    /** Still open (pending or countered), as opposed to accepted or rejected. */
    public boolean isOpen() { return isPending() || isCounter(); }

    /** True for the made-up rows the server invents for an empty ledger. */
    public boolean isSample() { return id != null && id.startsWith(SAMPLE_ID_PREFIX); }

    private boolean statusIs(String expected) {
        return status != null && status.trim().toUpperCase(Locale.ROOT).equals(expected);
    }

    public boolean sameContentAs(OrderRequestItem other) {
        return Objects.equals(id, other.id) && Objects.equals(status, other.status)
                && Objects.equals(quantityMT, other.quantityMT) && Objects.equals(offeredRate, other.offeredRate)
                && Objects.equals(buyerName, other.buyerName) && Objects.equals(material, other.material);
    }
}
