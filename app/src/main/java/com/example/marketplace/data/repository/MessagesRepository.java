package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.bid.DocumentsResponse;
import com.example.marketplace.data.model.messages.ConversationResponse;
import com.example.marketplace.data.model.messages.ConversationsResponse;
import com.example.marketplace.data.model.messages.MessagesResponse;
import com.example.marketplace.data.model.messages.SendMessageRequest;
import com.example.marketplace.data.model.messages.SendMessageResponse;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.utils.Resource;

import okhttp3.MultipartBody;

/** Everything about conversations the server is asked: the thread list, a thread, its messages and shared documents. */
public class MessagesRepository {

    private static MessagesRepository instance;

    private MessagesRepository() { }

    public static synchronized MessagesRepository getInstance() {
        if (instance == null) {
            instance = new MessagesRepository();
        }
        return instance;
    }

    /** GET conversations. */
    public LiveData<Resource<ConversationsResponse>> getConversations() {
        return ApiResults.enqueue(ApiClient.getApiService().getConversations());
    }

    /** GET conversations/{id}. */
    public LiveData<Resource<ConversationResponse>> getConversation(@NonNull String conversationId) {
        return ApiResults.enqueue(ApiClient.getApiService().getConversation(conversationId));
    }

    /** GET conversations/{id}/messages. */
    public LiveData<Resource<MessagesResponse>> getMessages(@NonNull String conversationId) {
        return ApiResults.enqueue(ApiClient.getApiService().getMessages(conversationId));
    }

    /** POST conversations/{id}/messages. */
    public LiveData<Resource<SendMessageResponse>> sendMessage(@NonNull String conversationId, @NonNull String text) {
        return ApiResults.enqueue(ApiClient.getApiService().sendMessage(conversationId, new SendMessageRequest(text)));
    }

    /** PATCH conversations/{id}/read: tells the server these messages were seen. */
    public LiveData<Resource<MessageResponse>> markRead(@NonNull String conversationId) {
        return ApiResults.enqueue(ApiClient.getApiService().markConversationRead(conversationId));
    }

    /** POST conversations/{id}/documents (multipart, part name "file"). */
    public LiveData<Resource<DocumentsResponse>> uploadDocument(@NonNull String conversationId,
                                                                @NonNull MultipartBody.Part file) {
        return ApiResults.enqueue(ApiClient.getApiService().uploadDocument(conversationId, file));
    }
}
