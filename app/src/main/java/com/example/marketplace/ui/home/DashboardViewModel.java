package com.example.marketplace.ui.home;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.data.repository.DashboardRepository;
import com.example.marketplace.utils.ResourceLoader;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.Roles;
import com.example.marketplace.utils.SessionManager;

/**
 * Everything the Home screen needs. Vendors get their dashboard numbers, approvals and recent
 * orders; operators get the marketplace feed (with search and a material filter).
 * It survives rotation, so nothing is reloaded just because the screen turned.
 */
public class DashboardViewModel extends ViewModel {

    private final DashboardRepository repository = DashboardRepository.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private final ResourceLoader<MeResponse> profile = new ResourceLoader<>();
    private final ResourceLoader<VendorOverviewResponse> vendorOverview = new ResourceLoader<>();
    private final ResourceLoader<OrderApprovalsResponse> vendorApprovals = new ResourceLoader<>();
    private final ResourceLoader<OrdersResponse> recentOrders = new ResourceLoader<>();
    private final ResourceLoader<ProductsResponse> products = new ResourceLoader<>();

    private String searchQuery = "";
    @Nullable
    private String selectedMaterial;
    /** True once the role-specific data has been requested at least once. */
    private boolean roleDataRequested;

    public DashboardViewModel() {
        refresh();
    }

    public LiveData<Resource<MeResponse>> getProfile() { return profile.asLiveData(); }
    public LiveData<Resource<VendorOverviewResponse>> getVendorOverview() { return vendorOverview.asLiveData(); }
    public LiveData<Resource<OrderApprovalsResponse>> getVendorApprovals() { return vendorApprovals.asLiveData(); }
    public LiveData<Resource<OrdersResponse>> getRecentOrders() { return recentOrders.asLiveData(); }
    public LiveData<Resource<ProductsResponse>> getProducts() { return products.asLiveData(); }

    public String getSearchQuery() { return searchQuery; }

    @Nullable
    public String getSelectedMaterial() { return selectedMaterial; }

    /** True when the signed-in user is a vendor; false for operators. */
    public boolean isVendor() {
        return Roles.VENDOR.equalsIgnoreCase(session.getRole());
    }

    /** False only in the rare case that the saved login has no role yet. */
    public boolean isRoleKnown() {
        String role = session.getRole();
        return Roles.VENDOR.equalsIgnoreCase(role) || Roles.OPERATOR.equalsIgnoreCase(role);
    }

    /** Reloads everything (pull-to-refresh and the Retry button). */
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

    public void setSearchQuery(String query) {
        String clean = query == null ? "" : query.trim();
        if (!clean.equals(searchQuery)) {
            searchQuery = clean;
            loadProducts();
        }
    }

    public void setSelectedMaterial(@Nullable String material) {
        boolean same = material == null ? selectedMaterial == null : material.equals(selectedMaterial);
        if (!same) {
            selectedMaterial = material;
            loadProducts();
        }
    }

    private void loadRoleData() {
        roleDataRequested = true;
        if (isVendor()) {
            vendorOverview.load(repository.getVendorOverview());
            vendorApprovals.load(repository.getVendorApprovals());
            recentOrders.load(repository.getOrders());
        } else {
            loadProducts();
        }
    }

    private void loadProducts() {
        if (!isVendor()) {
            products.load(repository.getProducts(searchQuery, selectedMaterial));
        }
    }
}
