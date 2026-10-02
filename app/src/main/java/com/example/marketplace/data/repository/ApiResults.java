package com.example.marketplace.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.BuildConfig;
import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shared helpers every repository uses: run a Retrofit call and report it as LiveData,
 * and turn failures into messages (the server's own message when there is one).
 */
final class ApiResults {

    private static final String TAG = ApiClient.LOG_TAG;

    /** Decides what to do with a successful (2xx) answer, e.g. save a token first. */
    interface SuccessHandler<T> {
        Resource<T> handle(@NonNull T body);
    }

    private ApiResults() { }

    static <T> LiveData<Resource<T>> enqueue(Call<T> call) {
        return enqueue(call, Resource::success);
    }

    static <T> LiveData<Resource<T>> enqueue(Call<T> call, SuccessHandler<T> onSuccess) {
        MutableLiveData<Resource<T>> result = new MutableLiveData<>(Resource.loading());
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                if (!response.isSuccessful()) {
                    // 400 / 401 / 403 / 404 / 409 / 500: show exactly what the server said.
                    String serverMessage = readErrorMessage(response);
                    Log.w(TAG, "HTTP " + response.code() + " from server: " + serverMessage);
                    result.postValue(Resource.error(serverMessage, R.string.error_generic));
                    return;
                }
                T body = response.body();
                result.postValue(body == null
                        ? Resource.error(R.string.error_generic)
                        : onSuccess.handle(body));
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                Log.e(TAG, call.request().method() + " " + call.request().url() + " failed", t);
                result.postValue(describeFailure(t));
            }
        });
        return result;
    }

    /** The request never got an answer: work out why, so the message actually helps. */
    static <T> Resource<T> describeFailure(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return Resource.error(R.string.error_timeout);
        }
        if (t instanceof IOException) {
            // Debug builds show which address was tried; that is the first thing to check.
            if (BuildConfig.DEBUG) {
                String url = SessionManager.getInstance().getBaseUrl();
                HttpUrl base = HttpUrl.parse(url);
                String address = base == null ? url : base.host() + ":" + base.port();
                return Resource.errorWithArgs(R.string.error_cannot_reach_server_debug, address);
            }
            return Resource.error(R.string.error_network);
        }
        return Resource.error(R.string.error_generic);
    }

    /**
     * Reads the human message out of an error answer. The Express backend sends
     * {"message": "..."}; older handlers may use "msg" or "error" (text or an object with "message").
     * Returns null when the body is empty, not JSON (e.g. an HTML error page) or has no message.
     */
    @Nullable
    static String readErrorMessage(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody == null) {
                return null;
            }
            JsonElement root = JsonParser.parseString(errorBody.string());
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject object = root.getAsJsonObject();
            for (String key : new String[]{"message", "msg", "error"}) {
                String text = asText(object.get(key));
                if (text != null) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // Unreadable body: the caller falls back to a generic message.
        }
        return null;
    }

    @Nullable
    private static String asText(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            String text = element.getAsString().trim();
            return text.isEmpty() ? null : text;
        }
        if (element.isJsonObject()) {
            return asText(element.getAsJsonObject().get("message"));
        }
        return null;
    }
}
