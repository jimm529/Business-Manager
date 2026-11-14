package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.notifications.NotificationsResponse;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.utils.Resource;
import com.google.gson.JsonObject;

/** The notification list, and marking one or all of them as read. */
public class NotificationsRepository {

    private static NotificationsRepository instance;

    private NotificationsRepository() { }

    public static synchronized NotificationsRepository getInstance() {
        if (instance == null) {
            instance = new NotificationsRepository();
        }
        return instance;
    }

    /** GET notifications. */
    public LiveData<Resource<NotificationsResponse>> getNotifications() {
        return ApiResults.enqueue(ApiClient.getApiService().getNotifications());
    }

    /** PATCH notifications/read with the notification's key. */
    public LiveData<Resource<MessageResponse>> markRead(@NonNull String key) {
        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        return ApiResults.enqueue(ApiClient.getApiService().markNotificationRead(body));
    }

    /** PATCH notifications/read-all. */
    public LiveData<Resource<MessageResponse>> markAllRead() {
        return ApiResults.enqueue(ApiClient.getApiService().markAllNotificationsRead());
    }
}
