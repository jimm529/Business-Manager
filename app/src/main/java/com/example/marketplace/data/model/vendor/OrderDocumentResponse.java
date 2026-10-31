package com.example.marketplace.data.model.vendor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of GET vendor-portal/orders/{id}/document: the address of the order's document. */
public class OrderDocumentResponse {

    @SerializedName("documentUrl")
    private String documentUrl;
    @SerializedName("downloadUrl")
    private String downloadUrl;

    @Nullable
    public String getDocumentUrl() { return documentUrl; }

    @Nullable
    public String getDownloadUrl() { return downloadUrl != null ? downloadUrl : documentUrl; }
}
