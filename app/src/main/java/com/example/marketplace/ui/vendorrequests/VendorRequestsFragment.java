package com.example.marketplace.ui.vendorrequests;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.OrderRequestItem;
import com.example.marketplace.databinding.FragmentVendorRequestsBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * The vendor's Requests tab (Order Requests Ledger): stat tiles, the buyers' requests with Accept
 * and Reject, and an insights panel. Everything is worked out from the vendor's real requests.
 */
public class VendorRequestsFragment extends Fragment {

    private FragmentVendorRequestsBinding binding;
    private VendorRequestsViewModel viewModel;
    private OrderRequestsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VendorRequestsViewModel.class);

        adapter = new OrderRequestsAdapter(new OrderRequestsAdapter.Listener() {
            @Override
            public void onAccept(@NonNull OrderRequestItem item) {
                confirm(item, true);
            }

            @Override
            public void onReject(@NonNull OrderRequestItem item) {
                confirm(item, false);
            }
        });
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRequests.setAdapter(adapter);

        setUpSearch();
        binding.btnFilter.setOnClickListener(this::showFilterMenu);
        binding.btnLoadMore.setOnClickListener(v -> viewModel.showMore());
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getLedger().observe(getViewLifecycleOwner(), this::render);
        viewModel.isBusy().observe(getViewLifecycleOwner(), busy -> adapter.setBusy(Boolean.TRUE.equals(busy)));
        viewModel.getResults().observe(getViewLifecycleOwner(), this::handleResult);
    }

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getQuery().isEmpty()) {
            binding.etSearch.setText(viewModel.getQuery());
        }
        // The requests are already on the phone, so searching can react to every key.
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });
    }

    private void showFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        VendorRequestsViewModel.StatusFilter[] filters = VendorRequestsViewModel.StatusFilter.values();
        for (int i = 0; i < filters.length; i++) {
            menu.getMenu().add(0, i, i, filters[i].label).setCheckable(true)
                    .setChecked(filters[i] == viewModel.getFilter());
        }
        menu.getMenu().setGroupCheckable(0, true, true);
        menu.setOnMenuItemClickListener(item -> {
            viewModel.setFilter(filters[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    // ---------------------------------------------------------------- content

    private void render(Resource<VendorRequestsViewModel.Ledger> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    binding.content.setVisibility(View.GONE);
                    binding.states.progress.setVisibility(View.VISIBLE);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data != null) {
                    binding.content.setVisibility(View.VISIBLE);
                    bind(resource.data);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                binding.content.setVisibility(View.GONE);
                binding.states.tvError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.states.errorGroup.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void hideStates() {
        binding.states.progress.setVisibility(View.GONE);
        binding.states.emptyGroup.setVisibility(View.GONE);
        binding.states.errorGroup.setVisibility(View.GONE);
    }

    private void bind(@NonNull VendorRequestsViewModel.Ledger ledger) {
        bindStats(ledger.stats);
        adapter.submitList(ledger.rows);

        boolean empty = ledger.rows.isEmpty();
        binding.tvListEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.tvListEmpty.setText(ledger.totalRequests == 0
                ? R.string.requests_empty : R.string.requests_empty_filtered);
        binding.btnLoadMore.setVisibility(ledger.hasMore ? View.VISIBLE : View.GONE);

        bindInsights(ledger.stats);
    }

    private void bindStats(@NonNull VendorRequestsViewModel.Stats stats) {
        String noValue = getString(R.string.no_value);
        binding.tvActive.setText(String.valueOf(stats.active));
        binding.tvPending.setText(String.valueOf(stats.pending));

        String quantity = Formatters.number(stats.totalQuantityMT);
        binding.tvQuantity.setText(quantity == null ? noValue
                : getString(R.string.order_quantity, quantity, getString(R.string.requests_unit_mt)));
        binding.tvFocus.setVisibility(stats.focusMaterial == null ? View.GONE : View.VISIBLE);
        binding.tvFocus.setText(stats.focusMaterial == null
                ? null : getString(R.string.requests_stat_focus, stats.focusMaterial));

        String rate = Formatters.currency(stats.averageRate, "INR", 2);
        binding.tvRate.setText(rate == null ? noValue : rate + getString(R.string.requests_unit_kg));
    }

    /** The dark panel: three facts and a closing line, all taken from the open requests. */
    private void bindInsights(@NonNull VendorRequestsViewModel.Stats stats) {
        if (stats.open.isEmpty()) {
            binding.insights.setVisibility(View.GONE);
            return;
        }
        binding.insights.setVisibility(View.VISIBLE);

        String pending = String.valueOf(stats.pending);
        binding.tvInsightPending.setText(bold(getResources().getQuantityString(
                R.plurals.requests_insight_pending, stats.pending, stats.pending), pending));

        OrderRequestItem best = null;
        OrderRequestItem largest = null;
        for (OrderRequestItem item : stats.open) {
            if (item.getOfferedRate() != null
                    && (best == null || item.getOfferedRate() > best.getOfferedRate())) {
                best = item;
            }
            if (item.getQuantityMT() != null
                    && (largest == null || item.getQuantityMT() > largest.getQuantityMT())) {
                largest = item;
            }
        }

        String noValue = getString(R.string.no_value);
        binding.rowInsightBest.setVisibility(best == null ? View.GONE : View.VISIBLE);
        if (best != null) {
            String rate = Formatters.currency(best.getOfferedRate(), "INR", 2) + getString(R.string.requests_unit_kg);
            String buyer = best.getBuyerName() == null ? getString(R.string.requests_unknown_buyer) : best.getBuyerName();
            binding.tvInsightBest.setText(bold(getString(R.string.requests_insight_best, rate, buyer), rate));
        }

        binding.rowInsightLargest.setVisibility(largest == null ? View.GONE : View.VISIBLE);
        if (largest != null) {
            String quantity = Formatters.number(largest.getQuantityMT());
            String material = largest.getMaterial() == null ? noValue : largest.getMaterial();
            binding.tvInsightLargest.setText(bold(getString(R.string.requests_insight_largest,
                    quantity == null ? noValue : quantity, material), quantity == null ? noValue : quantity));
        }

        String average = Formatters.currency(stats.averageRate, "INR", 2);
        binding.tvInsightAverage.setVisibility(average == null ? View.GONE : View.VISIBLE);
        if (average != null) {
            binding.tvInsightAverage.setText(getString(R.string.requests_insight_average,
                    average + getString(R.string.requests_unit_kg), stats.open.size()));
        }
    }

    private static CharSequence bold(@NonNull String full, @NonNull String part) {
        SpannableString text = new SpannableString(full);
        int start = full.indexOf(part);
        if (start >= 0 && !part.isEmpty()) {
            text.setSpan(new StyleSpan(Typeface.BOLD), start, start + part.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

    // ---------------------------------------------------------------- actions

    private void confirm(@NonNull OrderRequestItem item, boolean accept) {
        String id = item.getId();
        if (id == null) {
            return;
        }
        String order = item.getOrderId() == null ? getString(R.string.no_value) : item.getOrderId();
        String buyer = item.getBuyerName() == null ? getString(R.string.requests_unknown_buyer) : item.getBuyerName();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(accept ? R.string.requests_accept_title : R.string.requests_reject_title)
                .setMessage(getString(accept ? R.string.requests_accept_message : R.string.requests_reject_message,
                        order, buyer))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(accept ? R.string.requests_accept : R.string.requests_reject, (dialog, which) -> {
                    if (accept) {
                        viewModel.accept(id);
                    } else {
                        viewModel.reject(id);
                    }
                })
                .show();
    }

    private void handleResult(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
