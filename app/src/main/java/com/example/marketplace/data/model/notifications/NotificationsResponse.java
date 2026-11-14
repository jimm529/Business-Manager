package com.example.marketplace.data.model.notifications;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET notifications: the newest notifications of the signed-in person. */
public class NotificationsResponse {

    @SerializedName("notifications")
    private List<NotificationItem> notifications;

    @NonNull
    public List<NotificationItem> getNotifications() {
        return notifications == null ? new ArrayList<>() : notifications;
    }
}
