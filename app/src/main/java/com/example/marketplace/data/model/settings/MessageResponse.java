package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** The plain {"success", "message"} answer of password change and 2FA verify. */
public class MessageResponse {

    @SerializedName("message")
    private String message;

    @Nullable
    public String getMessage() {
        return message;
    }
}
