package com.example.marketplace.data.model.market;

import com.google.gson.annotations.SerializedName;

/** Answer of POST lots/{id}/offers: the server's confirmation text. */
public class OrderResponse {

    @SerializedName("message")
    private String message;

    /** May be null. */
    public String getMessage() { return message; }
}
