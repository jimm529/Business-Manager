package com.example.marketplace.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.marketplace.data.model.auth.User;

/**
 * Keeps the login token (and who is signed in) in encrypted storage.
 * Also tells the UI when the server rejected the token (401) so it can go back to Login.
 */
public class SessionManager {

    private static final String FILE_NAME = "marketplace_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";

    private static SessionManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>(false);

    private SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = createPrefs();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SessionManager.init() must be called first");
        }
        return instance;
    }

    private SharedPreferences createPrefs() {
        try {
            return openEncrypted();
        } catch (Exception firstFailure) {
            // The key store can lose its key (e.g. after a restore). Start clean instead of crashing.
            appContext.deleteSharedPreferences(FILE_NAME);
            try {
                return openEncrypted();
            } catch (Exception secondFailure) {
                throw new IllegalStateException("Secure storage is not available", secondFailure);
            }
        }
    }

    private SharedPreferences openEncrypted() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                appContext,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public synchronized void saveSession(String token, @Nullable User user) {
        SharedPreferences.Editor editor = prefs.edit().putString(KEY_TOKEN, token);
        if (user != null) {
            editor.putString(KEY_ROLE, user.getRole())
                    .putString(KEY_USER_ID, user.getId())
                    .putString(KEY_NAME, user.getFullName())
                    .putString(KEY_EMAIL, user.getEmail());
        }
        editor.apply();
    }

    @Nullable
    public synchronized String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    @Nullable
    public synchronized String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public synchronized boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public synchronized void clear() {
        prefs.edit().clear().apply();
    }

    /** Called by the network layer when the server answers 401 to a signed-in request. */
    public void notifySessionExpired() {
        clear();
        sessionExpired.postValue(true);
    }

    public LiveData<Boolean> getSessionExpired() {
        return sessionExpired;
    }

    /** The UI calls this after it has sent the user back to Login. */
    public void consumeSessionExpired() {
        sessionExpired.setValue(false);
    }
}
