package com.example.marketplace.data.model.messages;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET conversations/{id}/messages, oldest first. */
public class MessagesResponse {

    @SerializedName("data")
    private List<ChatMessage> data;

    @NonNull
    public List<ChatMessage> getMessages() {
        return data == null ? new ArrayList<>() : data;
    }
}
