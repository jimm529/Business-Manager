package com.example.marketplace.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.utils.Resource;

import java.util.HashMap;
import java.util.Map;

/** The calls behind the Orders and Purchase History screens of the More tab. */
public class OrdersRepository {

    /** The largest page the backend allows for purchase history. */
    private static final int HISTORY_PAGE_SIZE = 100;

    private static OrdersRepository instance;

    private OrdersRepository() { }

    public static synchronized OrdersRepository getInstance() {
        if (instance == null) {
            instance = new OrdersRepository();
        }
        return instance;
    }

    /** Every order of the signed-in user, newest first (GET orders). */
    public LiveData<Resource<OrdersResponse>> getOrders() {
        return ApiResults.enqueue(ApiClient.getApiService().getOrders());
    }

    /** Totals plus the newest past purchases, with no date limit (GET purchase-history). Buyers only. */
    public LiveData<Resource<PurchaseHistoryResponse>> getPurchaseHistory() {
        return ApiResults.enqueue(ApiClient.getApiService().getPurchaseHistory(HISTORY_PAGE_SIZE, null));
    }

    /** One page of past purchases for the Purchase History screen; status is a server key or null for all. */
    public LiveData<Resource<PurchaseHistoryResponse>> getPurchaseHistoryPage(int page, int pageSize,
                                                                              @Nullable String status) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(pageSize));
        if (status != null && !status.isEmpty()) {
            params.put("status", status);
        }
        return ApiResults.enqueue(ApiClient.getApiService().getPurchaseHistoryPage(params));
    }
}
