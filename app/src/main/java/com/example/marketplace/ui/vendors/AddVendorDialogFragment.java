package com.example.marketplace.ui.vendors;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.CreateVendorRequest;
import com.example.marketplace.data.model.vendor.RegisteredVendor;
import com.example.marketplace.databinding.DialogAddVendorBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

/**
 * The "Register Vendor" sheet. It checks the boxes on the phone, then asks the view model to call
 * POST vendors. The list reloads by itself after a success; the server's own message shows inside the sheet.
 * The buttons stay at the bottom while the form scrolls.
 */
public class AddVendorDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "AddVendorDialog";

    /** How much of the screen the sheet fills, so the page behind it still peeks out at the top. */
    private static final float SHEET_HEIGHT_FRACTION = 0.92f;

    private DialogAddVendorBinding binding;
    private RegisteredVendorsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogAddVendorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(RegisteredVendorsViewModel.class);

        // A fresh sheet starts clean; after a rotation it keeps the running or failed call.
        if (savedInstanceState == null) {
            viewModel.clearCreateState();
        }

        setUpComplianceChips();
        binding.btnVendorClose.setOnClickListener(v -> dismiss());
        binding.btnVendorCancel.setOnClickListener(v -> dismiss());
        binding.btnVendorSubmit.setOnClickListener(v -> submit());
        clearErrorWhenTyping(binding.tilCompany);
        clearErrorWhenTyping(binding.tilLicense);
        clearErrorWhenTyping(binding.tilLocation);
        clearErrorWhenTyping(binding.tilMaterial);
        clearErrorWhenTyping(binding.tilVolume);
        clearErrorWhenTyping(binding.tilManager);
        clearErrorWhenTyping(binding.tilEmail);

        viewModel.getCreateState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open tall and fully, and let the keyboard push the form up instead of covering it.
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.getLayoutParams().height = Math.round(
                        getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_FRACTION);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    // ---------------------------------------------------------------- compliance status

    /** Three choices as chips: Compliant (the first choice), Pending, Expired. */
    private void setUpComplianceChips() {
        if (binding.groupCompliance.getChildCount() > 0) {
            return;
        }
        addComplianceChip(R.string.vendors_compliance_compliant, RegisteredVendor.COMPLIANCE_COMPLIANT, true);
        addComplianceChip(R.string.vendors_compliance_pending, RegisteredVendor.COMPLIANCE_PENDING, false);
        addComplianceChip(R.string.vendors_compliance_expired, RegisteredVendor.COMPLIANCE_EXPIRED, false);
    }

    private void addComplianceChip(@StringRes int label, @NonNull String value, boolean checked) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.groupCompliance, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        binding.groupCompliance.addView(chip);
        if (checked) {
            binding.groupCompliance.check(chip.getId());
        }
    }

    /** The value the server expects for the chip that is selected. */
    @NonNull
    private String complianceValue() {
        View chip = binding.groupCompliance.findViewById(binding.groupCompliance.getCheckedChipId());
        Object tag = chip == null ? null : chip.getTag();
        return tag instanceof String ? (String) tag : RegisteredVendor.COMPLIANCE_COMPLIANT;
    }

    // ---------------------------------------------------------------- sending

    private void submit() {
        binding.tvVendorError.setVisibility(View.GONE);
        String company = value(binding.tilCompany);
        String license = value(binding.tilLicense);
        String location = value(binding.tilLocation);
        String material = value(binding.tilMaterial);
        String volumeText = value(binding.tilVolume);
        String certification = value(binding.tilCertification);
        String manager = value(binding.tilManager);
        String email = value(binding.tilEmail);

        // The first box with a problem is the one the form scrolls to.
        TextInputLayout firstBad = null;
        firstBad = firstBad(firstBad, binding.tilCompany, requireText(binding.tilCompany, company));
        firstBad = firstBad(firstBad, binding.tilLicense, requireText(binding.tilLicense, license));
        firstBad = firstBad(firstBad, binding.tilLocation, requireText(binding.tilLocation, location));
        firstBad = firstBad(firstBad, binding.tilMaterial, requireText(binding.tilMaterial, material));

        double volume = 0;
        boolean volumeOk;
        if (volumeText.isEmpty()) {
            binding.tilVolume.setError(getString(R.string.vendors_error_required));
            volumeOk = false;
        } else {
            try {
                volume = Double.parseDouble(volumeText);
            } catch (NumberFormatException notANumber) {
                volume = 0;
            }
            volumeOk = volume > 0;
            binding.tilVolume.setError(volumeOk ? null : getString(R.string.vendors_error_volume));
        }
        firstBad = firstBad(firstBad, binding.tilVolume, volumeOk);

        firstBad = firstBad(firstBad, binding.tilManager, requireText(binding.tilManager, manager));

        boolean emailOk;
        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.vendors_error_required));
            emailOk = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.vendors_error_email));
            emailOk = false;
        } else {
            binding.tilEmail.setError(null);
            emailOk = true;
        }
        firstBad = firstBad(firstBad, binding.tilEmail, emailOk);

        if (firstBad != null) {
            firstBad.requestFocus();
            return;
        }
        viewModel.createVendor(new CreateVendorRequest(company, license, location, material, volume,
                certification, complianceValue(), manager, email));
    }

    private void render(@NonNull Resource<MessageResponse> resource) {
        boolean loading = resource.status == Resource.Status.LOADING;
        binding.progressVendor.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVendorSubmit.setEnabled(!loading);
        binding.btnVendorCancel.setEnabled(!loading);
        binding.btnVendorClose.setEnabled(!loading);
        setCancelable(!loading);

        switch (resource.status) {
            case SUCCESS:
                viewModel.clearCreateState();
                dismissAllowingStateLoss();
                break;
            case ERROR:
                binding.tvVendorError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.tvVendorError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        viewModel.clearCreateState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ---------------------------------------------------------------- helpers

    /** Shows or clears the "required" error of one box; returns true when the box is filled. */
    private boolean requireText(@NonNull TextInputLayout layout, @NonNull String text) {
        layout.setError(text.isEmpty() ? getString(R.string.vendors_error_required) : null);
        return !text.isEmpty();
    }

    @Nullable
    private static TextInputLayout firstBad(@Nullable TextInputLayout first, @NonNull TextInputLayout candidate,
                                            boolean ok) {
        return first != null || ok ? first : candidate;
    }

    private static void clearErrorWhenTyping(@NonNull TextInputLayout layout) {
        if (layout.getEditText() != null) {
            layout.getEditText().addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    layout.setError(null);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) { }
            });
        }
    }

    @NonNull
    private static String value(@NonNull TextInputLayout layout) {
        return layout.getEditText() == null || layout.getEditText().getText() == null
                ? "" : layout.getEditText().getText().toString().trim();
    }
}
