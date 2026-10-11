package com.example.marketplace.ui.vendors;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.CreateVendorRequest;
import com.example.marketplace.data.model.vendor.RegisteredVendor;
import com.example.marketplace.databinding.DialogAddVendorBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Register Vendor" form. It checks the boxes on the phone, then asks the view model to call
 * POST vendors. The list reloads by itself after a success; the server's own message shows inside the dialog.
 */
public class AddVendorDialogFragment extends DialogFragment {

    public static final String TAG = "AddVendorDialog";

    private DialogAddVendorBinding binding;
    private RegisteredVendorsViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(RegisteredVendorsViewModel.class);
        binding = DialogAddVendorBinding.inflate(LayoutInflater.from(requireContext()));

        // A fresh dialog starts clean; after a rotation it keeps the running or failed call.
        if (savedInstanceState == null) {
            viewModel.clearCreateState();
        }

        setUpComplianceDropdown();
        binding.btnVendorCancel.setOnClickListener(v -> dismiss());
        binding.btnVendorSubmit.setOnClickListener(v -> submit());
        viewModel.getCreateState().observe(this, this::render);

        return new MaterialAlertDialogBuilder(requireContext()).setView(binding.getRoot()).create();
    }

    private void setUpComplianceDropdown() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.vendors_compliance_compliant));
        labels.add(getString(R.string.vendors_compliance_pending));
        labels.add(getString(R.string.vendors_compliance_expired));
        binding.ddComplianceStatus.setAdapter(new AllItemsAdapter(requireContext(), labels));
        if (TextUtils.isEmpty(binding.ddComplianceStatus.getText())) {
            binding.ddComplianceStatus.setText(labels.get(0), false);
        }
    }

    /** The value the server expects for the label chosen in the dropdown. */
    @NonNull
    private String complianceValue() {
        String chosen = String.valueOf(binding.ddComplianceStatus.getText());
        if (chosen.equals(getString(R.string.vendors_compliance_pending))) {
            return RegisteredVendor.COMPLIANCE_PENDING;
        }
        if (chosen.equals(getString(R.string.vendors_compliance_expired))) {
            return RegisteredVendor.COMPLIANCE_EXPIRED;
        }
        return RegisteredVendor.COMPLIANCE_COMPLIANT;
    }

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

        boolean valid = true;
        valid &= requireText(binding.tilCompany, company);
        valid &= requireText(binding.tilLicense, license);
        valid &= requireText(binding.tilLocation, location);
        valid &= requireText(binding.tilMaterial, material);
        valid &= requireText(binding.tilManager, manager);

        double volume = 0;
        if (volumeText.isEmpty()) {
            binding.tilVolume.setError(getString(R.string.vendors_error_required));
            valid = false;
        } else {
            try {
                volume = Double.parseDouble(volumeText);
            } catch (NumberFormatException notANumber) {
                volume = 0;
            }
            if (volume <= 0) {
                binding.tilVolume.setError(getString(R.string.vendors_error_volume));
                valid = false;
            } else {
                binding.tilVolume.setError(null);
            }
        }

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.vendors_error_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.vendors_error_email));
            valid = false;
        } else {
            binding.tilEmail.setError(null);
        }

        if (valid) {
            viewModel.createVendor(new CreateVendorRequest(company, license, location, material, volume,
                    certification, complianceValue(), manager, email));
        }
    }

    private void render(@NonNull Resource<MessageResponse> resource) {
        boolean loading = resource.status == Resource.Status.LOADING;
        binding.progressVendor.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVendorSubmit.setEnabled(!loading);
        binding.btnVendorCancel.setEnabled(!loading);
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

    /** Shows or clears the "required" error of one box; returns true when the box is filled. */
    private boolean requireText(@NonNull TextInputLayout layout, @NonNull String text) {
        layout.setError(text.isEmpty() ? getString(R.string.vendors_error_required) : null);
        return !text.isEmpty();
    }

    @NonNull
    private static String value(@NonNull TextInputLayout layout) {
        return layout.getEditText() == null || layout.getEditText().getText() == null
                ? "" : layout.getEditText().getText().toString().trim();
    }

    /** A dropdown list that always shows every choice, even after one has been picked. */
    private static class AllItemsAdapter extends ArrayAdapter<String> {
        private final List<String> all;

        AllItemsAdapter(@NonNull android.content.Context context, @NonNull List<String> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, items);
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
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
