package com.example.marketplace.ui.vendorsales;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.SalesHistoryResponse;
import com.example.marketplace.data.model.vendor.SalesOrder;
import com.example.marketplace.databinding.FragmentVendorSalesBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * The vendor's Sales History: search, a date range, More Filters, one card per order with
 * document buttons, and page buttons. Every change asks the server for the matching page.
 */
public class VendorSalesFragment extends Fragment {

    private static final long SEARCH_DELAY_MS = 400;
    /** With this many pages or fewer, every page number is shown. */
    private static final int SHOW_ALL_PAGES_UP_TO = 5;

    private FragmentVendorSalesBinding binding;
    private VendorSalesViewModel viewModel;
    private SalesOrdersAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorSalesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VendorSalesViewModel.class);

        adapter = new SalesOrdersAdapter(new SalesOrdersAdapter.Listener() {
            @Override
            public void onViewDocument(@NonNull SalesOrder order) {
                openDocument(order, false);
            }

            @Override
            public void onDownloadDocument(@NonNull SalesOrder order) {
                openDocument(order, true);
            }
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        setUpSearch();
        binding.btnDateRange.setOnClickListener(v -> pickDateRange());
        binding.btnClearDates.setOnClickListener(v -> viewModel.setDateRange(null, null));
        binding.btnMoreFilters.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(SalesFiltersBottomSheet.TAG) == null) {
                new SalesFiltersBottomSheet().show(getChildFragmentManager(), SalesFiltersBottomSheet.TAG);
            }
        });
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());

        showDateRange();
        viewModel.getSales().observe(getViewLifecycleOwner(), this::render);
        viewModel.isOpeningDocument().observe(getViewLifecycleOwner(), opening ->
                adapter.setBusy(Boolean.TRUE.equals(opening)));
        viewModel.getDocumentLinks().observe(getViewLifecycleOwner(), this::openLink);
        viewModel.getDocumentErrors().observe(getViewLifecycleOwner(), this::showError);
    }

    // ---------------------------------------------------------------- search and dates

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
        if (TextUtils.isEmpty(binding.etSearch.getText()) && !viewModel.getSearch().isEmpty()) {
            binding.etSearch.setText(viewModel.getSearch());
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
                pendingSearch = () -> viewModel.setSearch(query);
                handler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
            }
        });
    }

    private void pickDateRange() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        if (viewModel.getStartUtc() != null && viewModel.getEndUtc() != null) {
            builder.setSelection(new Pair<>(viewModel.getStartUtc(), viewModel.getEndUtc()));
        }
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                viewModel.setDateRange(selection.first, selection.second);
                showDateRange();
            }
        });
        picker.show(getChildFragmentManager(), "sales_dates");
    }

    /** The picker works in UTC, so the text is formatted in UTC too or a day could shift. */
    private void showDateRange() {
        Long start = viewModel.getStartUtc();
        Long end = viewModel.getEndUtc();
        if (start == null || end == null) {
            binding.tvDateRange.setText(R.string.sales_all_dates);
            binding.btnClearDates.setVisibility(View.GONE);
            return;
        }
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        binding.tvDateRange.setText(getString(R.string.sales_date_range_value, format.format(start), format.format(end)));
        binding.btnClearDates.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- list

    private void render(Resource<SalesHistoryResponse> resource) {
        showDateRange();
        switch (resource.status) {
            case LOADING:
                binding.states.progress.setVisibility(resource.data == null ? View.VISIBLE : View.GONE);
                binding.states.errorGroup.setVisibility(View.GONE);
                if (resource.data != null) {
                    bind(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                if (resource.data != null) {
                    bind(resource.data);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
                binding.pagination.setVisibility(View.GONE);
                binding.tvEmpty.setVisibility(View.GONE);
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

    private void bind(@NonNull SalesHistoryResponse data) {
        List<SalesOrder> orders = data.getOrders();
        adapter.submitList(orders);

        boolean empty = orders.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        boolean narrowed = !viewModel.getSearch().isEmpty() || viewModel.hasActiveFilters()
                || viewModel.getStartUtc() != null;
        binding.tvEmpty.setText(narrowed ? R.string.sales_empty_filtered : R.string.sales_empty);

        binding.pagination.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            binding.tvShowing.setText(getString(R.string.sales_showing,
                    data.getShowingStart(), data.getShowingEnd(), data.getTotalOrders()));
            buildPager(data.getPage(), data.getTotalPages());
        }
    }

    // ---------------------------------------------------------------- page buttons

    /** "< 1 2 3 ... 41 >": the first pages, the current one with its neighbours, and the last page. */
    private void buildPager(int current, int total) {
        binding.pager.removeAllViews();
        binding.pager.addView(arrowButton(R.drawable.ic_arrow_back, R.string.cd_previous_page,
                current > 1 ? current - 1 : -1));

        for (int page : pagesToShow(current, total)) {
            if (page == 0) {
                TextView dots = new TextView(requireContext());
                dots.setText(R.string.sales_ellipsis);
                dots.setTextColor(MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
                dots.setPadding(dp(10), 0, dp(10), 0);
                dots.setGravity(android.view.Gravity.CENTER);
                binding.pager.addView(dots, new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
            } else {
                binding.pager.addView(pageButton(page, page == current));
            }
        }

        binding.pager.addView(arrowButton(R.drawable.ic_chevron_right, R.string.cd_next_page,
                current < total ? current + 1 : -1));
    }

    /** Page numbers to show; 0 stands for "...". */
    private static List<Integer> pagesToShow(int current, int total) {
        List<Integer> pages = new ArrayList<>();
        if (total <= SHOW_ALL_PAGES_UP_TO) {
            for (int i = 1; i <= total; i++) {
                pages.add(i);
            }
        } else if (current <= 3) {
            pages.add(1);
            pages.add(2);
            pages.add(3);
            pages.add(0);
            pages.add(total);
        } else if (current >= total - 2) {
            pages.add(1);
            pages.add(0);
            pages.add(total - 2);
            pages.add(total - 1);
            pages.add(total);
        } else {
            pages.add(1);
            pages.add(0);
            pages.add(current - 1);
            pages.add(current);
            pages.add(current + 1);
            pages.add(0);
            pages.add(total);
        }
        return pages;
    }

    private MaterialButton pageButton(int page, boolean selected) {
        MaterialButton button = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(String.valueOf(page));
        button.setContentDescription(getString(R.string.cd_page, page));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinWidth(dp(46));
        button.setMinimumWidth(dp(46));
        button.setPadding(0, 0, 0, 0);
        button.setCornerRadius(dp(8));
        if (selected) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                    binding.getRoot(), com.google.android.material.R.attr.colorSecondary)));
            button.setTextColor(MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorOnSecondary));
            button.setStrokeWidth(0);
        } else {
            button.setTextColor(MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorOnSurface));
            button.setOnClickListener(v -> viewModel.goToPage(page));
        }
        button.setLayoutParams(buttonParams());
        return button;
    }

    /** Previous / next. A target of -1 means there is nothing to go to, so the button is dimmed. */
    private MaterialButton arrowButton(int icon, int description, int targetPage) {
        MaterialButton button = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setIconResource(icon);
        button.setIconTint(android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                binding.getRoot(), com.google.android.material.R.attr.colorOnSurface)));
        button.setIconPadding(0);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setContentDescription(getString(description));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinWidth(dp(46));
        button.setMinimumWidth(dp(46));
        button.setPadding(dp(11), 0, dp(11), 0);
        button.setCornerRadius(dp(8));
        button.setEnabled(targetPage > 0);
        button.setOnClickListener(v -> viewModel.goToPage(targetPage));
        button.setLayoutParams(buttonParams());
        return button;
    }

    private android.widget.LinearLayout.LayoutParams buttonParams() {
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        params.setMarginStart(dp(3));
        params.setMarginEnd(dp(3));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------------------------------------------------------------- order documents

    private void openDocument(@NonNull SalesOrder order, boolean download) {
        if (order.getId() != null) {
            viewModel.openDocument(order.getId(), download);
        }
    }

    private void openLink(@Nullable Event<String> event) {
        String url = event == null ? null : event.getContentIfNotHandled();
        if (url == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException | SecurityException noApp) {
            Snackbar.make(binding.getRoot(), R.string.sales_doc_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showError(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result != null) {
            Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        pendingSearch = null;
        binding = null;
    }
}
