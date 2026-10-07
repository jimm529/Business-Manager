package com.example.marketplace.data.model.bid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of uploading a document: the thread's whole list of shared documents. */
public class DocumentsResponse {

    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private List<NegotiationDocument> data;

    @Nullable
    public String getMessage() {
        return message;
    }

    @NonNull
    public List<NegotiationDocument> getDocuments() {
        return data == null ? new ArrayList<>() : data;
    }
}
