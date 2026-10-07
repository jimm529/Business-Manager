package com.example.marketplace.data.model.bid;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of accepting or countering a deal: a message, the updated deal and (on accept) the transaction summary. */
public class BidActionResponse {

    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private NegotiationDetail data;
    @SerializedName("transaction")
    private TransactionConfirmation transaction;

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public NegotiationDetail getBid() {
        return data;
    }

    @Nullable
    public TransactionConfirmation getTransaction() {
        return transaction;
    }
}
