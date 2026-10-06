package com.example.marketplace.ui.market;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.BookmarkResponse;
import com.example.marketplace.data.model.market.MarketProduct;
import com.example.marketplace.data.model.market.MarketplaceResponse;
import com.example.marketplace.data.model.market.OrderResponse;
import com.example.marketplace.data.repository.MarketRepository;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the Market tab needs: the listings, live search, the filters (material and location
 * are sent to the server; minimum weight and "verified only" are applied here on the loaded list),
 * saving a lot and placing an order. It survives rotation.
 */
public class MarketViewModel extends ViewModel {

    /** The filters shown as chips. Never changed after creation. */
    public static class FilterState {
        @Nullable public final String material;
        @Nullable public final String regionValue;
        @Nullable public final String regionLabel;
        /** Smallest lot size in kilograms, or null for any size. */
        @Nullable public final Integer minWeightKg;
        public final boolean verifiedOnly;

        public FilterState(@Nullable String material, @Nullable String regionValue, @Nullable String regionLabel,
                           @Nullable Integer minWeightKg, boolean verifiedOnly) {
            this.material = material;
            this.regionValue = regionValue;
            this.regionLabel = regionLabel;
            this.minWeightKg = minWeightKg;
            this.verifiedOnly = verifiedOnly;
        }

        public boolean isEmpty() {
            return material == null && regionValue == null && minWeightKg == null && !verifiedOnly;
        }
    }

    /** Outcome of a save or order action, to show once as a message. */
    public static class ActionResult {
        /** Text for a success, or 0 when the action failed. */
        @StringRes public final int successRes;
        /** The failed call (its message is shown), or null on success. */
        @Nullable public final Resource<?> error;
        /** Message sent by the server on success (may be null). */
        @Nullable public final String serverMessage;

        ActionResult(@StringRes int successRes, @Nullable Resource<?> error, @Nullable String serverMessage) {
            this.successRes = successRes;
            this.error = error;
            this.serverMessage = serverMessage;
        }
    }

    private final MarketRepository repository = MarketRepository.getInstance();

    private final ResourceLoader<MarketplaceResponse> response = new ResourceLoader<>();
    private final MediatorLiveData<Resource<List<MarketProduct>>> products = new MediatorLiveData<>();
    private final MutableLiveData<FilterState> filters =
            new MutableLiveData<>(new FilterState(null, null, null, null, false));
    private final MutableLiveData<Event<ActionResult>> actionResults = new MutableLiveData<>();
    /** Saved states changed by the buyer since the last reload (lot id -> saved). */
    private final Map<String, Boolean> bookmarkOverrides = new HashMap<>();

    private String searchQuery = "";

    public MarketViewModel() {
        products.addSource(response.asLiveData(), resource -> publish());
        products.addSource(filters, state -> publish());
        load();
    }

    public LiveData<Resource<List<MarketProduct>>> getProducts() { return products; }
    public LiveData<FilterState> getFilters() { return filters; }
    public LiveData<Event<ActionResult>> getActionResults() { return actionResults; }
    public String getSearchQuery() { return searchQuery; }

    @NonNull
    private FilterState currentFilters() {
        FilterState state = filters.getValue();
        return state == null ? new FilterState(null, null, null, null, false) : state;
    }

    /** Materials the server knows about, for the filter sheet. Empty until the first answer. */
    @NonNull
    public List<String> getAvailableMaterials() {
        Resource<MarketplaceResponse> latest = response.asLiveData().getValue();
        return latest == null || latest.data == null ? new ArrayList<>() : latest.data.getMaterials();
    }

    /** Locations the server knows about, for the filter sheet. Empty until the first answer. */
    @NonNull
    public List<MarketplaceResponse.Region> getAvailableRegions() {
        Resource<MarketplaceResponse> latest = response.asLiveData().getValue();
        return latest == null || latest.data == null ? new ArrayList<>() : latest.data.getRegions();
    }

    /** Reloads the listings (pull-to-refresh and Retry). */
    public void refresh() {
        load();
    }

    public void setSearchQuery(@Nullable String query) {
        String clean = query == null ? "" : query.trim();
        if (!clean.equals(searchQuery)) {
            searchQuery = clean;
            load();
        }
    }

    /** Applies all filters of the filter sheet at once. */
    public void setFilters(@Nullable String material, @Nullable String regionValue, @Nullable String regionLabel,
                           @Nullable Integer minWeightKg, boolean verifiedOnly) {
        FilterState old = currentFilters();
        boolean serverSideChanged = !same(old.material, material) || !same(old.regionValue, regionValue);
        filters.setValue(new FilterState(material, regionValue, regionLabel, minWeightKg, verifiedOnly));
        if (serverSideChanged) {
            load();
        }
    }

    public void removeMaterial() {
        FilterState s = currentFilters();
        setFilters(null, s.regionValue, s.regionLabel, s.minWeightKg, s.verifiedOnly);
    }

    public void removeRegion() {
        FilterState s = currentFilters();
        setFilters(s.material, null, null, s.minWeightKg, s.verifiedOnly);
    }

    public void removeWeight() {
        FilterState s = currentFilters();
        setFilters(s.material, s.regionValue, s.regionLabel, null, s.verifiedOnly);
    }

    public void removeVerified() {
        FilterState s = currentFilters();
        setFilters(s.material, s.regionValue, s.regionLabel, s.minWeightKg, false);
    }

    /** Saves or un-saves a lot. The card changes at once and goes back if the server says no. */
    public void toggleBookmark(@NonNull MarketProduct product) {
        final String id = product.getId();
        if (id == null) {
            return;
        }
        final boolean before = product.isBookmarked();
        bookmarkOverrides.put(id, !before);
        publish();

        runOnce(repository.toggleBookmark(id), (Resource<BookmarkResponse> result) -> {
            if (result.status == Resource.Status.SUCCESS) {
                Boolean saved = result.data == null ? null : result.data.getBookmarked();
                bookmarkOverrides.put(id, saved == null ? !before : saved);
                publish();
            } else {
                bookmarkOverrides.put(id, before);
                publish();
                actionResults.setValue(new Event<>(new ActionResult(0, result, null)));
            }
        });
    }

    /** Sends an order request for the lot at its listed price and quantity. */
    public void placeOrder(@NonNull MarketProduct product) {
        String id = product.getId();
        if (id == null) {
            return;
        }
        runOnce(repository.placeOrder(id), (Resource<OrderResponse> result) -> {
            if (result.status == Resource.Status.SUCCESS) {
                actionResults.setValue(new Event<>(new ActionResult(R.string.market_order_sent, null,
                        result.data == null ? null : result.data.getMessage())));
                load(); // the card now shows the buyer's own bid
            } else {
                actionResults.setValue(new Event<>(new ActionResult(0, result, null)));
            }
        });
    }

    // ---------------------------------------------------------------- internals

    private void load() {
        FilterState state = currentFilters();
        bookmarkOverrides.clear();
        response.load(repository.getLots(searchQuery, state.material, state.regionValue));
    }

    /** Builds the visible list: the loaded lots, minus the ones the local filters hide, with saved states applied. */
    private void publish() {
        Resource<MarketplaceResponse> latest = response.asLiveData().getValue();
        if (latest == null) {
            return;
        }
        if (latest.status == Resource.Status.IDLE) {
            return;
        }
        if (latest.status == Resource.Status.ERROR) {
            products.setValue(latest.withData(null));
            return;
        }
        if (latest.data == null) {
            // Still loading the first time: nothing to show yet.
            products.setValue(latest.status == Resource.Status.SUCCESS
                    ? Resource.success(new ArrayList<>()) : latest.withData(null));
            return;
        }

        FilterState state = currentFilters();
        List<MarketProduct> visible = new ArrayList<>();
        for (MarketProduct product : latest.data.getProducts()) {
            if (state.minWeightKg != null) {
                Double kg = product.getQuantityKg();
                if (kg == null || kg < state.minWeightKg) {
                    continue;
                }
            }
            if (state.verifiedOnly && !product.isVerified()) {
                continue;
            }
            Boolean override = product.getId() == null ? null : bookmarkOverrides.get(product.getId());
            visible.add(override == null || override == product.isBookmarked()
                    ? product : product.withBookmarked(override));
        }
        products.setValue(latest.withData(visible));
    }

    /** Receives the final (non-loading) result of one call. */
    private interface Callback<T> {
        void onResult(Resource<T> result);
    }

    /** Waits for the call to finish, hands over the result once and stops listening. */
    private <T> void runOnce(LiveData<Resource<T>> source, Callback<T> callback) {
        source.observeForever(new Observer<Resource<T>>() {
            @Override
            public void onChanged(Resource<T> resource) {
                if (resource.status == Resource.Status.LOADING || resource.status == Resource.Status.IDLE) {
                    return;
                }
                source.removeObserver(this);
                callback.onResult(resource);
            }
        });
    }

    private static boolean same(@Nullable String a, @Nullable String b) {
        return a == null ? b == null : a.equals(b);
    }
}
