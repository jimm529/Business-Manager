package com.example.marketplace.data.model.bid;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.Objects;

/** One file shared in a conversation (certificate, insurance, manifest...). */
public class NegotiationDocument {

    @SerializedName("_id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("url")
    private String url;
    @SerializedName("contentType")
    private String contentType;
    @SerializedName("size")
    private Long size;
    @SerializedName("uploadedByName")
    private String uploadedByName;
    @SerializedName("uploadedAt")
    private String uploadedAt;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return name;
    }

    @Nullable
    public String getDownloadUrl() {
        return url;
    }

    @Nullable
    public String getFileType() {
        return contentType;
    }

    @Nullable
    public Long getFileSize() {
        return size;
    }

    @Nullable
    public String getUploadedByName() {
        return uploadedByName;
    }

    @Nullable
    public String getDate() {
        return uploadedAt;
    }

    public boolean isPdf() {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf");
    }

    public boolean sameContentAs(NegotiationDocument other) {
        return Objects.equals(id, other.id) && Objects.equals(name, other.name)
                && Objects.equals(url, other.url) && Objects.equals(size, other.size)
                && Objects.equals(uploadedAt, other.uploadedAt);
    }
}
