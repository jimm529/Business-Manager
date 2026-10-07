package com.example.marketplace.data.model.messages;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of POST conversations/{id}/messages: the message as the server saved it. */
public class SendMessageResponse {

    @SerializedName("data")
    private ChatMessage data;

    @Nullable
    public ChatMessage getMessage() {
        return data;
    }
}
