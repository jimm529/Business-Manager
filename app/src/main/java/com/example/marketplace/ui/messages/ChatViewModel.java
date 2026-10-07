package com.example.marketplace.ui.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.messages.ChatMessage;
import com.example.marketplace.data.model.messages.MessagesResponse;
import com.example.marketplace.data.model.messages.SendMessageResponse;
import com.example.marketplace.data.repository.MessagesRepository;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Holds the messages of one conversation and sends new ones. Survives rotation. */
public class ChatViewModel extends ViewModel {

    private final MessagesRepository repository = MessagesRepository.getInstance();
    private final ResourceLoader<MessagesResponse> loader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<List<ChatMessage>>> messages = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<Event<ActionResult>> sendResults = new MutableLiveData<>();

    /** Messages this person sent that the last reload did not include yet. */
    private final List<ChatMessage> justSent = new ArrayList<>();
    @Nullable
    private String conversationId;

    public ChatViewModel() {
        messages.addSource(loader.asLiveData(), state -> {
            if (state.status == Resource.Status.SUCCESS) {
                justSent.clear();
            }
            publish();
        });
    }

    /** Starts loading the conversation. Calling it again with the same id (e.g. after rotation) does nothing. */
    public void init(@NonNull String id) {
        if (!id.equals(conversationId)) {
            conversationId = id;
            justSent.clear();
            refresh();
        }
    }

    public LiveData<Resource<List<ChatMessage>>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> isSending() {
        return sending;
    }

    /** One-time outcome of a send: an error to show, or success so the input can be cleared. */
    public LiveData<Event<ActionResult>> getSendResults() {
        return sendResults;
    }

    public void refresh() {
        if (conversationId != null) {
            loader.load(repository.getMessages(conversationId));
        }
    }

    public void send(@NonNull String text) {
        String clean = text.trim();
        if (conversationId == null || clean.isEmpty() || Boolean.TRUE.equals(sending.getValue())) {
            return;
        }
        sending.setValue(true);
        LiveData<Resource<SendMessageResponse>> call = repository.sendMessage(conversationId, clean);
        call.observeForever(new Observer<Resource<SendMessageResponse>>() {
            @Override
            public void onChanged(Resource<SendMessageResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                sending.setValue(false);
                if (result.status == Resource.Status.SUCCESS) {
                    ChatMessage sent = result.data == null ? null : result.data.getMessage();
                    if (sent != null) {
                        justSent.add(sent);
                        publish();
                    }
                    sendResults.setValue(new Event<>(ActionResult.success(null, R.string.chat_sent)));
                } else {
                    sendResults.setValue(new Event<>(ActionResult.failure(result)));
                }
            }
        });
    }

    private void publish() {
        Resource<MessagesResponse> state = loader.asLiveData().getValue();
        if (state == null) {
            return;
        }
        List<ChatMessage> all = null;
        if (state.data != null) {
            all = new ArrayList<>(state.data.getMessages());
            for (ChatMessage sent : justSent) {
                if (!containsId(all, sent.getId())) {
                    all.add(sent);
                }
            }
        } else if (!justSent.isEmpty() && state.status != Resource.Status.ERROR) {
            all = new ArrayList<>(justSent);
        }
        messages.setValue(state.withData(all));
    }

    private static boolean containsId(List<ChatMessage> list, @Nullable String id) {
        for (ChatMessage message : list) {
            if (Objects.equals(message.getId(), id)) {
                return true;
            }
        }
        return false;
    }
}
