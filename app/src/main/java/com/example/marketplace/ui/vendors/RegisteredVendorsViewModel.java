package com.example.marketplace.ui.vendors;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.CreateVendorRequest;
import com.example.marketplace.data.model.vendor.RegisteredVendor;
import com.example.marketplace.data.model.vendor.RegisteredVendorsResponse;
import com.example.marketplace.data.repository.VendorRepository;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.List;

/**
 * Holds the vendor list, the search text and the status filter so they survive rotation.
 * The server does the searching and filtering; typing waits a moment before asking again.
 */
public class RegisteredVendorsViewModel extends ViewModel {

    private static final long SEARCH_DELAY_MS = 400;

    private final VendorRepository repository = VendorRepository.getInstance();
    private final ResourceLoader<RegisteredVendorsResponse> loader = new ResourceLoader<>();
    private final ResourceLoader<MessageResponse> createLoader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<MessageResponse>> createState = new MediatorLiveData<>();
    private final LiveData<Resource<List<RegisteredVendor>>> vendors;
    private final MutableLiveData<Event<Integer>> successMessages = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::refresh;

    private String query = "";
    /** One of RegisteredVendor.STATUS_*, or null for "all". */
    @Nullable
    private String statusFilter;

    public RegisteredVendorsViewModel() {
        vendors = Transformations.map(loader.asLiveData(), resource ->
                resource.withData(resource.data == null ? null : resource.data.getVendors()));
        createState.addSource(createLoader.asLiveData(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                successMessages.setValue(new Event<>(R.string.vendors_registered));
                refresh();
            }
            createState.setValue(resource);
        });
        refresh();
    }

    /** The vendors that match the search and filter (loading, error, or success with the list). */
    public LiveData<Resource<List<RegisteredVendor>>> getVendors() {
        return vendors;
    }

    /** State of "Register Vendor". */
    public LiveData<Resource<MessageResponse>> getCreateState() {
        return createState;
    }

    /** One-time "vendor registered" message. */
    public LiveData<Event<Integer>> getSuccessMessages() {
        return successMessages;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    @Nullable
    public String getStatusFilter() {
        return statusFilter;
    }

    /** Called on every key; the list is reloaded once the person pauses typing. */
    public void setQuery(@Nullable String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(query)) {
            query = clean;
            handler.removeCallbacks(searchRunnable);
            handler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
        }
    }

    public void setStatusFilter(@Nullable String status) {
        if (status == null ? statusFilter != null : !status.equals(statusFilter)) {
            statusFilter = status;
            refresh();
        }
    }

    public void refresh() {
        handler.removeCallbacks(searchRunnable);
        loader.load(repository.getVendors(escapeForServer(query), statusFilter));
    }

    public void createVendor(@NonNull CreateVendorRequest request) {
        createLoader.load(repository.createVendor(request));
    }

    /** The dialog is closed: forget the result. */
    public void clearCreateState() {
        createLoader.reset();
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(searchRunnable);
    }

    /** The server treats the search as a regular expression, so special characters are escaped to be searched literally. */
    @Nullable
    private static String escapeForServer(@NonNull String text) {
        return text.isEmpty() ? null : text.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }
}
