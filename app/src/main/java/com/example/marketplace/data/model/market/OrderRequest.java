package com.example.marketplace.data.model.market;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Body of POST lots/{id}/offers. Every field is left out when null, and the server then uses the
 * lot's own price and quantity. The Place Order page fills in the buyer's own quantity, price and notes.
 */
public class OrderRequest {

    @SerializedName("proposedRate")
    private Double proposedRate;
    @SerializedName("proposedQuantity")
    private Double proposedQuantity;
    @SerializedName("notes")
    private String notes;

    /** An order at the lot's own price and quantity. */
    public OrderRequest() { }

    /** An order with the buyer's own price per kg, quantity in metric tons, and optional notes. */
    public OrderRequest(double proposedRate, double proposedQuantity, @Nullable String notes) {
        this.proposedRate = proposedRate;
        this.proposedQuantity = proposedQuantity;
        this.notes = notes == null || notes.trim().isEmpty() ? null : notes.trim();
    }
}
