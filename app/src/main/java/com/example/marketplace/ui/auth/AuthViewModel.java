package com.example.marketplace.ui.auth;

import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.auth.AuthResponse;
import com.example.marketplace.data.model.auth.LoginRequest;
import com.example.marketplace.data.model.auth.RegisterRequest;
import com.example.marketplace.data.repository.AuthRepository;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.Roles;

import java.util.regex.Pattern;

/**
 * Holds everything the Login and Register screens need so it survives rotation:
 * the chosen portal, validation errors and the state of the network call.
 */
public class AuthViewModel extends ViewModel {

    // Same rule the backend enforces: 8+ characters with an uppercase letter, a digit and a symbol.
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9\\s]");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthRepository repository = AuthRepository.getInstance();

    private final MutableLiveData<String> selectedRole = new MutableLiveData<>(Roles.OPERATOR);
    private final MutableLiveData<FormErrors> formErrors = new MutableLiveData<>(new FormErrors());
    private final MediatorLiveData<Resource<AuthResponse>> authResult =
            new MediatorLiveData<>();

    @Nullable
    private LiveData<Resource<AuthResponse>> currentSource;

    public AuthViewModel() {
        authResult.setValue(Resource.idle());
    }

    public LiveData<String> getSelectedRole() {
        return selectedRole;
    }

    public void selectRole(String role) {
        selectedRole.setValue(role);
    }

    public LiveData<FormErrors> getFormErrors() {
        return formErrors;
    }

    public LiveData<Resource<AuthResponse>> getAuthResult() {
        return authResult;
    }

    /** Call after the UI has shown a success/error, so it is not shown again after rotation. */
    public void consumeAuthResult() {
        authResult.setValue(Resource.idle());
    }

    public void login(@Nullable String email, @Nullable String password) {
        if (isBusy()) {
            return;
        }
        String cleanEmail = trim(email);
        FormErrors errors = new FormErrors();
        errors.email = validateEmail(cleanEmail);
        // Existing accounts may predate the password rule, so login only checks it is not empty.
        errors.password = (password == null || password.isEmpty()) ? R.string.error_password_required : null;
        formErrors.setValue(errors);
        if (errors.hasErrors()) {
            return;
        }
        submit(repository.login(new LoginRequest(cleanEmail, password, currentRole())));
    }

    public void register(@Nullable String fullName, @Nullable String jobTitle, @Nullable String email,
                         @Nullable String password, @Nullable String confirmPassword) {
        if (isBusy()) {
            return;
        }
        String cleanName = trim(fullName);
        String cleanJob = trim(jobTitle);
        String cleanEmail = trim(email);
        String pass = password == null ? "" : password;
        String confirm = confirmPassword == null ? "" : confirmPassword;

        FormErrors errors = new FormErrors();
        errors.fullName = cleanName.isEmpty() ? R.string.error_name_required : null;
        errors.jobTitle = cleanJob.isEmpty() ? R.string.error_job_title_required : null;
        errors.email = validateEmail(cleanEmail);
        errors.password = validateNewPassword(pass);
        if (confirm.isEmpty()) {
            errors.confirmPassword = R.string.error_password_required;
        } else if (!confirm.equals(pass)) {
            errors.confirmPassword = R.string.error_confirm_mismatch;
        }
        formErrors.setValue(errors);
        if (errors.hasErrors()) {
            return;
        }
        submit(repository.register(
                new RegisterRequest(cleanName, cleanJob, cleanEmail, pass, confirm, currentRole())));
    }

    private boolean isBusy() {
        Resource<AuthResponse> value = authResult.getValue();
        return value != null && value.status == Resource.Status.LOADING;
    }

    private void submit(LiveData<Resource<AuthResponse>> source) {
        if (currentSource != null) {
            authResult.removeSource(currentSource);
        }
        currentSource = source;
        authResult.addSource(source, authResult::setValue);
    }

    private String currentRole() {
        String role = selectedRole.getValue();
        return role == null ? Roles.OPERATOR : role;
    }

    private static String trim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Nullable
    private static Integer validateEmail(String email) {
        if (email.isEmpty()) {
            return R.string.error_email_required;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return R.string.error_email_invalid;
        }
        return null;
    }

    @Nullable
    private static Integer validateNewPassword(String password) {
        if (password.isEmpty()) {
            return R.string.error_password_required;
        }
        boolean strong = password.length() >= MIN_PASSWORD_LENGTH
                && UPPERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SYMBOL.matcher(password).find();
        return strong ? null : R.string.error_password_rule;
    }
}
