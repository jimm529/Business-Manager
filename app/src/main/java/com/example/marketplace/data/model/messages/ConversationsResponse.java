package com.example.marketplace.data.model.messages;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET conversations. */
public class ConversationsResponse {

    @SerializedName("data")
    private List<Conversation> data;

    @NonNull
    public List<Conversation> getConversations() {
        return data == null ? new ArrayList<>() : data;
    }
}
