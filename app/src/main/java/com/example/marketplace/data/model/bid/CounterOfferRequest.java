package com.example.marketplace.data.model.bid;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** What the buyer sends to counter a deal: new rate per kg, new quantity in metric tons, and notes. */
public class CounterOfferRequest {

    @SerializedName("proposedRate")
    private final double proposedRate;
    @SerializedName("revisedQuantity")
    private final double revisedQuantity;
    @SerializedName("notes")
    private final String notes;
    /** Lets the server notice when the other side changed the deal in the meantime. */
    @SerializedName("expectedUpdatedAt")
    private final String expectedUpdatedAt;

    public CounterOfferRequest(double proposedRate, double revisedQuantity, @Nullable String notes,
                               @Nullable String expectedUpdatedAt) {
        this.proposedRate = proposedRate;
        this.revisedQuantity = revisedQuantity;
        this.notes = notes == null ? "" : notes;
        this.expectedUpdatedAt = expectedUpdatedAt;
    }
}
