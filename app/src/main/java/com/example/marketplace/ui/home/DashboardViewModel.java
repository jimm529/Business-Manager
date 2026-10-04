package com.example.marketplace.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.dashboard.ActiveDeal;
import com.example.marketplace.data.model.dashboard.DashboardSummary;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.PurchaseHistoryResponse;
import com.example.marketplace.data.model.dashboard.ShipmentItem;
import com.example.marketplace.data.model.dashboard.SupplierSummary;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.data.repository.DashboardRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;
import com.example.marketplace.utils.Roles;
import com.example.marketplace.utils.SessionManager;

import java.util.List;

/**
 * Everything the Home screen needs. Vendors get their dashboard numbers, approvals and recent
 * orders; buyers (operators) get spend metrics, the monthly chart, active deals, incoming
 * shipments and suppliers. Each section has its own loading / success / error state.
 * It survives rotation, so nothing is reloaded just because the screen turned.
 */
public class DashboardViewModel extends ViewModel {

    private final DashboardRepository repository = DashboardRepository.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private final ResourceLoader<MeResponse> profile = new ResourceLoader<>();

    // vendor
    private final ResourceLoader<VendorOverviewResponse> vendorOverview = new ResourceLoader<>();
    private final ResourceLoader<OrderApprovalsResponse> vendorApprovals = new ResourceLoader<>();
    private final ResourceLoader<OrdersResponse> recentOrders = new ResourceLoader<>();

    // buyer
    private final ResourceLoader<DashboardSummary> summary = new ResourceLoader<>();
    private final ResourceLoader<List<ActiveDeal>> deals = new ResourceLoader<>();
    private final ResourceLoader<List<ShipmentItem>> shipments = new ResourceLoader<>();
    private final ResourceLoader<List<SupplierSummary>> suppliers = new ResourceLoader<>();

    /** True once the role-specific data has been requested at least once. */
    private boolean roleDataRequested;

    public DashboardViewModel() {
        refresh();
    }

    public LiveData<Resource<MeResponse>> getProfile() { return profile.asLiveData(); }
    public LiveData<Resource<VendorOverviewResponse>> getVendorOverview() { return vendorOverview.asLiveData(); }
    public LiveData<Resource<OrderApprovalsResponse>> getVendorApprovals() { return vendorApprovals.asLiveData(); }
    public LiveData<Resource<OrdersResponse>> getRecentOrders() { return recentOrders.asLiveData(); }
    public LiveData<Resource<DashboardSummary>> getSummary() { return summary.asLiveData(); }
    public LiveData<Resource<List<ActiveDeal>>> getDeals() { return deals.asLiveData(); }
    public LiveData<Resource<List<ShipmentItem>>> getShipments() { return shipments.asLiveData(); }
    public LiveData<Resource<List<SupplierSummary>>> getSuppliers() { return suppliers.asLiveData(); }

    /** True when the signed-in user is a vendor; false for operators. */
    public boolean isVendor() {
        return Roles.VENDOR.equalsIgnoreCase(session.getRole());
    }

    /** False only in the rare case that the saved login has no role yet. */
    public boolean isRoleKnown() {
        String role = session.getRole();
        return Roles.VENDOR.equalsIgnoreCase(role) || Roles.OPERATOR.equalsIgnoreCase(role);
    }

    /** Reloads every section at once (pull-to-refresh and the retry taps). */
    public void refresh() {
        profile.load(repository.getProfile());
        if (isRoleKnown()) {
            loadRoleData();
        }
    }

    /** Called when the profile arrived: if the role was unknown until now, load the data for it. */
    public void onProfileLoaded() {
        if (!roleDataRequested && isRoleKnown()) {
            loadRoleData();
        }
    }

    private void loadRoleData() {
        roleDataRequested = true;
        if (isVendor()) {
            vendorOverview.load(repository.getVendorOverview());
            vendorApprovals.load(repository.getVendorApprovals());
            recentOrders.load(repository.getOrders());
        } else {
            loadBuyerData();
        }
    }

    private void loadBuyerData() {
        LiveData<Resource<PurchaseHistoryResponse>> history = repository.getPurchaseHistory();
        // One orders call feeds both the "Active Orders" card and the shipments list.
        LiveData<Resource<OrdersResponse>> orders = repository.getOrders();

        MediatorLiveData<Resource<DashboardSummary>> combined = new MediatorLiveData<>();
        Runnable recompute = () -> {
            Resource<PurchaseHistoryResponse> h = history.getValue();
            Resource<OrdersResponse> o = orders.getValue();
            if (h != null && o != null) {
                combined.setValue(repository.buildSummary(h, o));
            }
        };
        combined.addSource(history, r -> recompute.run());
        combined.addSource(orders, r -> recompute.run());

        summary.load(combined);
        shipments.load(Transformations.map(orders, repository::buildShipments));
        deals.load(repository.getActiveDeals());
        suppliers.load(repository.getSuppliers());
    }
}
