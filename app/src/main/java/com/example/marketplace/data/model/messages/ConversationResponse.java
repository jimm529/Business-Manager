package com.example.marketplace.data.model.messages;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of GET conversations/{id}: one thread, including the documents shared in it. */
public class ConversationResponse {

    @SerializedName("data")
    private Conversation data;

    @Nullable
    public Conversation getConversation() {
        return data;
    }
}
