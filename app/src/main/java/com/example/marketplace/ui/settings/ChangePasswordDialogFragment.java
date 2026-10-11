package com.example.marketplace.ui.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.databinding.DialogChangePasswordBinding;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceMessages;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.regex.Pattern;

/**
 * The Change Password dialog. It checks the three fields on the phone, then asks the settings view
 * model to call PUT settings/password. The server's own message (e.g. wrong current password) is shown inside the dialog.
 */
public class ChangePasswordDialogFragment extends DialogFragment {

    public static final String TAG = "ChangePasswordDialog";

    /** The same rule the server applies: 8+ characters, an uppercase letter, a number and a symbol. */
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]");
    private static final int MIN_LENGTH = 8;

    private DialogChangePasswordBinding binding;
    private SettingsViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(SettingsViewModel.class);
        binding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()));

        // A fresh dialog starts clean; after a rotation it keeps the running or failed call.
        if (savedInstanceState == null) {
            viewModel.clearPasswordState(false);
        }

        binding.btnPasswordCancel.setOnClickListener(v -> dismiss());
        binding.btnPasswordSubmit.setOnClickListener(v -> submit());
        viewModel.getPasswordState().observe(this, this::render);

        return new MaterialAlertDialogBuilder(requireContext()).setView(binding.getRoot()).create();
    }

    private void submit() {
        binding.tilCurrent.setError(null);
        binding.tilNew.setError(null);
        binding.tilConfirm.setError(null);
        binding.tvPasswordError.setVisibility(View.GONE);

        String current = text(binding.etCurrent.getText());
        String newPassword = text(binding.etNew.getText());
        String confirm = text(binding.etConfirm.getText());

        boolean valid = true;
        if (TextUtils.isEmpty(current)) {
            binding.tilCurrent.setError(getString(R.string.pw_error_required));
            valid = false;
        }
        if (TextUtils.isEmpty(newPassword)) {
            binding.tilNew.setError(getString(R.string.pw_error_required));
            valid = false;
        } else if (!isStrong(newPassword)) {
            binding.tilNew.setError(getString(R.string.pw_error_weak));
            valid = false;
        }
        if (TextUtils.isEmpty(confirm)) {
            binding.tilConfirm.setError(getString(R.string.pw_error_required));
            valid = false;
        } else if (!confirm.equals(newPassword)) {
            binding.tilConfirm.setError(getString(R.string.pw_error_mismatch));
            valid = false;
        }
        if (valid) {
            viewModel.changePassword(current, newPassword, confirm);
        }
    }

    private void render(@NonNull Resource<MessageResponse> resource) {
        boolean loading = resource.status == Resource.Status.LOADING;
        binding.progressPassword.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPasswordSubmit.setEnabled(!loading);
        binding.btnPasswordCancel.setEnabled(!loading);
        setCancelable(!loading);

        switch (resource.status) {
            case SUCCESS:
                viewModel.clearPasswordState(true);
                dismissAllowingStateLoss();
                break;
            case ERROR:
                binding.tvPasswordError.setText(ResourceMessages.errorText(requireContext(), resource));
                binding.tvPasswordError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        viewModel.clearPasswordState(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static boolean isStrong(@NonNull String password) {
        return password.length() >= MIN_LENGTH
                && UPPERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SYMBOL.matcher(password).find();
    }

    @NonNull
    private static String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
