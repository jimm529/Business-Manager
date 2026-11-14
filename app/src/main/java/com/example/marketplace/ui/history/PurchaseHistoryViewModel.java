package com.example.marketplace.ui.history;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.repository.OrdersRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

/**
 * Holds the Purchase History: which status is chosen and which page is shown, and loads the matching
 * page from the server. Changing the status goes back to page 1. Survives rotation.
 */
public class PurchaseHistoryViewModel extends ViewModel {

    private static final int PAGE_SIZE = 10;

    /** The status chips. The key is what the server expects; ALL leaves the filter out. */
    public enum StatusFilter {
        ALL(null, R.string.history_filter_all),
        COMPLETED("completed", R.string.history_filter_completed),
        ON_THE_WAY("on_the_way", R.string.history_filter_on_the_way),
        DISPATCHED("dispatched", R.string.history_filter_dispatched),
        PROCESSING("processing", R.string.history_filter_processing),
        AWAITING_DOCS("awaiting_docs", R.string.history_filter_awaiting_docs),
        CANCELLED("cancelled", R.string.history_filter_cancelled);

        @Nullable
        final String key;
        @StringRes
        public final int label;

        StatusFilter(@Nullable String key, int label) {
            this.key = key;
            this.label = label;
        }
    }

    private final ResourceLoader<PurchaseHistoryResponse> history = new ResourceLoader<>();
    private StatusFilter filter = StatusFilter.ALL;
    private int page = 1;

    public PurchaseHistoryViewModel() {
        refresh();
    }

    public LiveData<Resource<PurchaseHistoryResponse>> getHistory() {
        return history.asLiveData();
    }

    @NonNull
    public StatusFilter getFilter() {
        return filter;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public void setFilter(@NonNull StatusFilter newFilter) {
        if (newFilter != filter) {
            filter = newFilter;
            page = 1;
            refresh();
        }
    }

    public void goToPage(int newPage) {
        if (newPage >= 1 && newPage != page) {
            page = newPage;
            refresh();
        }
    }

    public void refresh() {
        history.load(OrdersRepository.getInstance().getPurchaseHistoryPage(page, PAGE_SIZE, filter.key));
    }
}
