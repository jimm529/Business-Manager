package com.example.marketplace.data.model.notifications;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/** One notification: a new message, a deal waiting for you, or an order that changed. */
public class NotificationItem {

    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_DEAL = "deal";
    public static final String TYPE_LOGISTICS = "logistics";

    /** Stable id the server uses to remember what was read. */
    @SerializedName("key")
    private String key;
    /** message, deal, order or logistics. */
    @SerializedName("type")
    private String type;
    @SerializedName("title")
    private String title;
    @SerializedName("body")
    private String body;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("link")
    private String link;
    @SerializedName("read")
    private Boolean read;

    /** Set on the phone the moment the person opens it, before the server has confirmed. */
    private transient boolean locallyRead;

    @Nullable public String getKey() { return key; }
    @Nullable public String getType() { return type; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getBody() { return body; }
    @Nullable public String getCreatedAt() { return createdAt; }

    public boolean isRead() { return Boolean.TRUE.equals(read) || locallyRead; }

    public void setLocallyRead(boolean value) { locallyRead = value; }

    /** For a message notification: the conversation to open, taken from the server's link. */
    @Nullable
    public String getConversationId() {
        if (link == null || !link.contains("?")) {
            return null;
        }
        return Uri.parse("app://x" + link).getQueryParameter("conversationId");
    }

    public boolean sameContentAs(NotificationItem other) {
        return Objects.equals(key, other.key) && isRead() == other.isRead()
                && Objects.equals(title, other.title) && Objects.equals(body, other.body)
                && Objects.equals(createdAt, other.createdAt);
    }
}
