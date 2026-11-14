package com.example.marketplace.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.MeResponse;
import com.example.marketplace.data.model.auth.User;
import com.example.marketplace.data.model.dashboard.ActiveDeal;
import com.example.marketplace.data.model.dashboard.DashboardSummary;
import com.example.marketplace.data.model.dashboard.OrderApprovalsResponse;
import com.example.marketplace.data.model.dashboard.ShipmentItem;
import com.example.marketplace.data.model.dashboard.SupplierSummary;
import com.example.marketplace.data.model.dashboard.VendorOverviewResponse;
import com.example.marketplace.data.model.order.Order;
import com.example.marketplace.data.model.order.OrdersResponse;
import com.example.marketplace.databinding.FragmentHomeBinding;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Home / Dashboard. Vendors see their business numbers, approvals and recent orders.
 * Buyers (operators) see spend metrics, the monthly purchase chart, active deals, incoming
 * shipments and regular suppliers. Pull down to reload every section at once.
 * Each section shows its own loading, empty and error message.
 */
public class HomeFragment extends Fragment {

    private static final int RECENT_ORDERS_LIMIT = 5;
    private static final int DEALS_LIMIT = 5;
    private static final int SHIPMENTS_LIMIT = 5;
    private static final int SUPPLIERS_LIMIT = 6;
    private static final int SUPPLIER_COLUMNS = 2;

    private FragmentHomeBinding binding;
    private DashboardViewModel viewModel;
    private RecentOrdersAdapter ordersAdapter;
    private ActiveDealsAdapter dealsAdapter;
    private IncomingShipmentsAdapter shipmentsAdapter;
    private SuppliersAdapter suppliersAdapter;

    /** True once the vendor dashboard has been shown for this view. */
    private boolean contentShown;
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

        setUpLists();
        setUpHeaders();
        setUpRefresh();
        setUpActions();
        setUpSearch();
        applyRole();

        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getChatTarget().observe(getViewLifecycleOwner(), this::openChat);
        viewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getVendorOverview().observe(getViewLifecycleOwner(), this::renderVendorOverview);
        viewModel.getVendorApprovals().observe(getViewLifecycleOwner(), this::renderVendorApprovals);
        viewModel.getRecentOrders().observe(getViewLifecycleOwner(), this::renderRecentOrders);
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getDeals().observe(getViewLifecycleOwner(), this::renderDeals);
        viewModel.getShipments().observe(getViewLifecycleOwner(), this::renderShipments);
        viewModel.getSuppliers().observe(getViewLifecycleOwner(), this::renderSuppliers);
    }

    // ---------------------------------------------------------------- set-up

    private void setUpLists() {
        ordersAdapter = new RecentOrdersAdapter();
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(ordersAdapter);

        // Counter opens the chat with the deal's vendor; Decline is built in a later step.
        dealsAdapter = new ActiveDealsAdapter(new ActiveDealsAdapter.Listener() {
            @Override
            public void onCounter(@NonNull ActiveDeal deal) {
                viewModel.openDealChat(deal);
            }

            @Override
            public void onDecline(@NonNull ActiveDeal deal) {
                showComingSoon();
            }
        });
        binding.rvDeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDeals.setAdapter(dealsAdapter);

        shipmentsAdapter = new IncomingShipmentsAdapter(shipment -> openOrders());
        binding.rvShipments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvShipments.setAdapter(shipmentsAdapter);

        suppliersAdapter = new SuppliersAdapter();
        binding.rvSuppliers.setLayoutManager(new GridLayoutManager(requireContext(), SUPPLIER_COLUMNS));
        binding.rvSuppliers.setAdapter(suppliersAdapter);
    }

    private void setUpHeaders() {
        // Vendor header
        binding.btnLogout.setOnClickListener(v -> logout());
        renderHeader();

        // Buyer header
        binding.tvAvatar.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.logout);
            menu.setOnMenuItemClickListener(item -> {
                logout();
                return true;
            });
            menu.show();
        });
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));
        binding.btnSearch.setOnClickListener(v -> {
            binding.operatorContent.smoothScrollTo(0, 0);
            binding.etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void logout() {
        SessionManager.getInstance().clear();
        NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
    }

    private void setUpRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        // Only start a refresh when the visible dashboard is scrolled to its very top.
        binding.swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            View scroller = viewModel.isVendor() ? binding.vendorContent : binding.operatorContent;
            return scroller.canScrollVertically(-1);
        });
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setUpActions() {
        // These screens are built in later steps.
        binding.btnNewListing.setOnClickListener(v -> showComingSoon());
        binding.btnOrderApprovals.setOnClickListener(v -> showComingSoon());
        binding.btnViewAllDeals.setOnClickListener(v -> showComingSoon());
        binding.btnTrackAll.setOnClickListener(v -> openOrders());
    }

    private void setUpSearch() {
        // Searching the marketplace belongs to the Market tab, which is built in a later step.
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus();
                showComingSoon();
                return true;
            }
            return false;
        });
    }

    /** The eye button of a shipment and Track All: the Orders & Tracking page. */
    private void openOrders() {
        NavHostFragment.findNavController(this).navigate(R.id.ordersFragment);
    }

    /** Chat with the deal's vendor; with no chat available, the message list and a note. */
    private void openChat(@Nullable com.example.marketplace.utils.Event<DashboardViewModel.ChatTarget> event) {
        DashboardViewModel.ChatTarget target = event == null ? null : event.getContentIfNotHandled();
        if (target == null) {
            return;
        }
        androidx.navigation.NavController nav = NavHostFragment.findNavController(this);
        if (target.conversationId == null) {
            Snackbar.make(binding.getRoot(), R.string.deal_chat_unavailable, Snackbar.LENGTH_LONG).show();
            nav.navigate(R.id.messagesFragment);
            return;
        }
        Bundle args = new Bundle();
        args.putString(com.example.marketplace.ui.messages.ChatFragment.ARG_CONVERSATION_ID, target.conversationId);
        args.putString(com.example.marketplace.ui.messages.ChatFragment.ARG_PARTICIPANT_NAME, target.vendorName);
        args.putString(com.example.marketplace.ui.messages.ChatFragment.ARG_BATCH_NUMBER, target.lotNumber);
        nav.navigate(R.id.action_home_to_chat, args);
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    /** Shows the vendor or the buyer header and dashboard, depending on the role. */
    private void applyRole() {
        boolean vendor = viewModel.isVendor();
        boolean known = viewModel.isRoleKnown();
        binding.operatorHeader.setVisibility(known && !vendor ? View.VISIBLE : View.GONE);
        binding.vendorHeader.setVisibility(known && !vendor ? View.GONE : View.VISIBLE);
        binding.tvRoleBadge.setVisibility(known ? View.VISIBLE : View.GONE);
        binding.tvRoleBadge.setText(vendor ? R.string.role_vendor : R.string.role_operator);
        binding.tvAvatar.setText(initials(SessionManager.getInstance().getFullName()));

        if (known && !vendor) {
            // The buyer dashboard is always visible; each section shows its own state.
            hideStates();
            binding.vendorContent.setVisibility(View.GONE);
            binding.operatorContent.setVisibility(View.VISIBLE);
        } else if (!contentShown) {
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
        binding.tvAvatar.setText(initials(name));

        // Vendors show their company; operators show their job title.
        String subtitle = viewModel.isVendor() ? companyName : jobTitle;
        binding.tvSubtitle.setText(subtitle == null ? "" : subtitle);
        binding.tvSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
    }

    /** "Jane Doe" becomes "JD", "Jane" becomes "J"; a dash when there is no name. */
    private String initials(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.no_value);
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < parts.length && letters.length() < 2; i++) {
            letters.append(parts[i].substring(0, parts[i].offsetByCodePoints(0, 1)));
        }
        return letters.toString().toUpperCase(Locale.getDefault());
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

    // ----------------------------------------------------------------- buyer

    private void renderSummary(Resource<DashboardSummary> resource) {
        if (viewModel.isVendor()) {
            return;
        }
        String noValue = getString(R.string.no_value);
        DashboardSummary summary = resource.data;

        if (resource.status == Resource.Status.SUCCESS && summary != null) {
            binding.tvTotalSpend.setText(Formatters.compactCurrency(requireContext(), summary.getTotalSpend()));
            binding.tvActiveOrders.setText(String.format(Locale.getDefault(), "%02d", summary.getActiveOrdersCount()));
            String volume = Formatters.number(summary.getMonthlyVolume());
            binding.tvMonthlyVolume.setText(volume == null ? noValue : getString(R.string.metric_volume_value, volume));

            binding.chartMonthly.setData(summary.getMonthlyPurchases());
            if (summary.hasNoPurchases()) {
                showChartMessage(getString(R.string.empty_monthly_purchase), false);
            } else {
                binding.tvChartMessage.setVisibility(View.GONE);
            }
        } else {
            // Loading or failed: dashes rather than wrong numbers, and an empty chart with a message.
            binding.tvTotalSpend.setText(noValue);
            binding.tvActiveOrders.setText(noValue);
            binding.tvMonthlyVolume.setText(noValue);
            binding.chartMonthly.setData(null);
            if (resource.status == Resource.Status.ERROR) {
                showChartMessage(retryText(resource), true);
            } else {
                showChartMessage(getString(R.string.loading), false);
            }
        }
        updateRefreshing();
    }

    private void showChartMessage(String message, boolean retry) {
        binding.tvChartMessage.setText(message);
        binding.tvChartMessage.setVisibility(View.VISIBLE);
        binding.tvChartMessage.setOnClickListener(retry ? v -> viewModel.refresh() : null);
        binding.tvChartMessage.setClickable(retry);
    }

    private void renderDeals(Resource<List<ActiveDeal>> resource) {
        if (viewModel.isVendor()) {
            return;
        }
        List<ActiveDeal> deals = resource.data;
        renderSection(resource, binding.rvDeals, binding.tvDealsMessage, R.string.empty_active_deals,
                deals == null ? null : new ArrayList<>(deals.subList(0, Math.min(DEALS_LIMIT, deals.size()))),
                dealsAdapter::submitList);

        // The bell shows a dot while at least one deal waits for the buyer's answer.
        boolean waiting = false;
        if (deals != null) {
            for (ActiveDeal deal : deals) {
                if (deal.getTurnStatus() == ActiveDeal.Turn.YOURS) {
                    waiting = true;
                    break;
                }
            }
        }
        binding.notificationDot.setVisibility(waiting ? View.VISIBLE : View.GONE);
    }

    private void renderShipments(Resource<List<ShipmentItem>> resource) {
        if (viewModel.isVendor()) {
            return;
        }
        List<ShipmentItem> shipments = resource.data;
        renderSection(resource, binding.rvShipments, binding.tvShipmentsMessage, R.string.empty_shipments,
                shipments == null ? null : new ArrayList<>(shipments.subList(0, Math.min(SHIPMENTS_LIMIT, shipments.size()))),
                shipmentsAdapter::submitList);
    }

    private void renderSuppliers(Resource<List<SupplierSummary>> resource) {
        if (viewModel.isVendor()) {
            return;
        }
        List<SupplierSummary> suppliers = resource.data;
        renderSection(resource, binding.rvSuppliers, binding.tvSuppliersMessage, R.string.empty_suppliers,
                suppliers == null ? null : new ArrayList<>(suppliers.subList(0, Math.min(SUPPLIERS_LIMIT, suppliers.size()))),
                suppliersAdapter::submitList);
    }

    /** Receives the list to show in a section's adapter. */
    private interface ListSink<T> {
        void submit(List<T> items);
    }

    /**
     * One dashboard section: the list when there is data, otherwise a message in its place
     * (loading on the first load, "nothing yet" when empty, or the error with a tap-to-retry).
     * While reloading, the previous list stays on screen.
     */
    private <T> void renderSection(Resource<List<T>> resource, RecyclerView list, TextView message,
                                   @StringRes int emptyRes, @Nullable List<T> items, ListSink<T> sink) {
        switch (resource.status) {
            case SUCCESS:
                sink.submit(items == null ? new ArrayList<>() : items);
                boolean empty = items == null || items.isEmpty();
                list.setVisibility(empty ? View.GONE : View.VISIBLE);
                showSectionMessage(message, empty ? getString(emptyRes) : null, false);
                break;
            case LOADING:
                if (items != null) {
                    sink.submit(items);
                    list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    showSectionMessage(message, null, false);
                } else {
                    sink.submit(new ArrayList<>());
                    list.setVisibility(View.GONE);
                    showSectionMessage(message, getString(R.string.loading), false);
                }
                break;
            case ERROR:
                sink.submit(new ArrayList<>());
                list.setVisibility(View.GONE);
                showSectionMessage(message, retryText(resource), true);
                break;
            default:
                break;
        }
        updateRefreshing();
    }

    private void showSectionMessage(TextView view, @Nullable String text, boolean retry) {
        view.setVisibility(text == null ? View.GONE : View.VISIBLE);
        view.setText(text);
        view.setOnClickListener(retry ? v -> viewModel.refresh() : null);
        view.setClickable(retry);
    }

    private String retryText(Resource<?> resource) {
        return getString(R.string.error_tap_to_retry, ResourceMessages.errorText(requireContext(), resource));
    }

    /** Stops the pull-to-refresh spinner once no buyer section is loading any more. */
    private void updateRefreshing() {
        if (binding == null || viewModel.isVendor()) {
            return;
        }
        if (!isLoading(viewModel.getSummary().getValue()) && !isLoading(viewModel.getDeals().getValue())
                && !isLoading(viewModel.getShipments().getValue()) && !isLoading(viewModel.getSuppliers().getValue())) {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private static boolean isLoading(@Nullable Resource<?> resource) {
        return resource != null && resource.status == Resource.Status.LOADING;
    }

    // ---------------------------------------------------------- vendor states

    /**
     * Shows loading / error for the vendor dashboard's main call and reports whether fresh data arrived.
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
        binding = null;
    }
}
