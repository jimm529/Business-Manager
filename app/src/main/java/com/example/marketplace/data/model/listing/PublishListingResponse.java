package com.example.marketplace.data.model.listing;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of publishing: the server's message (it names the new lot). */
public class PublishListingResponse {

    @SerializedName("message")
    private String message;

    @Nullable
    public String getMessage() { return message; }
}
