package com.example.marketplace.ui.market;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.market.MarketplaceResponse;
import com.example.marketplace.databinding.BottomSheetMarketFiltersBinding;
import com.example.marketplace.utils.Formatters;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Objects;

/**
 * The filter sheet: pick a minimum weight, a material, a location and "verified sellers only",
 * then Apply. It shares the Market screen's view model, so Apply updates the list directly.
 */
public class MarketFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MarketFilterBottomSheet";

    private BottomSheetMarketFiltersBinding binding;
    private MarketViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMarketFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(MarketViewModel.class);
        MarketViewModel.FilterState current = viewModel.getFilters().getValue();

        // Weight options come from an array resource, in kilograms.
        int[] weights = getResources().getIntArray(R.array.market_weight_options_kg);
        fillGroup(binding.groupWeight, getString(R.string.market_filter_any),
                current == null ? null : current.minWeightKg, weights.length, index -> weights[index],
                value -> getString(R.string.market_chip_weight, Formatters.number(((Integer) value).doubleValue())));

        List<String> materials = viewModel.getAvailableMaterials();
        fillGroup(binding.groupMaterial, getString(R.string.market_filter_any),
                current == null ? null : current.material, materials.size(), materials::get, value -> (String) value);

        List<MarketplaceResponse.Region> regions = viewModel.getAvailableRegions();
        fillGroup(binding.groupLocation, getString(R.string.market_filter_any),
                current == null ? null : current.regionValue, regions.size(),
                index -> regions.get(index).getValue(),
                value -> {
                    for (MarketplaceResponse.Region region : regions) {
                        if (Objects.equals(region.getValue(), value)) {
                            return region.getLabel();
                        }
                    }
                    return String.valueOf(value);
                });

        binding.switchVerified.setChecked(current != null && current.verifiedOnly);

        binding.btnClear.setOnClickListener(v -> {
            viewModel.setFilters(null, null, null, null, false);
            dismiss();
        });
        binding.btnApply.setOnClickListener(v -> {
            String regionValue = (String) selectedTag(binding.groupLocation);
            String regionLabel = null;
            for (MarketplaceResponse.Region region : regions) {
                if (Objects.equals(region.getValue(), regionValue)) {
                    regionLabel = region.getLabel();
                }
            }
            viewModel.setFilters((String) selectedTag(binding.groupMaterial), regionValue, regionLabel,
                    (Integer) selectedTag(binding.groupWeight), binding.switchVerified.isChecked());
            dismiss();
        });
    }

    /** Gives the value for the option at a position. */
    private interface ValueAt {
        Object get(int index);
    }

    /** Gives the text shown for a value. */
    private interface LabelFor {
        String get(Object value);
    }

    /** Fills a single-choice group: an "Any" chip first, then one chip per option, selecting the current value. */
    private void fillGroup(ChipGroup group, String anyLabel, @Nullable Object selected, int count,
                           ValueAt valueAt, LabelFor labelFor) {
        group.removeAllViews();
        group.setSelectionRequired(true);
        addChip(group, anyLabel, null, selected == null);
        for (int i = 0; i < count; i++) {
            Object value = valueAt.get(i);
            addChip(group, labelFor.get(value), value, Objects.equals(value, selected));
        }
        // A saved filter that is no longer offered by the server falls back to "Any".
        if (group.getCheckedChipId() == View.NO_ID) {
            group.check(group.getChildAt(0).getId());
        }
    }

    private void addChip(ChipGroup group, String label, @Nullable Object value, boolean checked) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        group.addView(chip);
        if (checked) {
            group.check(chip.getId());
        }
    }

    @Nullable
    private static Object selectedTag(ChipGroup group) {
        View chip = group.findViewById(group.getCheckedChipId());
        return chip == null ? null : chip.getTag();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
