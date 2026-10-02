package com.example.marketplace.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.common.HealthResponse;
import com.example.marketplace.utils.Resource;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Checks whether a server address is reachable, without touching the address the app currently uses. */
public class ServerRepository {

    /** Short on purpose: a wrong address should fail fast instead of making the user wait. */
    private static final int TEST_TIMEOUT_SECONDS = 5;

    private static ServerRepository instance;

    private ServerRepository() { }

    public static synchronized ServerRepository getInstance() {
        if (instance == null) {
            instance = new ServerRepository();
        }
        return instance;
    }

    /**
     * Calls GET health on the given server address (already cleaned up by ServerUrls).
     * Success carries the server's answer, including the database status.
     */
    public LiveData<Resource<HealthResponse>> checkHealth(@NonNull String serverUrl) {
        MutableLiveData<Resource<HealthResponse>> result = new MutableLiveData<>(Resource.loading());
        Call<HealthResponse> call;
        try {
            call = ApiClient.createService(serverUrl, TEST_TIMEOUT_SECONDS).getHealth();
        } catch (IllegalArgumentException invalidAddress) {
            result.setValue(Resource.error(R.string.server_url_invalid));
            return result;
        }

        call.enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(@NonNull Call<HealthResponse> call, @NonNull Response<HealthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    // Something answered, but it is not our backend (or it is broken).
                    result.postValue(Resource.errorWithArgs(R.string.server_test_http, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<HealthResponse> call, @NonNull Throwable t) {
                Log.w(ApiClient.LOG_TAG, "Health check to " + call.request().url() + " failed: " + t);
                result.postValue(t instanceof IOException
                        ? Resource.error(R.string.server_test_unreachable)
                        : Resource.error(R.string.error_generic));
            }
        });
        return result;
    }
}
