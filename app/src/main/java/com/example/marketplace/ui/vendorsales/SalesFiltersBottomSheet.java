package com.example.marketplace.ui.vendorsales;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.vendor.SalesHistoryResponse;
import com.example.marketplace.databinding.BottomSheetSalesFiltersBinding;
import com.example.marketplace.utils.Resource;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The More Filters sheet of Sales History: status, material, purity range, value range and sort order.
 * It shares the screen's view model, so Apply reloads the list right away.
 */
public class SalesFiltersBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SalesFiltersBottomSheet";

    private BottomSheetSalesFiltersBinding binding;
    private VendorSalesViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSalesFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(VendorSalesViewModel.class);

        // The choices come from the server's last answer: only statuses and materials this vendor really has.
        Resource<SalesHistoryResponse> state = viewModel.getSales().getValue();
        SalesHistoryResponse data = state == null ? null : state.data;
        List<String> statuses = data == null ? new ArrayList<>() : data.getStatuses();
        List<String> materials = data == null ? new ArrayList<>() : data.getMaterials();

        fillGroup(binding.groupStatus, viewModel.getStatus(), statuses, s -> s);
        fillGroup(binding.groupMaterial, viewModel.getMaterial(), materials, m -> m);
        binding.labelMaterial.setVisibility(materials.isEmpty() ? View.GONE : View.VISIBLE);
        binding.groupMaterial.setVisibility(materials.isEmpty() ? View.GONE : View.VISIBLE);

        binding.groupSort.removeAllViews();
        for (VendorSalesViewModel.Sort sort : VendorSalesViewModel.Sort.values()) {
            addChip(binding.groupSort, getString(sort.label), sort, sort == viewModel.getSort());
        }

        binding.etMinPurity.setText(viewModel.getMinPurity());
        binding.etMaxPurity.setText(viewModel.getMaxPurity());
        binding.etMinValue.setText(viewModel.getMinValue());
        binding.etMaxValue.setText(viewModel.getMaxValue());

        binding.btnClear.setOnClickListener(v -> {
            viewModel.clearFilters();
            dismiss();
        });
        binding.btnApply.setOnClickListener(v -> {
            VendorSalesViewModel.Sort sort = (VendorSalesViewModel.Sort) selectedTag(binding.groupSort);
            viewModel.applyFilters((String) selectedTag(binding.groupStatus), (String) selectedTag(binding.groupMaterial),
                    text(binding.etMinPurity), text(binding.etMaxPurity), text(binding.etMinValue),
                    text(binding.etMaxValue), sort == null ? VendorSalesViewModel.Sort.NEWEST : sort);
            dismiss();
        });
    }

    private interface LabelFor {
        String get(String value);
    }

    /** Fills a single-choice group: an "Any" chip first, then one chip per server choice. */
    private void fillGroup(ChipGroup group, @Nullable String selected, List<String> values, LabelFor labelFor) {
        group.removeAllViews();
        addChip(group, getString(R.string.market_filter_any), null, selected == null);
        for (String value : values) {
            addChip(group, labelFor.get(value), value, Objects.equals(value, selected));
        }
        // A saved filter that is no longer offered falls back to "Any".
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

    private static String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
