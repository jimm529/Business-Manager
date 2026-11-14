package com.example.marketplace.ui.market;

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
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.MarketProduct;
import com.example.marketplace.databinding.FragmentMarketBinding;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * The Market tab: live listings from the server with search, filter chips, saving a lot and
 * placing an order. Pull down to reload; loading, empty and error states are all shown.
 */
public class MarketFragment extends Fragment {

    private static final long SEARCH_DELAY_MS = 400;

    private FragmentMarketBinding binding;
    private MarketViewModel viewModel;
    private MarketProductsAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MarketViewModel.class);

        adapter = new MarketProductsAdapter(new MarketProductsAdapter.Listener() {
            @Override
            public void onBookmarkClick(@NonNull MarketProduct product) {
                viewModel.toggleBookmark(product);
            }

            @Override
            public void onPlaceOrderClick(@NonNull MarketProduct product) {
                openPlaceOrder(product);
            }
        });
        binding.rvMarket.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMarket.setAdapter(adapter);

        setUpHeader();
        setUpSearch();
        binding.swipeRefresh.setColorSchemeColors(
                MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorPrimary));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.states.btnRetry.setOnClickListener(v -> viewModel.refresh());
        binding.btnFilters.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag(MarketFilterBottomSheet.TAG) == null) {
                new MarketFilterBottomSheet().show(getChildFragmentManager(), MarketFilterBottomSheet.TAG);
            }
        });

        viewModel.getProducts().observe(getViewLifecycleOwner(), this::renderProducts);
        viewModel.getFilters().observe(getViewLifecycleOwner(), this::renderChips);
        viewModel.getActionResults().observe(getViewLifecycleOwner(), this::handleAction);
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));

        binding.tvAvatar.setText(Formatters.initials(requireContext(), SessionManager.getInstance().getFullName()));
        binding.tvAvatar.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add(R.string.logout);
            menu.setOnMenuItemClickListener(item -> {
                SessionManager.getInstance().clear();
                NavHostFragment.findNavController(this).navigate(R.id.action_global_loginFragment);
                return true;
            });
            menu.show();
        });
    }

    private void setUpSearch() {
        // After a rotation the box is empty again: show the search the view model still has.
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

    // ---------------------------------------------------------------- list

    private void renderProducts(Resource<List<MarketProduct>> resource) {
        switch (resource.status) {
            case LOADING:
                hideStates();
                if (resource.data == null) {
                    adapter.submitList(null);
                    binding.states.progress.setVisibility(View.VISIBLE);
                } else {
                    adapter.submitList(resource.data);
                }
                break;
            case SUCCESS:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                List<MarketProduct> items = resource.data;
                adapter.submitList(items);
                if (items == null || items.isEmpty()) {
                    binding.states.tvEmpty.setText(R.string.market_empty);
                    binding.states.emptyGroup.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                binding.swipeRefresh.setRefreshing(false);
                hideStates();
                adapter.submitList(null);
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

    // ---------------------------------------------------------------- chips

    /** One green pill per active filter; the x on a pill removes that filter and reloads. */
    private void renderChips(MarketViewModel.FilterState state) {
        binding.chipGroup.removeAllViews();
        if (state.minWeightKg != null) {
            addChip(getString(R.string.market_chip_weight, Formatters.number(state.minWeightKg.doubleValue())),
                    viewModel::removeWeight);
        }
        if (state.material != null) {
            addChip(state.material, viewModel::removeMaterial);
        }
        if (state.verifiedOnly) {
            addChip(getString(R.string.market_chip_verified), viewModel::removeVerified);
        }
        if (state.regionValue != null) {
            addChip(state.regionLabel == null ? state.regionValue : state.regionLabel, viewModel::removeRegion);
        }
        binding.chipScroll.setVisibility(state.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addChip(String label, Runnable onRemove) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_market_filter_chip, binding.chipGroup, false);
        chip.setText(label);
        chip.setCloseIconContentDescription(getString(R.string.cd_remove_filter));
        chip.setOnCloseIconClickListener(v -> onRemove.run());
        chip.setOnClickListener(v -> onRemove.run());
        binding.chipGroup.addView(chip);
    }

    // ------------------------------------------------------------- actions

    /** Opens the full Place Order page for this lot. */
    private void openPlaceOrder(MarketProduct product) {
        String id = product.getId();
        if (id == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(PlaceOrderFragment.ARG_LOT_ID, id);
        NavHostFragment.findNavController(this).navigate(R.id.action_market_to_placeOrder, args);
    }

    private void handleAction(@Nullable Event<MarketViewModel.ActionResult> event) {
        MarketViewModel.ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result == null) {
            return;
        }
        String message;
        if (result.error != null) {
            message = ResourceMessages.errorText(requireContext(), result.error);
        } else if (result.serverMessage != null && !result.serverMessage.trim().isEmpty()) {
            message = result.serverMessage;
        } else {
            message = getString(result.successRes);
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        pendingSearch = null;
        binding = null;
    }
}
