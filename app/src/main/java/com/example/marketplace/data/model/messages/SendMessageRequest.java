package com.example.marketplace.data.model.messages;

import com.google.gson.annotations.SerializedName;

/** The text of a new chat message. */
public class SendMessageRequest {

    @SerializedName("content")
    private final String content;

    public SendMessageRequest(String content) {
        this.content = content;
    }
}
