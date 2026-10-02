package com.example.marketplace.utils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

/**
 * Holds the latest state of one network call as LiveData and lets a ViewModel run the call again.
 * While a reload is running the previous data stays available, so the screen does not flash empty.
 * An answer that arrives from an older, replaced call is ignored.
 */
public class ResourceLoader<T> {

    private final MediatorLiveData<Resource<T>> state = new MediatorLiveData<>();
    @Nullable
    private LiveData<Resource<T>> currentSource;

    public ResourceLoader() {
        state.setValue(Resource.idle());
    }

    public LiveData<Resource<T>> asLiveData() {
        return state;
    }

    /** Forgets the current call and its result; the state goes back to idle. */
    public void reset() {
        if (currentSource != null) {
            state.removeSource(currentSource);
            currentSource = null;
        }
        state.setValue(Resource.idle());
    }

    /** Starts (or restarts) the call whose result is given as LiveData. */
    public void load(LiveData<Resource<T>> source) {
        if (currentSource != null) {
            state.removeSource(currentSource);
        }
        currentSource = source;
        state.addSource(source, resource -> {
            if (resource.status == Resource.Status.LOADING) {
                Resource<T> previous = state.getValue();
                state.setValue(Resource.loading(previous == null ? null : previous.data));
            } else {
                state.setValue(resource);
            }
        });
    }
}
