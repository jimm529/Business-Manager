package com.example.marketplace.ui.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.data.repository.OrdersRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads the orders once and keeps the chosen status filter and page, so rotation neither reloads nor loses
 * them. The filters group the backend's statuses; "All" shows everything. The server sends every order in one
 * answer, so the list is cut into pages of {@link #PAGE_SIZE} here.
 */
public class OrdersViewModel extends ViewModel {

    public static final int PAGE_SIZE = 10;

    /** The filter chips. Each one covers one or more statuses the backend uses. */
    public enum Filter {
        ALL(),
        PENDING("processing", "awaiting docs"),
        IN_PROGRESS("on the way", "in-transit", "dispatched"),
        DELIVERED("delivered", "completed"),
        CANCELLED("cancelled");

        private final String[] statuses;

        Filter(String... statuses) {
            this.statuses = statuses;
        }

        public boolean matches(@Nullable String status) {
            if (this == ALL) {
                return true;
            }
            if (status == null) {
                return false;
            }
            String normal = status.trim().toLowerCase(Locale.ROOT);
            for (String candidate : statuses) {
                if (candidate.equals(normal)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** What the screen draws: the orders of the current page plus the numbers around them. */
    public static class Page {
        public final List<Order> items;
        public final int page;
        public final int totalPages;
        /** Orders that match the chosen filter (all pages). */
        public final int matching;
        public final int totalOrders;
        public final int inProgress;
        public final int delivered;

        Page(List<Order> items, int page, int totalPages, int matching, int totalOrders, int inProgress,
             int delivered) {
            this.items = items;
            this.page = page;
            this.totalPages = totalPages;
            this.matching = matching;
            this.totalOrders = totalOrders;
            this.inProgress = inProgress;
            this.delivered = delivered;
        }
    }

    private final ResourceLoader<OrdersResponse> loader = new ResourceLoader<>();
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MutableLiveData<Integer> page = new MutableLiveData<>(1);
    private final MediatorLiveData<Resource<Page>> orders = new MediatorLiveData<>();

    public OrdersViewModel() {
        orders.addSource(loader.asLiveData(), resource -> publish());
        orders.addSource(filter, selected -> publish());
        orders.addSource(page, selected -> publish());
        refresh();
    }

    /** The current page of orders that match the chosen filter (loading, error, or success). */
    public LiveData<Resource<Page>> getOrders() {
        return orders;
    }

    @NonNull
    public Filter getFilter() {
        Filter current = filter.getValue();
        return current == null ? Filter.ALL : current;
    }

    public void setFilter(@NonNull Filter selected) {
        if (selected != getFilter()) {
            page.setValue(1);
            filter.setValue(selected);
        }
    }

    public void goToPage(int target) {
        Integer current = page.getValue();
        if (target >= 1 && (current == null || current != target)) {
            page.setValue(target);
        }
    }

    public void refresh() {
        loader.load(OrdersRepository.getInstance().getOrders());
    }

    private void publish() {
        Resource<OrdersResponse> state = loader.asLiveData().getValue();
        if (state == null) {
            return;
        }
        Page shown = null;
        if (state.data != null) {
            List<Order> matching = new ArrayList<>();
            int all = 0;
            int inProgress = 0;
            int delivered = 0;
            Filter selected = getFilter();
            for (Order order : state.data.getOrders()) {
                if (order == null) {
                    continue;
                }
                all++;
                if (Filter.IN_PROGRESS.matches(order.getStatus())) {
                    inProgress++;
                } else if (Filter.DELIVERED.matches(order.getStatus())) {
                    delivered++;
                }
                if (selected.matches(order.getStatus())) {
                    matching.add(order);
                }
            }
            int pages = Math.max(1, (matching.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            Integer wanted = page.getValue();
            int current = Math.min(Math.max(1, wanted == null ? 1 : wanted), pages);
            int from = (current - 1) * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, matching.size());
            shown = new Page(new ArrayList<>(matching.subList(from, to)), current, pages, matching.size(), all,
                    inProgress, delivered);
        }
        orders.setValue(state.withData(shown));
    }
}
