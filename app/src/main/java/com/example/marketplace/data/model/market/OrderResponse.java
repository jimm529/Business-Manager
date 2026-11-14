package com.example.marketplace.data.model.market;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of POST lots/{id}/offers: the server's confirmation text and the conversation with the seller. */
public class OrderResponse {

    @SerializedName("message")
    private String message;
    @SerializedName("conversationId")
    private String conversationId;

    /** May be null. */
    @Nullable
    public String getMessage() { return message; }

    /** The conversation with the seller where the request now waits; may be null. */
    @Nullable
    public String getConversationId() { return conversationId; }
}
