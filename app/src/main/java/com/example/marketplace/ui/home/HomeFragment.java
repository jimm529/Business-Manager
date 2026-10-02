package com.example.marketplace.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ProductsResponse;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.databinding.FragmentHomeBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Home / Dashboard. Vendors see their business numbers, approvals and recent orders;
 * operators see the live marketplace feed with search and material filters.
 * Pull down to reload. Loading, empty and error states are shown for each case.
 */
public class HomeFragment extends Fragment {

    private static final int RECENT_ORDERS_LIMIT = 5;
    private static final long SEARCH_DELAY_MS = 400;

    private FragmentHomeBinding binding;
    private DashboardViewModel viewModel;
    private RecentOrdersAdapter ordersAdapter;
    private ProductsAdapter productsAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    /** True once the main content (dashboard or feed) has been shown for this view. */
    private boolean contentShown;
    /** Categories the chips were last built from, to avoid rebuilding them on every reload. */
    @Nullable
    private List<String> renderedCategories;
    @Nullable
    private String jobTitle;
    @Nullable
    private String companyName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        contentShown = false;
        renderedCategories = null;

        ordersAdapter = new RecentOrdersAdapter();
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(ordersAdapter);
        productsAdapter = new ProductsAdapter();
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProducts.setAdapter(productsAdapter);

        setUpHeader();
        setUpRefresh();
        setUpVendorActions();
        setUpSearch();
        applyRole();

        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getVendorOverview().observe(getViewLifecycleOwner(), this::renderVendorOverview);
        viewModel.getVendorApprovals().observe(getViewLifecycleOwner(), this::renderVendorApprovals);
        viewModel.getRecentOrders().observe(getViewLifecycleOwner(), this::renderRecentOrders);
        viewModel.getProducts().observe(getViewLifecycleOwner(), this::renderProducts);
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnLogout.setOnClickListener(v -> {
            SessionManager.getInstance().clear();
            NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
        });
        renderHeader();
    }

    private void setUpRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        // Only start a refresh when the visible list or dashboard is scrolled to its very top.
        binding.swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            View scroller = viewModel.isVendor() ? binding.vendorContent : binding.rvProducts;
            return scroller.canScrollVertically(-1);
        });
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setUpVendorActions() {
        // These screens are built in later steps.
        View.OnClickListener comingSoon = v ->
                Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
        binding.btnNewListing.setOnClickListener(comingSoon);
        binding.btnOrderApprovals.setOnClickListener(comingSoon);
    }

    private void setUpSearch() {
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getSearchQuery().isEmpty()) {
            binding.etSearch.setText(viewModel.getSearchQuery());
        }
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                // Wait a moment after the last key so every letter does not hit the server.
                if (pendingSearch != null) {
                    handler.removeCallbacks(pendingSearch);
                }
                final String query = s.toString();
                pendingSearch = () -> viewModel.setSearchQuery(query);
                handler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
            }
        });
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (pendingSearch != null) {
                    handler.removeCallbacks(pendingSearch);
                }
                viewModel.setSearchQuery(String.valueOf(v.getText()));
                v.clearFocus();
                return true;
            }
            return false;
        });
    }

    /** Shows the vendor dashboard area or the operator feed area, depending on the role. */
    private void applyRole() {
        boolean vendor = viewModel.isVendor();
        boolean known = viewModel.isRoleKnown();
        binding.tvRoleBadge.setVisibility(known ? View.VISIBLE : View.GONE);
        binding.tvRoleBadge.setText(vendor ? R.string.role_vendor : R.string.role_operator);
        if (!contentShown) {
            binding.vendorContent.setVisibility(View.GONE);
            binding.operatorContent.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------- header

    private void renderProfile(Resource<MeResponse> resource) {
        User user = resource.data == null ? null : resource.data.getUser();
        if (user != null) {
            jobTitle = user.getJobTitle();
        }
        if (resource.status == Resource.Status.SUCCESS) {
            viewModel.onProfileLoaded();
            applyRole();
        } else if (resource.status == Resource.Status.ERROR && !viewModel.isRoleKnown()) {
            // Without a role there is nothing to show; the Retry button reloads the profile.
            showError(ResourceMessages.errorText(requireContext(), resource));
        }
        renderHeader();
    }

    private void renderHeader() {
        SessionManager session = SessionManager.getInstance();
        String name = session.getFullName();
        binding.tvGreeting.setText(TextUtils.isEmpty(name)
                ? getString(R.string.welcome_generic)
                : getString(R.string.welcome_user, name));

        // Vendors show their company; operators show their job title.
        String subtitle = viewModel.isVendor() ? companyName : jobTitle;
        binding.tvSubtitle.setText(subtitle == null ? "" : subtitle);
        binding.tvSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------- vendor

    private void renderVendorOverview(Resource<VendorOverviewResponse> resource) {
        if (!viewModel.isVendor()) {
            return;
        }
        if (!handleMainState(resource, binding.vendorContent)) {
            return;
        }
        VendorOverviewResponse.Data data = resource.data == null ? null : resource.data.getData();
        VendorOverviewResponse.Stats stats = data == null ? null : data.getStats();
        VendorOverviewResponse.VendorInfo vendor = data == null ? null : data.getVendor();

        String noValue = getString(R.string.no_value);
        Integer listings = stats == null ? null : stats.getActiveListings();
        binding.tvActiveListings.setText(listings == null ? noValue : String.valueOf(listings));
        String sales = stats == null ? null : Formatters.currency(stats.getTotalRevenue(), null, 0);
        binding.tvTotalSales.setText(sales == null ? noValue : sales);

        companyName = vendor == null ? null : vendor.getCompanyName();
        renderHeader();
    }

    private void renderVendorApprovals(Resource<OrderApprovalsResponse> resource) {
        if (!viewModel.isVendor()) {
            return;
        }
        // While loading or after a failure the card shows a dash rather than a wrong number.
        binding.tvPendingApprovals.setText(resource.data == null
                ? getString(R.string.no_value)
                : String.valueOf(resource.data.getPendingCount()));
    }

    private void renderRecentOrders(Resource<OrdersResponse> resource) {
        if (!viewModel.isVendor()) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                if (resource.data == null) {
                    showOrdersMessage(getString(R.string.loading));
                }
                break;
            case SUCCESS:
                List<Order> orders = resource.data == null ? new ArrayList<>() : resource.data.getOrders();
                List<Order> recent = orders.subList(0, Math.min(RECENT_ORDERS_LIMIT, orders.size()));
                ordersAdapter.submitList(new ArrayList<>(recent));
                if (recent.isEmpty()) {
                    showOrdersMessage(getString(R.string.empty_recent_orders));
                } else {
                    binding.tvOrdersMessage.setVisibility(View.GONE);
                    binding.rvOrders.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                showOrdersMessage(ResourceMessages.errorText(requireContext(), resource));
                break;
            default:
                break;
        }
    }

    private void showOrdersMessage(String message) {
        ordersAdapter.submitList(new ArrayList<>());
        binding.rvOrders.setVisibility(View.GONE);
        binding.tvOrdersMessage.setText(message);
        binding.tvOrdersMessage.setVisibility(View.VISIBLE);
    }

    // -------------------------------------------------------------- operator

    private void renderProducts(Resource<ProductsResponse> resource) {
        if (viewModel.isVendor()) {
            return;
        }
        if (!handleMainState(resource, binding.operatorContent)) {
            return;
        }
        List<com.example.marketplace.data.model.dashboard.Product> products =
                resource.data == null ? new ArrayList<>() : resource.data.getProducts();
        bindChips(resource.data == null ? new ArrayList<>() : resource.data.getCategories());
        productsAdapter.submitList(new ArrayList<>(products));

        boolean empty = products.isEmpty();
        binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            binding.states.tvEmpty.setText(R.string.empty_products);
            binding.states.emptyGroup.setVisibility(View.VISIBLE);
        }
    }

    private void bindChips(List<String> categories) {
        if (categories.equals(renderedCategories)) {
            return;
        }
        renderedCategories = new ArrayList<>(categories);

        binding.chipGroup.setOnCheckedStateChangeListener(null);
        binding.chipGroup.removeAllViews();
        addChip(null, getString(R.string.category_all));
        for (String category : categories) {
            addChip(category, category);
        }

        // Re-select the chip for the current filter (or "All" when that material no longer exists).
        String selected = viewModel.getSelectedMaterial();
        boolean found = false;
        for (int i = 0; i < binding.chipGroup.getChildCount(); i++) {
            View child = binding.chipGroup.getChildAt(i);
            if (Objects.equals(child.getTag(), selected)) {
                binding.chipGroup.check(child.getId());
                found = true;
                break;
            }
        }
        if (!found) {
            binding.chipGroup.check(binding.chipGroup.getChildAt(0).getId());
        }

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            View chip = group.findViewById(checkedIds.get(0));
            viewModel.setSelectedMaterial(chip == null ? null : (String) chip.getTag());
        });
    }

    private void addChip(@Nullable String material, String label) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.chipGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(material);
        binding.chipGroup.addView(chip);
    }

    // ---------------------------------------------------------- shared states

    /**
     * Shows loading / error for the screen's main call and reports whether fresh data arrived.
     * First load: centre spinner. Reload with data on screen: the pull-to-refresh spinner.
     * Failure: the error view with Retry, or just a message when content is already showing.
     */
    private boolean handleMainState(Resource<?> resource, View content) {
        switch (resource.status) {
            case LOADING:
                boolean reloading = resource.data != null;
                binding.swipeRefresh.setRefreshing(reloading);
                hideStates();
                if (!reloading && !contentShown) {
                    binding.states.progress.setVisibility(View.VISIBLE);
                }
                content.setVisibility(contentShown || reloading ? View.VISIBLE : View.GONE);
                return false;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                content.setVisibility(View.VISIBLE);
                contentShown = true;
                return true;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                String message = ResourceMessages.errorText(requireContext(), resource);
                if (contentShown) {
                    content.setVisibility(View.VISIBLE);
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                } else {
                    content.setVisibility(View.GONE);
                    showError(message);
                }
                return false;
            default:
                return false;
        }
    }

    private void showError(String message) {
        binding.swipeRefresh.setRefreshing(false);
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.tvError.setText(message);
        binding.states.errorGroup.setVisibility(View.VISIBLE);
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        pendingSearch = null;
        binding = null;
    }
}
