package com.example.marketplace.ui.vendorrequests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.OrderRequestItem;
import com.example.marketplace.data.model.vendor.OrderRequestsResponse;
import com.example.marketplace.data.model.vendor.RequestActionResponse;
import com.example.marketplace.data.repository.VendorOrdersRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Holds the vendor's Order Requests Ledger: the loaded requests, the search text, the status
 * filter, how many rows are shown, and accept / reject. Survives rotation.
 * Search and filters work on the requests already loaded, so they react instantly.
 */
public class VendorRequestsViewModel extends ViewModel {

    private static final int PAGE_SIZE = 20;

    /** Which requests the list shows. */
    public enum StatusFilter {
        ALL(R.string.requests_filter_all),
        PENDING(R.string.requests_filter_pending),
        COUNTER(R.string.requests_filter_counter),
        ACCEPTED(R.string.requests_filter_accepted),
        REJECTED(R.string.requests_filter_rejected);

        @StringRes
        public final int label;

        StatusFilter(int label) {
            this.label = label;
        }

        boolean matches(OrderRequestItem item) {
            switch (this) {
                case PENDING:
                    return item.isPending();
                case COUNTER:
                    return item.isCounter();
                case ACCEPTED:
                    return item.isAccepted();
                case REJECTED:
                    return !item.isOpen() && !item.isAccepted();
                default:
                    return true;
            }
        }
    }

    /** The numbers on the stat tiles, worked out from the loaded requests (never invented). */
    public static final class Stats {
        public final int active;
        public final int pending;
        public final double totalQuantityMT;
        @Nullable
        public final Double averageRate;
        @Nullable
        public final String focusMaterial;
        /** The open requests, newest first: what the insights panel talks about. */
        @NonNull
        public final List<OrderRequestItem> open;

        Stats(int active, int pending, double totalQuantityMT, @Nullable Double averageRate,
              @Nullable String focusMaterial, @NonNull List<OrderRequestItem> open) {
            this.active = active;
            this.pending = pending;
            this.totalQuantityMT = totalQuantityMT;
            this.averageRate = averageRate;
            this.focusMaterial = focusMaterial;
            this.open = open;
        }
    }

    /** What the screen shows: the rows for the current search and filter, plus the stats. */
    public static final class Ledger {
        @NonNull
        public final List<OrderRequestItem> rows;
        public final boolean hasMore;
        public final int totalRequests;
        @NonNull
        public final Stats stats;

        Ledger(@NonNull List<OrderRequestItem> rows, boolean hasMore, int totalRequests, @NonNull Stats stats) {
            this.rows = rows;
            this.hasMore = hasMore;
            this.totalRequests = totalRequests;
            this.stats = stats;
        }
    }

    private final VendorOrdersRepository repository = VendorOrdersRepository.getInstance();
    private final ResourceLoader<OrderRequestsResponse> loader = new ResourceLoader<>();
    private final MediatorLiveData<Resource<Ledger>> ledger = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<ActionResult>> results = new MutableLiveData<>();

    private String query = "";
    private StatusFilter filter = StatusFilter.ALL;
    private int visible = PAGE_SIZE;

    public VendorRequestsViewModel() {
        ledger.addSource(loader.asLiveData(), state -> publish());
        refresh();
    }

    public LiveData<Resource<Ledger>> getLedger() {
        return ledger;
    }

    /** True while an accept or reject is running. */
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    public LiveData<Event<ActionResult>> getResults() {
        return results;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    @NonNull
    public StatusFilter getFilter() {
        return filter;
    }

    public void setQuery(@Nullable String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(query)) {
            query = clean;
            visible = PAGE_SIZE;
            publish();
        }
    }

    public void setFilter(@NonNull StatusFilter newFilter) {
        if (newFilter != filter) {
            filter = newFilter;
            visible = PAGE_SIZE;
            publish();
        }
    }

    public void showMore() {
        visible += PAGE_SIZE;
        publish();
    }

    public void refresh() {
        loader.load(repository.getRequests());
    }

    public void accept(@NonNull String requestId) {
        runAction(repository.accept(requestId), R.string.requests_accepted);
    }

    public void reject(@NonNull String requestId) {
        runAction(repository.reject(requestId), R.string.requests_rejected);
    }

    private void runAction(LiveData<Resource<RequestActionResponse>> call, @StringRes int successText) {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        call.observeForever(new Observer<Resource<RequestActionResponse>>() {
            @Override
            public void onChanged(Resource<RequestActionResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                busy.setValue(false);
                if (result.status == Resource.Status.SUCCESS) {
                    String message = result.data == null ? null : result.data.getMessage();
                    results.setValue(new Event<>(ActionResult.success(message, successText)));
                } else {
                    results.setValue(new Event<>(ActionResult.failure(result)));
                }
                refresh();
            }
        });
    }

    // ---------------------------------------------------------------- building the screen

    private void publish() {
        Resource<OrderRequestsResponse> state = loader.asLiveData().getValue();
        if (state == null) {
            return;
        }
        if (state.data == null) {
            ledger.setValue(state.withData(null));
            return;
        }
        List<OrderRequestItem> all = new ArrayList<>(state.data.getRequests());
        // Waiting for the vendor first, then the newest.
        Collections.sort(all, (a, b) -> {
            if (a.isPending() != b.isPending()) {
                return a.isPending() ? -1 : 1;
            }
            Date da = Formatters.parseIso(a.getCreatedAt());
            Date db = Formatters.parseIso(b.getCreatedAt());
            if (da == null || db == null) {
                return da == null ? (db == null ? 0 : 1) : -1;
            }
            return db.compareTo(da);
        });

        List<OrderRequestItem> matching = new ArrayList<>();
        String needle = query.toLowerCase(Locale.getDefault());
        for (OrderRequestItem item : all) {
            if (filter.matches(item) && (needle.isEmpty() || matchesSearch(item, needle))) {
                matching.add(item);
            }
        }
        boolean hasMore = matching.size() > visible;
        List<OrderRequestItem> rows = new ArrayList<>(matching.subList(0, Math.min(visible, matching.size())));
        ledger.setValue(state.withData(new Ledger(rows, hasMore, all.size(), computeStats(all))));
    }

    private static boolean matchesSearch(OrderRequestItem item, String needle) {
        return contains(item.getOrderId(), needle) || contains(item.getBuyerName(), needle)
                || contains(item.getMaterial(), needle);
    }

    private static boolean contains(@Nullable String text, String needle) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(needle);
    }

    private static Stats computeStats(List<OrderRequestItem> all) {
        List<OrderRequestItem> open = new ArrayList<>();
        int pending = 0;
        double quantity = 0;
        double rateSum = 0;
        int rateCount = 0;
        Map<String, Integer> materials = new HashMap<>();
        for (OrderRequestItem item : all) {
            if (!item.isOpen()) {
                continue;
            }
            open.add(item);
            if (item.isPending()) {
                pending++;
            }
            if (item.getQuantityMT() != null) {
                quantity += item.getQuantityMT();
            }
            if (item.getOfferedRate() != null) {
                rateSum += item.getOfferedRate();
                rateCount++;
            }
            if (item.getMaterial() != null && !item.getMaterial().trim().isEmpty()) {
                Integer count = materials.get(item.getMaterial());
                materials.put(item.getMaterial(), count == null ? 1 : count + 1);
            }
        }
        String focus = null;
        int best = 0;
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                focus = entry.getKey();
            }
        }
        return new Stats(open.size(), pending, quantity, rateCount > 0 ? rateSum / rateCount : null, focus, open);
    }
}
