package com.example.marketplace.data.model.vendor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of accepting or rejecting a request: just the server's message. */
public class RequestActionResponse {

    @SerializedName("message")
    private String message;

    @Nullable
    public String getMessage() { return message; }
}
