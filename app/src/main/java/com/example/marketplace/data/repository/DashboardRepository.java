package com.example.marketplace.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.marketplace.R;
import com.example.marketplace.data.api.ApiClient;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.data.model.dashboard.ActiveDeal;
import com.example.marketplace.data.model.dashboard.ActiveDealsResponse;
import com.example.marketplace.data.model.dashboard.DashboardSummary;
import com.example.marketplace.data.model.dashboard.MonthlyPurchaseData;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.model.dashboard.ShipmentItem;
import com.example.marketplace.data.model.dashboard.SupplierSummary;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.dashboard.VendorsResponse;
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Loads everything the Home screen shows: the profile, the vendor dashboard, the marketplace feed
 * and the buyer dashboard (spend, active deals, incoming shipments, suppliers).
 * The buyer-dashboard methods also turn the raw server answers into the small UI models.
 */
public class DashboardRepository {

    /** Months shown in the Monthly Purchase chart. */
    public static final int CHART_MONTHS = 6;
    /** The server allows at most 100 rows per page. */
    private static final int PURCHASE_HISTORY_LIMIT = 100;

    private static DashboardRepository instance;

    private final SessionManager sessionManager;

    private DashboardRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static synchronized DashboardRepository getInstance() {
        if (instance == null) {
            instance = new DashboardRepository(SessionManager.getInstance());
        }
        return instance;
    }

    /** The signed-in person (GET auth/me). Also refreshes the saved name and role. */
    public LiveData<Resource<MeResponse>> getProfile() {
        return ApiResults.enqueue(ApiClient.getApiService().getMe(), this::keepProfile);
    }

    private Resource<MeResponse> keepProfile(@NonNull MeResponse body) {
        User user = body.getUser();
        if (user == null) {
            return Resource.error(R.string.error_generic);
        }
        sessionManager.updateUser(user);
        return Resource.success(body);
    }

    /** Vendor dashboard numbers (GET vendor-portal/overview). */
    public LiveData<Resource<VendorOverviewResponse>> getVendorOverview() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorOverview());
    }

    /** How many requests wait for the vendor's decision (GET vendor-portal/order-approvals). */
    public LiveData<Resource<OrderApprovalsResponse>> getVendorApprovals() {
        return ApiResults.enqueue(ApiClient.getApiService().getVendorApprovals());
    }

    /** The user's orders, newest first (GET orders). */
    public LiveData<Resource<OrdersResponse>> getOrders() {
        return ApiResults.enqueue(ApiClient.getApiService().getOrders());
    }

    /** Marketplace listings (GET lots), optionally filtered by search text and one material. */
    public LiveData<Resource<ProductsResponse>> getProducts(@Nullable String search, @Nullable String material) {
        return ApiResults.enqueue(ApiClient.getApiService().getLots(emptyToNull(search), emptyToNull(material)));
    }

    // ------------------------------------------------------------ buyer dashboard

    /** Spending totals and the newest orders of the last six months (GET purchase-history). */
    public LiveData<Resource<PurchaseHistoryResponse>> getPurchaseHistory() {
        return ApiResults.enqueue(ApiClient.getApiService()
                .getPurchaseHistory(PURCHASE_HISTORY_LIMIT, chartStartDate()));
    }

    /** Open negotiations as UI models (GET bids/active-deals). */
    public LiveData<Resource<List<ActiveDeal>>> getActiveDeals() {
        return Transformations.map(ApiResults.enqueue(ApiClient.getApiService().getActiveDeals()), resource -> {
            if (resource.status != Resource.Status.SUCCESS) {
                return resource.<List<ActiveDeal>>withData(null);
            }
            List<ActiveDeal> deals = new ArrayList<>();
            if (resource.data != null) {
                for (ActiveDealsResponse.Deal deal : resource.data.getDeals()) {
                    deals.add(toActiveDeal(deal));
                }
            }
            return Resource.success(deals);
        });
    }

    /** The buyer's suppliers as UI models (GET vendors). */
    public LiveData<Resource<List<SupplierSummary>>> getSuppliers() {
        return Transformations.map(ApiResults.enqueue(ApiClient.getApiService().getVendors()), resource -> {
            if (resource.status != Resource.Status.SUCCESS) {
                return resource.<List<SupplierSummary>>withData(null);
            }
            List<SupplierSummary> suppliers = new ArrayList<>();
            if (resource.data != null) {
                for (VendorsResponse.Vendor vendor : resource.data.getVendors()) {
                    suppliers.add(new SupplierSummary(vendor.getId(), vendor.getDisplayName(), vendor.getImageUrl()));
                }
            }
            return Resource.success(suppliers);
        });
    }

    /**
     * Joins the purchase history and the order list into the top cards and the chart.
     * Loading while either is still loading; an error as soon as either one failed.
     */
    public Resource<DashboardSummary> buildSummary(Resource<PurchaseHistoryResponse> history,
                                                   Resource<OrdersResponse> orders) {
        if (history.status == Resource.Status.ERROR) {
            return history.withData(null);
        }
        if (orders.status == Resource.Status.ERROR) {
            return orders.withData(null);
        }
        if (history.status != Resource.Status.SUCCESS || orders.status != Resource.Status.SUCCESS) {
            return Resource.loading();
        }

        PurchaseHistoryResponse purchases = history.data;
        PurchaseHistoryResponse.Metrics metrics = purchases == null ? null : purchases.getMetrics();
        double totalSpend = metrics == null || metrics.getTotalSpend() == null ? 0 : metrics.getTotalSpend();

        int activeOrders = 0;
        if (orders.data != null) {
            for (Order order : orders.data.getOrders()) {
                if (isActiveOrder(order.getStatus())) {
                    activeOrders++;
                }
            }
        }

        // Six months, oldest first, ending with the current month.
        int[] years = new int[CHART_MONTHS];
        int[] monthNumbers = new int[CHART_MONTHS];
        double[] amounts = new double[CHART_MONTHS];
        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        cursor.add(Calendar.MONTH, -(CHART_MONTHS - 1));
        for (int i = 0; i < CHART_MONTHS; i++) {
            years[i] = cursor.get(Calendar.YEAR);
            monthNumbers[i] = cursor.get(Calendar.MONTH);
            cursor.add(Calendar.MONTH, 1);
        }

        double monthlyVolume = 0;
        if (purchases != null) {
            for (PurchaseHistoryResponse.Row row : purchases.getOrders()) {
                if (row.getStatus() != null && "CANCELLED".equalsIgnoreCase(row.getStatus().trim())) {
                    continue;
                }
                int[] yearMonth = parseYearMonth(row.getDate());
                if (yearMonth == null) {
                    continue;
                }
                for (int i = 0; i < CHART_MONTHS; i++) {
                    if (years[i] == yearMonth[0] && monthNumbers[i] == yearMonth[1]) {
                        amounts[i] += row.getValueINR() == null ? 0 : row.getValueINR();
                        if (i == CHART_MONTHS - 1 && row.getQuantityMT() != null) {
                            monthlyVolume += row.getQuantityMT();
                        }
                    }
                }
            }
        }

        List<MonthlyPurchaseData> months = new ArrayList<>();
        for (int i = 0; i < CHART_MONTHS; i++) {
            months.add(new MonthlyPurchaseData(years[i], monthNumbers[i], amounts[i]));
        }
        return Resource.success(new DashboardSummary(totalSpend, activeOrders, monthlyVolume, months));
    }

    /** Orders that are not finished yet, as shipment rows (newest first, as the server sends them). */
    public Resource<List<ShipmentItem>> buildShipments(Resource<OrdersResponse> orders) {
        if (orders.status != Resource.Status.SUCCESS) {
            return orders.withData(null);
        }
        List<ShipmentItem> shipments = new ArrayList<>();
        if (orders.data != null) {
            for (Order order : orders.data.getOrders()) {
                if (!isActiveOrder(order.getStatus())) {
                    continue;
                }
                String date = order.getExpectedDate() != null ? order.getExpectedDate() : order.getCreatedAt();
                shipments.add(new ShipmentItem(order.getId(), order.getOrderId(), shipmentStage(order.getStatus()),
                        order.getCommodity(), parseIsoDate(date), order.getQuantity(), order.getUnit()));
            }
        }
        return Resource.success(shipments);
    }

    private static ActiveDeal toActiveDeal(ActiveDealsResponse.Deal deal) {
        // "vendor" means the supplier has to answer; anything else is the buyer's move.
        ActiveDeal.Turn turn = "vendor".equalsIgnoreCase(deal.getCurrentTurn())
                ? ActiveDeal.Turn.VENDOR : ActiveDeal.Turn.YOURS;
        boolean sentByYou = "operator".equalsIgnoreCase(deal.getLastActionBy());
        String title = deal.getMaterial() != null ? deal.getMaterial() : deal.getBatchTitle();
        return new ActiveDeal(deal.getId(), deal.getLotId(), title, deal.getQuantityMT(), deal.getPricePerMT(),
                deal.getStatus(), turn, sentByYou, null);
    }

    private static boolean isActiveOrder(@Nullable String status) {
        if (status == null) {
            return true;
        }
        String clean = status.trim();
        return !(clean.equalsIgnoreCase("Delivered") || clean.equalsIgnoreCase("Completed")
                || clean.equalsIgnoreCase("Cancelled"));
    }

    private static ShipmentItem.Stage shipmentStage(@Nullable String status) {
        String clean = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (clean.equals("on the way") || clean.equals("in-transit") || clean.equals("dispatched")) {
            return ShipmentItem.Stage.ON_THE_WAY;
        }
        if (clean.equals("processing")) {
            return ShipmentItem.Stage.PROCESSING;
        }
        return ShipmentItem.Stage.BOOKED;
    }

    /** First day of the oldest chart month as YYYY-MM-DD. */
    private static String chartStartDate() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.add(Calendar.MONTH, -(CHART_MONTHS - 1));
        return String.format(Locale.US, "%04d-%02d-01", start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1);
    }

    /** "2026-10-04" becomes {2026, 9} (month 0-11); null when the text is not a date. */
    @Nullable
    private static int[] parseYearMonth(@Nullable String date) {
        if (date == null || date.length() < 7) {
            return null;
        }
        try {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7)) - 1;
            return (month < 0 || month > 11) ? null : new int[]{year, month};
        } catch (NumberFormatException notADate) {
            return null;
        }
    }

    /** "2026-10-02T10:49:00.000Z" becomes milliseconds since 1970; null when missing or unreadable. */
    @Nullable
    private static Long parseIsoDate(@Nullable String iso) {
        if (iso == null || iso.length() < 19) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = format.parse(iso.substring(0, 19));
            return parsed == null ? null : parsed.getTime();
        } catch (ParseException notADate) {
            return null;
        }
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
