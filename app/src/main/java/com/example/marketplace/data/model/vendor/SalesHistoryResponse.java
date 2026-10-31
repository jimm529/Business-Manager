package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/sales-history: one page of orders, the paging numbers and the filter choices. */
public class SalesHistoryResponse {

    @SerializedName("orders")
    private List<SalesOrder> orders;
    @SerializedName("pagination")
    private Pagination pagination;
    @SerializedName("filterOptions")
    private FilterOptions filterOptions;

    private static class Pagination {
        @SerializedName("page")
        Integer page;
        @SerializedName("totalOrders")
        Integer totalOrders;
        @SerializedName("totalPages")
        Integer totalPages;
        @SerializedName("showingStart")
        Integer showingStart;
        @SerializedName("showingEnd")
        Integer showingEnd;
    }

    private static class FilterOptions {
        @SerializedName("materials")
        List<String> materials;
        @SerializedName("statuses")
        List<String> statuses;
    }

    @NonNull
    public List<SalesOrder> getOrders() {
        return orders == null ? new ArrayList<>() : orders;
    }

    public int getPage() { return pagination == null || pagination.page == null ? 1 : pagination.page; }

    public int getTotalPages() {
        return pagination == null || pagination.totalPages == null ? 1 : Math.max(1, pagination.totalPages);
    }

    public int getTotalOrders() {
        return pagination == null || pagination.totalOrders == null ? getOrders().size() : pagination.totalOrders;
    }

    public int getShowingStart() {
        return pagination == null || pagination.showingStart == null ? 0 : pagination.showingStart;
    }

    public int getShowingEnd() {
        return pagination == null || pagination.showingEnd == null ? getOrders().size() : pagination.showingEnd;
    }

    /** Materials this vendor has sold, for the filter sheet. */
    @NonNull
    public List<String> getMaterials() {
        return filterOptions == null || filterOptions.materials == null ? new ArrayList<>() : filterOptions.materials;
    }

    /** Statuses that exist among this vendor's orders, for the filter sheet. */
    @NonNull
    public List<String> getStatuses() {
        return filterOptions == null || filterOptions.statuses == null ? new ArrayList<>() : filterOptions.statuses;
    }
}
