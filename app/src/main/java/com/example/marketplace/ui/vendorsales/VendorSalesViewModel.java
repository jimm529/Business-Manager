package com.example.marketplace.ui.vendorsales;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.OrderDocumentResponse;
import com.example.marketplace.data.model.vendor.SalesHistoryResponse;
import com.example.marketplace.data.repository.VendorSalesRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Holds the vendor's Sales History: the search, date range, filters, sort order and current page,
 * and loads the matching page from the server. Survives rotation.
 */
public class VendorSalesViewModel extends ViewModel {

    private static final int PAGE_SIZE = 10;

    /** How the list is ordered. */
    public enum Sort {
        NEWEST(R.string.sales_sort_newest, "createdAt", "desc"),
        OLDEST(R.string.sales_sort_oldest, "createdAt", "asc"),
        VALUE_HIGH(R.string.sales_sort_value_high, "value", "desc"),
        VALUE_LOW(R.string.sales_sort_value_low, "value", "asc"),
        PURITY_HIGH(R.string.sales_sort_purity_high, "purity", "desc");

        @StringRes
        public final int label;
        final String field;
        final String order;

        Sort(int label, String field, String order) {
            this.label = label;
            this.field = field;
            this.order = order;
        }
    }

    private final VendorSalesRepository repository = VendorSalesRepository.getInstance();
    private final ResourceLoader<SalesHistoryResponse> sales = new ResourceLoader<>();
    private final MutableLiveData<Boolean> openingDocument = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> documentLinks = new MutableLiveData<>();
    private final MutableLiveData<Event<ActionResult>> documentErrors = new MutableLiveData<>();

    private String search = "";
    @Nullable private Long startUtc;
    @Nullable private Long endUtc;
    @Nullable private String status;
    @Nullable private String material;
    private String minPurity = "";
    private String maxPurity = "";
    private String minValue = "";
    private String maxValue = "";
    private Sort sort = Sort.NEWEST;
    private int page = 1;

    public VendorSalesViewModel() {
        load();
    }

    public LiveData<Resource<SalesHistoryResponse>> getSales() { return sales.asLiveData(); }

    public LiveData<Boolean> isOpeningDocument() { return openingDocument; }

    /** The address of a document to open (viewing or downloading). */
    public LiveData<Event<String>> getDocumentLinks() { return documentLinks; }

    public LiveData<Event<ActionResult>> getDocumentErrors() { return documentErrors; }

    // ---------------------------------------------------------------- current filters

    @NonNull public String getSearch() { return search; }
    @Nullable public Long getStartUtc() { return startUtc; }
    @Nullable public Long getEndUtc() { return endUtc; }
    @Nullable public String getStatus() { return status; }
    @Nullable public String getMaterial() { return material; }
    @NonNull public String getMinPurity() { return minPurity; }
    @NonNull public String getMaxPurity() { return maxPurity; }
    @NonNull public String getMinValue() { return minValue; }
    @NonNull public String getMaxValue() { return maxValue; }
    @NonNull public Sort getSort() { return sort; }
    public int getPage() { return page; }

    /** True when something other than the search box narrows or reorders the list. */
    public boolean hasActiveFilters() {
        return status != null || material != null || !minPurity.isEmpty() || !maxPurity.isEmpty()
                || !minValue.isEmpty() || !maxValue.isEmpty() || sort != Sort.NEWEST;
    }

    // ---------------------------------------------------------------- changes (each goes back to page 1)

    public void setSearch(@Nullable String text) {
        String clean = text == null ? "" : text.trim();
        if (!clean.equals(search)) {
            search = clean;
            page = 1;
            load();
        }
    }

    public void setDateRange(@Nullable Long startUtcMillis, @Nullable Long endUtcMillis) {
        startUtc = startUtcMillis;
        endUtc = endUtcMillis;
        page = 1;
        load();
    }

    public void applyFilters(@Nullable String newStatus, @Nullable String newMaterial, @Nullable String newMinPurity,
                             @Nullable String newMaxPurity, @Nullable String newMinValue, @Nullable String newMaxValue,
                             @NonNull Sort newSort) {
        status = newStatus;
        material = newMaterial;
        minPurity = clean(newMinPurity);
        maxPurity = clean(newMaxPurity);
        minValue = clean(newMinValue);
        maxValue = clean(newMaxValue);
        sort = newSort;
        page = 1;
        load();
    }

    public void clearFilters() {
        applyFilters(null, null, null, null, null, null, Sort.NEWEST);
    }

    public void goToPage(int newPage) {
        if (newPage >= 1 && newPage != page) {
            page = newPage;
            load();
        }
    }

    public void refresh() {
        load();
    }

    private void load() {
        sales.load(repository.getSales(buildQuery()));
    }

    private Map<String, String> buildQuery() {
        Map<String, String> query = new HashMap<>();
        query.put("page", String.valueOf(page));
        query.put("limit", String.valueOf(PAGE_SIZE));
        query.put("sortBy", sort.field);
        query.put("sortOrder", sort.order);
        putIfSet(query, "search", search);
        putIfSet(query, "status", status);
        putIfSet(query, "material", material);
        putIfSet(query, "minPurity", minPurity);
        putIfSet(query, "maxPurity", maxPurity);
        putIfSet(query, "minValue", minValue);
        putIfSet(query, "maxValue", maxValue);
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        day.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (startUtc != null) {
            query.put("startDate", day.format(startUtc));
        }
        if (endUtc != null) {
            query.put("endDate", day.format(endUtc));
        }
        return query;
    }

    private static void putIfSet(Map<String, String> query, String key, @Nullable String value) {
        if (value != null && !value.trim().isEmpty()) {
            query.put(key, value.trim());
        }
    }

    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    // ---------------------------------------------------------------- order documents

    /** Asks the server for the address of the order's document (view or download), then hands it to the screen. */
    public void openDocument(@NonNull String orderId, boolean download) {
        if (Boolean.TRUE.equals(openingDocument.getValue())) {
            return;
        }
        openingDocument.setValue(true);
        LiveData<Resource<OrderDocumentResponse>> call = repository.getDocument(orderId, download);
        call.observeForever(new Observer<Resource<OrderDocumentResponse>>() {
            @Override
            public void onChanged(Resource<OrderDocumentResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                openingDocument.setValue(false);
                String url = result.data == null ? null
                        : (download ? result.data.getDownloadUrl() : result.data.getDocumentUrl());
                if (result.status == Resource.Status.SUCCESS && url != null) {
                    documentLinks.setValue(new Event<>(url));
                } else {
                    documentErrors.setValue(new Event<>(ActionResult.failure(
                            result.status == Resource.Status.SUCCESS ? Resource.error(R.string.error_generic) : result)));
                }
            }
        });
    }
}
