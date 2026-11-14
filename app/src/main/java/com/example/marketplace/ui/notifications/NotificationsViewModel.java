package com.example.marketplace.ui.notifications;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.notifications.NotificationItem;
import com.example.marketplace.data.model.notifications.NotificationsResponse;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.repository.NotificationsRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the notification list. Opening a notification (or "Mark all read") shows it as read
 * straight away and tells the server in the background; if that fails the list is reloaded.
 */
public class NotificationsViewModel extends ViewModel {

    private final NotificationsRepository repository = NotificationsRepository.getInstance();
    private final ResourceLoader<NotificationsResponse> loader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<List<NotificationItem>>> notifications = new MediatorLiveData<>();
    private final MutableLiveData<Event<ActionResult>> errors = new MutableLiveData<>();

    /** Keys marked read on the phone; the server's list may not know them yet. */
    private final Set<String> locallyRead = new HashSet<>();

    public NotificationsViewModel() {
        notifications.addSource(loader.asLiveData(), state -> publish());
        refresh();
    }

    public LiveData<Resource<List<NotificationItem>>> getNotifications() {
        return notifications;
    }

    public LiveData<Event<ActionResult>> getErrors() {
        return errors;
    }

    public void refresh() {
        loader.load(repository.getNotifications());
    }

    /** How many notifications are still unread. */
    public static int unreadCount(@NonNull List<NotificationItem> items) {
        int count = 0;
        for (NotificationItem item : items) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public void markRead(@NonNull NotificationItem item) {
        String key = item.getKey();
        if (key == null || item.isRead()) {
            return;
        }
        locallyRead.add(key);
        publish();
        backgroundCall(repository.markRead(key));
    }

    public void markAllRead() {
        Resource<List<NotificationItem>> state = notifications.getValue();
        if (state == null || state.data == null || unreadCount(state.data) == 0) {
            return;
        }
        for (NotificationItem item : state.data) {
            if (item.getKey() != null) {
                locallyRead.add(item.getKey());
            }
        }
        publish();
        backgroundCall(repository.markAllRead());
    }

    /** Sends the "read" to the server without waiting; a failure shows a message and reloads the truth. */
    private void backgroundCall(LiveData<Resource<MessageResponse>> call) {
        call.observeForever(new Observer<Resource<MessageResponse>>() {
            @Override
            public void onChanged(Resource<MessageResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                if (result.status == Resource.Status.ERROR) {
                    locallyRead.clear();
                    errors.setValue(new Event<>(ActionResult.failure(result)));
                    refresh();
                }
            }
        });
    }

    private void publish() {
        Resource<NotificationsResponse> state = loader.asLiveData().getValue();
        if (state == null) {
            return;
        }
        List<NotificationItem> list = null;
        if (state.data != null) {
            list = new ArrayList<>(state.data.getNotifications());
            for (NotificationItem item : list) {
                item.setLocallyRead(item.getKey() != null && locallyRead.contains(item.getKey()));
            }
        }
        notifications.setValue(state.withData(list));
    }
}
