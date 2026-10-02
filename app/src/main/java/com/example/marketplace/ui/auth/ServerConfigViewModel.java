package com.example.marketplace.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.common.HealthResponse;
import com.example.marketplace.data.repository.ServerRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/** Keeps the result of "Test connection" alive while the Server Connection sheet is rotated. */
public class ServerConfigViewModel extends ViewModel {

    private final ServerRepository repository = ServerRepository.getInstance();
    private final ResourceLoader<HealthResponse> health = new ResourceLoader<>();

    public LiveData<Resource<HealthResponse>> getHealth() {
        return health.asLiveData();
    }

    /** Tests the given (already cleaned up) server address. A newer test replaces an older one. */
    public void test(String serverUrl) {
        health.load(repository.checkHealth(serverUrl));
    }

    /** Forgets the last result, e.g. because the address was edited afterwards. */
    public void clearResult() {
        health.reset();
    }
}
