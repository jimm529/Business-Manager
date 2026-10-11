package com.example.marketplace.ui.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.settings.TwoFactorSetupResponse;
import com.example.marketplace.databinding.DialogTwoFactorBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Finishes two-factor set-up: shows the key the server created (to type into an authenticator app)
 * and sends the 6-digit code with POST settings/2fa/verify.
 */
public class TwoFactorDialogFragment extends DialogFragment {

    public static final String TAG = "TwoFactorDialog";
    private static final int CODE_LENGTH = 6;

    private DialogTwoFactorBinding binding;
    private SettingsViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(SettingsViewModel.class);
        binding = DialogTwoFactorBinding.inflate(LayoutInflater.from(requireContext()));

        Resource<TwoFactorSetupResponse> setup = viewModel.getTwoFactorSetupState().getValue();
        String secret = setup == null || setup.data == null ? null : setup.data.getManualSecret();
        binding.tvSecret.setText(secret);

        binding.btnCodeCancel.setOnClickListener(v -> cancelSetup());
        binding.btnCodeSubmit.setOnClickListener(v -> submit());
        viewModel.getTwoFactorVerifyState().observe(this, this::render);

        return new MaterialAlertDialogBuilder(requireContext()).setView(binding.getRoot()).create();
    }

    @Override
    public void onStart() {
        super.onStart();
        // The set-up result was lost (e.g. the process restarted): nothing to show.
        Resource<TwoFactorSetupResponse> setup = viewModel.getTwoFactorSetupState().getValue();
        if (setup == null || setup.data == null) {
            dismissAllowingStateLoss();
        }
    }

    private void submit() {
        binding.tilCode.setError(null);
        binding.tvCodeError.setVisibility(View.GONE);
        String code = binding.etCode.getText() == null ? "" : binding.etCode.getText().toString().trim();
        if (code.length() != CODE_LENGTH) {
            binding.tilCode.setError(getString(R.string.tfa_error_code));
            return;
        }
        viewModel.verifyTwoFactor(code);
    }

    private void render(@NonNull Resource<MessageResponse> resource) {
        boolean loading = resource.status == Resource.Status.LOADING;
        binding.progressCode.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnCodeSubmit.setEnabled(!loading);
        binding.btnCodeCancel.setEnabled(!loading);
        setCancelable(!loading);

        switch (resource.status) {
            case SUCCESS:
                viewModel.clearTwoFactor();
                dismissAllowingStateLoss();
                break;
            case ERROR:
                binding.tvCodeError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.tvCodeError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void cancelSetup() {
        viewModel.clearTwoFactor();
        dismiss();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        viewModel.clearTwoFactor();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
