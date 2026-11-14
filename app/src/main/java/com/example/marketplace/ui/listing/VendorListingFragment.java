package com.example.marketplace.ui.listing;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marketplace.R;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.listing.ListingTemplatesResponse;
import com.example.marketplace.data.model.listing.LocationsResponse;
import com.example.marketplace.data.model.listing.PublishListingRequest;
import com.example.marketplace.databinding.FragmentVendorListingBinding;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.example.marketplace.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * The vendor's New Listing form (Specs, Logistics, Pricing). The dropdown choices, earlier listings
 * to copy and pickup suggestions come from the server; Publish sends the lot to the marketplace.
 */
public class VendorListingFragment extends Fragment {

    private static final long SEARCH_DELAY_MS = 400;
    /** The server's limit: a purity is a percentage. */
    private static final double MAX_PURITY = 100;

    private FragmentVendorListingBinding binding;
    private ListingFormViewModel viewModel;

    private List<ListingOptionsResponse.Option> materials = new ArrayList<>();
    private List<ListingOptionsResponse.Option> packagings = new ArrayList<>();
    private List<ListingOptionsResponse.Option> regions = new ArrayList<>();
    private List<ListingOptionsResponse.Option> incoterms = new ArrayList<>();
    private boolean optionsReady;

    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVendorListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ListingFormViewModel.class);

        setUpHeader();
        setUpTemplateSearch();
        setUpSteps();
        setUpFields();

        binding.btnOptionsRetry.setOnClickListener(v -> viewModel.loadOptions());
        binding.boxLab.setOnClickListener(v -> comingSoon());
        binding.btnDiscard.setOnClickListener(v -> confirmDiscard());
        binding.btnPublish.setOnClickListener(v -> submit());

        viewModel.getOptions().observe(getViewLifecycleOwner(), this::renderOptions);
        viewModel.getLocations().observe(getViewLifecycleOwner(), this::renderLocations);
        viewModel.getTemplates().observe(getViewLifecycleOwner(), this::renderTemplates);
        viewModel.isPublishing().observe(getViewLifecycleOwner(), publishing -> updatePublishButton());
        viewModel.getPublishResults().observe(getViewLifecycleOwner(), this::handlePublishResult);
    }

    // ---------------------------------------------------------------- set-up

    private void setUpHeader() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnNotifications.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_global_notifications));
        binding.tvAvatar.setText(Formatters.initials(requireContext(), SessionManager.getInstance().getFullName()));
    }

    private void setUpTemplateSearch() {
        binding.etTemplateSearch.addTextChangedListener(new SimpleWatcher(() -> {
            // Wait a moment after the last key so every letter does not hit the server.
            if (pendingSearch != null) {
                handler.removeCallbacks(pendingSearch);
            }
            final String query = String.valueOf(binding.etTemplateSearch.getText());
            pendingSearch = () -> viewModel.searchTemplates(query);
            handler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
        }));
    }

    private void setUpSteps() {
        binding.step1.setOnClickListener(v -> scrollTo(binding.sectionSpecs));
        binding.step2.setOnClickListener(v -> scrollTo(binding.sectionLogistics));
        binding.step3.setOnClickListener(v -> scrollTo(binding.sectionPricing));
        binding.scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldX, oldY) -> updateSteps(scrollY));
        binding.scroll.post(() -> updateSteps(binding.scroll.getScrollY()));
    }

    private void setUpFields() {
        // The certification sentence has a highlighted phrase in the middle.
        String link = getString(R.string.listing_certify_link);
        String sentence = getString(R.string.listing_certify, link);
        SpannableString text = new SpannableString(sentence);
        int start = sentence.indexOf(link);
        if (start >= 0) {
            text.setSpan(new UnderlineSpan(), start, start + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorPrimary)), start, start + link.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.cbCertify.setText(text);
        binding.cbCertify.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                binding.tvCertifyError.setVisibility(View.GONE);
            }
        });

        Runnable recalculate = this::updateTotal;
        binding.etPrice.addTextChangedListener(new SimpleWatcher(recalculate));
        binding.etQuantity.addTextChangedListener(new SimpleWatcher(recalculate));
        updateTotal();

        binding.etDate.setOnClickListener(v -> pickDate());
        binding.tilDate.setEndIconOnClickListener(v -> pickDate());
        showDate(viewModel.getAvailabilityUtcMillis());

        clearErrorOnChange(binding.etPurity, binding.tilPurity);
        clearErrorOnChange(binding.etQuantity, binding.tilQuantity);
        clearErrorOnChange(binding.etPrice, binding.tilPrice);
        clearErrorOnChange(binding.actPickup, binding.tilPickup);
    }

    private void clearErrorOnChange(android.widget.EditText field, com.google.android.material.textfield.TextInputLayout layout) {
        field.addTextChangedListener(new SimpleWatcher(() -> layout.setError(null)));
    }

    // ---------------------------------------------------------------- choices from the server

    private void renderOptions(Resource<ListingOptionsResponse> resource) {
        switch (resource.status) {
            case SUCCESS:
                optionsReady = resource.data != null;
                binding.optionsError.setVisibility(View.GONE);
                if (resource.data != null) {
                    materials = resource.data.getMaterialTypes();
                    packagings = resource.data.getPackagingTypes();
                    regions = resource.data.getRegions();
                    incoterms = resource.data.getIncoterms();
                    bindDropdown(binding.actMaterial, binding.tilMaterial, materials,
                            viewModel.getMaterialValue(), this::onMaterialPicked);
                    bindDropdown(binding.actPackaging, binding.tilPackaging, packagings,
                            viewModel.getPackagingValue(), viewModel::setPackagingValue);
                    bindDropdown(binding.actRegion, binding.tilRegion, regions,
                            viewModel.getRegionValue(), viewModel::setRegionValue);
                    bindDropdown(binding.actShipping, binding.tilShipping, incoterms,
                            viewModel.getIncotermValue(), viewModel::setIncotermValue);
                }
                break;
            case ERROR:
                optionsReady = false;
                binding.optionsError.setVisibility(View.VISIBLE);
                binding.tvOptionsError.setText(getString(R.string.listing_options_error) + " "
                        + ResourceMessages.errorText(requireContext(), resource));
                break;
            default:
                // Loading: keep what is there.
                break;
        }
        updatePublishButton();
    }

    /** Fills one dropdown with the server's choices and shows the one already chosen (e.g. after rotation). */
    private void bindDropdown(AutoCompleteTextView field, com.google.android.material.textfield.TextInputLayout layout,
                              List<ListingOptionsResponse.Option> options, @Nullable String selectedValue,
                              Consumer<String> onPicked) {
        List<String> labels = new ArrayList<>();
        for (ListingOptionsResponse.Option option : options) {
            labels.add(option.getLabel());
        }
        field.setAdapter(new AllChoicesAdapter(requireContext(), labels));
        field.setOnItemClickListener((parent, v, position, id) -> {
            onPicked.accept(options.get(position).getValue());
            layout.setError(null);
        });
        ListingOptionsResponse.Option selected = find(options, selectedValue);
        field.setText(selected == null ? "" : selected.getLabel(), false);
    }

    private void onMaterialPicked(@Nullable String value) {
        viewModel.setMaterialValue(value);
        // A usual purity for the material is suggested, but only when the field is still empty.
        ListingOptionsResponse.Option option = find(materials, value);
        if (option != null && option.getDefaultPurity() != null
                && TextUtils.isEmpty(binding.etPurity.getText())) {
            binding.etPurity.setText(plain(option.getDefaultPurity()));
        }
    }

    private void renderLocations(Resource<LocationsResponse> resource) {
        if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
            binding.actPickup.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, resource.data.getLocations()));
            binding.actPickup.setThreshold(1);
        }
    }

    // ---------------------------------------------------------------- earlier listings

    private void renderTemplates(Resource<ListingTemplatesResponse> resource) {
        binding.chipTemplates.removeAllViews();
        binding.tvTemplatesMessage.setVisibility(View.GONE);
        if (resource.status == Resource.Status.ERROR) {
            binding.tvTemplatesMessage.setText(ResourceMessages.errorText(requireContext(), resource));
            binding.tvTemplatesMessage.setVisibility(View.VISIBLE);
            return;
        }
        if (resource.status != Resource.Status.SUCCESS || resource.data == null) {
            return;
        }
        List<ListingTemplatesResponse.Template> templates = resource.data.getTemplates();
        if (templates.isEmpty()) {
            binding.tvTemplatesMessage.setText(R.string.listing_templates_empty);
            binding.tvTemplatesMessage.setVisibility(View.VISIBLE);
            return;
        }
        for (ListingTemplatesResponse.Template template : templates) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_template_chip, binding.chipTemplates, false);
            String material = template.getMaterialType() == null ? getString(R.string.no_value) : template.getMaterialType();
            String purity = Formatters.number(template.getPurityGrade());
            chip.setText(purity == null ? material : getString(R.string.listing_template_label, material, purity));
            chip.setOnClickListener(v -> applyTemplate(template));
            binding.chipTemplates.addView(chip);
        }
    }

    /** Copies an earlier listing into the form. Region and date stay as they are: they are not part of a template. */
    private void applyTemplate(@NonNull ListingTemplatesResponse.Template template) {
        ListingOptionsResponse.Option material = find(materials, template.getMaterialType());
        viewModel.setMaterialValue(material == null ? null : material.getValue());
        binding.actMaterial.setText(material == null ? "" : material.getLabel(), false);

        binding.etPurity.setText(plain(template.getPurityGrade()));
        binding.etQuantity.setText(plain(template.getQuantityMT()));
        binding.etPrice.setText(plain(template.getAskingPricePerKg()));

        ListingOptionsResponse.Option packaging = find(packagings, template.getPackagingType());
        viewModel.setPackagingValue(packaging == null ? null : packaging.getValue());
        binding.actPackaging.setText(packaging == null ? "" : packaging.getLabel(), false);

        ListingOptionsResponse.Option shipping = find(incoterms, template.getShippingTerms());
        viewModel.setIncotermValue(shipping == null ? null : shipping.getValue());
        binding.actShipping.setText(shipping == null ? "" : shipping.getLabel(), false);

        binding.actPickup.setText(template.getPickupLocation() == null ? "" : template.getPickupLocation());
        binding.actPickup.dismissDropDown();
    }

    // ---------------------------------------------------------------- steps, date, total

    private void scrollTo(@NonNull View section) {
        binding.scroll.smoothScrollTo(0, Math.max(0, section.getTop() - dp(8)));
    }

    /** Highlights the step whose section is at the top of the screen. */
    private void updateSteps(int scrollY) {
        if (binding == null) {
            return;
        }
        int line = scrollY + dp(80);
        int active = 1;
        if (line >= binding.sectionPricing.getTop()) {
            active = 3;
        } else if (line >= binding.sectionLogistics.getTop()) {
            active = 2;
        }
        styleStep(binding.tvStep1, binding.tvStep1Label, active == 1);
        styleStep(binding.tvStep2, binding.tvStep2Label, active == 2);
        styleStep(binding.tvStep3, binding.tvStep3Label, active == 3);
    }

    private void styleStep(TextView circle, TextView label, boolean active) {
        circle.setBackgroundResource(active ? R.drawable.bg_step_active : R.drawable.bg_step_idle);
        int color = MaterialColors.getColor(binding.getRoot(), active
                ? com.google.android.material.R.attr.colorSecondary
                : com.google.android.material.R.attr.colorOnSurfaceVariant);
        circle.setTextColor(color);
        label.setTextColor(color);
    }

    private void pickDate() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraints);
        Long current = viewModel.getAvailabilityUtcMillis();
        if (current != null) {
            builder.setSelection(current);
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            viewModel.setAvailabilityUtcMillis(selection);
            binding.tilDate.setError(null);
            showDate(selection);
        });
        picker.show(getChildFragmentManager(), "listing_date");
    }

    /** The picker works in UTC, so the text is formatted in UTC too or the day could shift. */
    private void showDate(@Nullable Long utcMillis) {
        if (utcMillis == null) {
            binding.etDate.setText("");
            return;
        }
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        binding.etDate.setText(format.format(utcMillis));
    }

    private void updateTotal() {
        if (binding == null) {
            return;
        }
        Double price = parse(binding.etPrice.getText());
        Double quantity = parse(binding.etQuantity.getText());
        if (price == null || quantity == null) {
            binding.tvTotal.setText(R.string.no_value);
            return;
        }
        String total = Formatters.currency(price * quantity * 1000d, "INR", 2);
        binding.tvTotal.setText(total == null ? getString(R.string.no_value) : total);
    }

    // ---------------------------------------------------------------- publish and discard

    private void updatePublishButton() {
        if (binding == null) {
            return;
        }
        boolean publishing = Boolean.TRUE.equals(viewModel.isPublishing().getValue());
        binding.btnPublish.setEnabled(optionsReady && !publishing);
        binding.btnDiscard.setEnabled(!publishing);
    }

    private void submit() {
        Double purity = parse(binding.etPurity.getText());
        Double quantity = parse(binding.etQuantity.getText());
        Double price = parse(binding.etPrice.getText());
        String pickup = binding.actPickup.getText() == null ? "" : binding.actPickup.getText().toString().trim();
        Long date = viewModel.getAvailabilityUtcMillis();

        View firstBad = null;
        firstBad = mark(binding.tilMaterial, viewModel.getMaterialValue() == null,
                R.string.listing_error_material, firstBad, binding.sectionSpecs);
        firstBad = mark(binding.tilPurity, purity == null || purity < 0 || purity > MAX_PURITY,
                R.string.listing_error_purity, firstBad, binding.sectionSpecs);
        firstBad = mark(binding.tilQuantity, quantity == null || quantity <= 0,
                R.string.listing_error_quantity, firstBad, binding.sectionSpecs);
        firstBad = mark(binding.tilPackaging, viewModel.getPackagingValue() == null,
                R.string.listing_error_packaging, firstBad, binding.sectionSpecs);
        firstBad = mark(binding.tilPickup, pickup.isEmpty(),
                R.string.listing_error_pickup, firstBad, binding.sectionLogistics);
        firstBad = mark(binding.tilRegion, viewModel.getRegionValue() == null,
                R.string.listing_error_region, firstBad, binding.sectionLogistics);
        firstBad = mark(binding.tilDate, date == null, R.string.listing_error_date, firstBad, binding.sectionLogistics);
        firstBad = mark(binding.tilShipping, viewModel.getIncotermValue() == null,
                R.string.listing_error_shipping, firstBad, binding.sectionLogistics);
        firstBad = mark(binding.tilPrice, price == null || price <= 0,
                R.string.listing_error_price, firstBad, binding.sectionPricing);
        boolean certified = binding.cbCertify.isChecked();
        binding.tvCertifyError.setVisibility(certified ? View.GONE : View.VISIBLE);
        if (!certified && firstBad == null) {
            firstBad = binding.sectionPricing;
        }

        if (firstBad != null) {
            scrollTo(firstBad);
            return;
        }
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        day.setTimeZone(TimeZone.getTimeZone("UTC"));
        viewModel.publish(new PublishListingRequest(viewModel.getMaterialValue(), purity, quantity,
                viewModel.getPackagingValue(), pickup, viewModel.getRegionValue(), day.format(date),
                viewModel.getIncotermValue(), price, true));
    }

    /** Shows or clears one field's error; remembers the first section that has a problem. */
    @Nullable
    private View mark(com.google.android.material.textfield.TextInputLayout layout, boolean bad,
                      int message, @Nullable View firstBad, View section) {
        layout.setError(bad ? getString(message) : null);
        return bad && firstBad == null ? section : firstBad;
    }

    private void handlePublishResult(@Nullable Event<ActionResult> event) {
        ActionResult result = event == null ? null : event.getContentIfNotHandled();
        if (result == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), result.text(requireContext()), Snackbar.LENGTH_LONG).show();
        if (!result.isError()) {
            clearForm();
        }
    }

    private void confirmDiscard() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.listing_discard_title)
                .setMessage(R.string.listing_discard_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.listing_discard, (dialog, which) -> {
                    clearForm();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .show();
    }

    private void clearForm() {
        viewModel.clearSelections();
        binding.actMaterial.setText("", false);
        binding.actPackaging.setText("", false);
        binding.actRegion.setText("", false);
        binding.actShipping.setText("", false);
        binding.etPurity.setText("");
        binding.etQuantity.setText("");
        binding.etPrice.setText("");
        binding.actPickup.setText("");
        binding.etDate.setText("");
        binding.cbCertify.setChecked(false);
        binding.tvCertifyError.setVisibility(View.GONE);
        binding.tilMaterial.setError(null);
        binding.tilPackaging.setError(null);
        binding.tilRegion.setError(null);
        binding.tilShipping.setError(null);
        binding.tilDate.setError(null);
        binding.scroll.scrollTo(0, 0);
    }

    // ---------------------------------------------------------------- helpers

    @Nullable
    private static ListingOptionsResponse.Option find(List<ListingOptionsResponse.Option> options, @Nullable String value) {
        if (value == null) {
            return null;
        }
        for (ListingOptionsResponse.Option option : options) {
            if (option.getValue() != null && option.getValue().equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        return null;
    }

    /** Reads a typed number; a comma counts as the decimal point. Null when empty or not a number. */
    @Nullable
    private static Double parse(@Nullable CharSequence text) {
        if (text == null || text.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.toString().trim().replace(',', '.'));
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** 99.97 -> "99.97", 25.0 -> "25": no grouping, no trailing zeros, so it can be edited. */
    private static String plain(@Nullable Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void comingSoon() {
        Snackbar.make(binding.getRoot(), R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        pendingSearch = null;
        binding = null;
    }

    /** A text watcher that only cares that the text changed. */
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }

    /**
     * A dropdown list that always shows every choice. The default one hides the other choices once
     * one is selected, which makes changing your mind confusing.
     */
    private static class AllChoicesAdapter extends ArrayAdapter<String> {
        private final List<String> all;

        AllChoicesAdapter(@NonNull Context context, @NonNull List<String> items) {
            super(context, android.R.layout.simple_list_item_1, new ArrayList<>(items));
            this.all = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = all;
                    results.count = all.size();
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    addAll((List<String>) results.values);
                    notifyDataSetChanged();
                }
            };
        }
    }
}
