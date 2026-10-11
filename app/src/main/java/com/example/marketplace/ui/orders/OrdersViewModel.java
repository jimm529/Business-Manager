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
 * Loads the orders once and keeps the chosen status filter, so rotation neither reloads nor loses it.
 * The four filters group the backend's statuses; "All" shows everything.
 */
public class OrdersViewModel extends ViewModel {

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

        boolean matches(@Nullable String status) {
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

    private final ResourceLoader<OrdersResponse> loader = new ResourceLoader<>();
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MediatorLiveData<Resource<List<Order>>> orders = new MediatorLiveData<>();

    public OrdersViewModel() {
        orders.addSource(loader.asLiveData(), resource -> publish());
        orders.addSource(filter, selected -> publish());
        refresh();
    }

    /** The orders that match the chosen filter (loading, error, or success with the list). */
    public LiveData<Resource<List<Order>>> getOrders() {
        return orders;
    }

    @NonNull
    public Filter getFilter() {
        Filter current = filter.getValue();
        return current == null ? Filter.ALL : current;
    }

    public void setFilter(@NonNull Filter selected) {
        if (selected != getFilter()) {
            filter.setValue(selected);
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
        List<Order> shown = null;
        if (state.data != null) {
            shown = new ArrayList<>();
            Filter selected = getFilter();
            for (Order order : state.data.getOrders()) {
                if (order != null && selected.matches(order.getStatus())) {
                    shown.add(order);
                }
            }
        }
        orders.setValue(state.withData(shown));
    }
}
