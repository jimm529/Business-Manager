package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.vendor.OrderDocumentResponse;
import com.example.marketplace.data.model.vendor.SalesHistoryResponse;
import com.example.marketplace.utils.Resource;

import java.util.Map;

/** The vendor's Sales History: pages of orders with filters, and the document of one order. */
public class VendorSalesRepository {

    private static VendorSalesRepository instance;

    private VendorSalesRepository() { }

    public static synchronized VendorSalesRepository getInstance() {
        if (instance == null) {
            instance = new VendorSalesRepository();
        }
        return instance;
    }

    /** GET vendor-portal/sales-history. The map holds only the filters that are set. */
    public LiveData<Resource<SalesHistoryResponse>> getSales(@NonNull Map<String, String> filters) {
        return ApiResults.enqueue(ApiClient.getApiService().getSalesHistory(filters));
    }

    /** GET vendor-portal/orders/{id}/document. */
    public LiveData<Resource<OrderDocumentResponse>> getDocument(@NonNull String orderId, boolean download) {
        return ApiResults.enqueue(ApiClient.getApiService().getOrderDocument(orderId, download));
    }
}
