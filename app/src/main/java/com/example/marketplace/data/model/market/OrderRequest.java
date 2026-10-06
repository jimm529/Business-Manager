package com.example.marketplace.data.model.market;

import com.google.gson.annotations.SerializedName;

/**
 * Body of POST lots/{id}/offers. Every field is left out when null, and the server then uses the
 * lot's own price and quantity, which is what "PLACE ORDER" on a card asks for.
 */
public class OrderRequest {

    @SerializedName("proposedRate")
    private Double proposedRate;
    @SerializedName("proposedQuantity")
    private Double proposedQuantity;
    @SerializedName("notes")
    private String notes;
}
