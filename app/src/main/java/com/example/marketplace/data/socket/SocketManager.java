package com.example.marketplace.data.socket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.data.model.messages.ChatMessage;
import com.example.marketplace.utils.SessionManager;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * The live connection to the server (Socket.io). While a person is signed in it stays connected, so
 * new messages and list changes arrive at once, in both directions, without pulling to refresh.
 *
 * Screens listen for what they care about: a chat listens for new messages of its conversation, the
 * Messages list listens for "a conversation changed". Everything is delivered on the main thread.
 */
public final class SocketManager {

    private static final String TAG = "MARKETPLACE_SOCKET";

    /** A message arrived in a conversation this phone has joined. */
    public interface MessageListener {
        void onNewMessage(@NonNull ChatMessage message);
    }

    /** A conversation's list entry changed (new last message, unread count...). */
    public interface ConversationListener {
        void onConversationUpdated();
    }

    private static SocketManager instance;

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final CopyOnWriteArrayList<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ConversationListener> conversationListeners = new CopyOnWriteArrayList<>();
    /** Conversations this phone wants to hear about; they are joined again after every reconnect. */
    private final Set<String> joined = Collections.synchronizedSet(new HashSet<>());

    @Nullable
    private Socket socket;
    @Nullable
    private String connectedToken;
    @Nullable
    private String connectedUrl;

    private SocketManager() { }

    /**
     * Connects when a person is signed in (and reconnects when the person or the server address changed).
     * Safe to call as often as you like.
     */
    public synchronized void ensureConnected() {
        SessionManager session = SessionManager.getInstance();
        String token = session.getToken();
        String url = session.getBaseUrl();
        if (token == null || token.isEmpty() || url == null) {
            disconnect();
            return;
        }
        if (socket != null && token.equals(connectedToken) && url.equals(connectedUrl)) {
            if (!socket.connected()) {
                socket.connect();
            }
            return;
        }
        disconnect();
        try {
            IO.Options options = IO.Options.builder()
                    .setAuth(Collections.singletonMap("token", token))
                    .setReconnection(true)
                    .build();
            Socket created = IO.socket(URI.create(url), options);
            created.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "connected");
                // After a reconnect the server has forgotten our rooms.
                synchronized (joined) {
                    for (String id : joined) {
                        created.emit("join_conversation", id);
                    }
                }
            });
            created.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "disconnected"));
            created.on(Socket.EVENT_CONNECT_ERROR, args -> Log.w(TAG, "connect error: "
                    + (args.length > 0 ? String.valueOf(args[0]) : "")));
            created.on("new_message", args -> {
                ChatMessage message = parseMessage(args);
                if (message != null) {
                    mainThread.post(() -> {
                        for (MessageListener listener : messageListeners) {
                            listener.onNewMessage(message);
                        }
                    });
                }
            });
            created.on("conversation_updated", args -> mainThread.post(() -> {
                for (ConversationListener listener : conversationListeners) {
                    listener.onConversationUpdated();
                }
            }));
            socket = created;
            connectedToken = token;
            connectedUrl = url;
            created.connect();
        } catch (RuntimeException badAddress) {
            Log.w(TAG, "could not start the live connection", badAddress);
        }
    }

    /** Drops the connection (signing out). */
    public synchronized void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
        connectedToken = null;
        connectedUrl = null;
    }

    /** Start hearing about one conversation (the chat is open). */
    public synchronized void join(@NonNull String conversationId) {
        joined.add(conversationId);
        ensureConnected();
        if (socket != null && socket.connected()) {
            socket.emit("join_conversation", conversationId);
        }
    }

    /** Stop hearing about one conversation (the chat is closed). */
    public synchronized void leave(@NonNull String conversationId) {
        joined.remove(conversationId);
        if (socket != null && socket.connected()) {
            socket.emit("leave_conversation", conversationId);
        }
    }

    public void addMessageListener(@NonNull MessageListener listener) {
        messageListeners.addIfAbsent(listener);
    }

    public void removeMessageListener(@NonNull MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addConversationListener(@NonNull ConversationListener listener) {
        conversationListeners.addIfAbsent(listener);
    }

    public void removeConversationListener(@NonNull ConversationListener listener) {
        conversationListeners.remove(listener);
    }

    @Nullable
    private ChatMessage parseMessage(Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof JSONObject)) {
            return null;
        }
        try {
            return gson.fromJson(args[0].toString(), ChatMessage.class);
        } catch (RuntimeException unreadable) {
            Log.w(TAG, "could not read a live message", unreadable);
            return null;
        }
    }
}
