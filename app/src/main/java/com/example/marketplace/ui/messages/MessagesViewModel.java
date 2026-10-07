package com.example.marketplace.ui.messages;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.messages.Conversation;
import com.example.marketplace.data.model.messages.ConversationsResponse;
import com.example.marketplace.data.repository.MessagesRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;
import com.example.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Holds the conversation list and the search text, so both survive rotation. */
public class MessagesViewModel extends ViewModel {

    private final MessagesRepository repository = MessagesRepository.getInstance();
    private final ResourceLoader<ConversationsResponse> loader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<List<Conversation>>> conversations = new MediatorLiveData<>();
    private final LiveData<Integer> totalUnread;
    private String query = "";

    public MessagesViewModel() {
        conversations.addSource(loader.asLiveData(), response -> publish());
        totalUnread = Transformations.map(loader.asLiveData(), response -> {
            int total = 0;
            if (response.data != null) {
                String role = SessionManager.getInstance().getRole();
                for (Conversation conversation : response.data.getConversations()) {
                    total += conversation.getUnreadCount(role);
                }
            }
            return total;
        });
        refresh();
    }

    /** The threads that match the search text. */
    public LiveData<Resource<List<Conversation>>> getConversations() {
        return conversations;
    }

    /** Unread messages over all threads: drives the dot on the bell. */
    public LiveData<Integer> getTotalUnread() {
        return totalUnread;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(query)) {
            query = clean;
            publish();
        }
    }

    public void refresh() {
        loader.load(repository.getConversations());
    }

    private void publish() {
        Resource<ConversationsResponse> state = loader.asLiveData().getValue();
        if (state == null) {
            return;
        }
        List<Conversation> filtered = null;
        if (state.data != null) {
            filtered = new ArrayList<>();
            String role = SessionManager.getInstance().getRole();
            String needle = query.toLowerCase(Locale.getDefault());
            for (Conversation conversation : state.data.getConversations()) {
                if (needle.isEmpty() || matches(conversation, role, needle)) {
                    filtered.add(conversation);
                }
            }
        }
        conversations.setValue(state.withData(filtered));
    }

    private static boolean matches(Conversation conversation, String role, String needle) {
        return contains(conversation.getParticipantName(role), needle)
                || contains(conversation.getBatchTitle(), needle)
                || contains(conversation.getLastMessage(), needle);
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(needle);
    }
}
