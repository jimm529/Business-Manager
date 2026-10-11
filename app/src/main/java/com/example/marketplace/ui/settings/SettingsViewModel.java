package com.example.marketplace.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.settings.ApprovalWorkflows;
import com.example.marketplace.data.model.settings.ChangePasswordRequest;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.settings.OperatorSettingsResponse;
import com.example.marketplace.data.model.settings.TwoFactorSetupResponse;
import com.example.marketplace.data.repository.SettingsRepository;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds everything of the Operator Settings screen so it survives rotation: the settings from the
 * server, the draft the person is editing, whether anything is unsaved, and the save, password
 * and two-factor calls.
 */
public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository = SettingsRepository.getInstance();

    private final ResourceLoader<OperatorSettingsResponse> loader = new ResourceLoader<>();
    private final ResourceLoader<OperatorSettingsResponse> saveLoader = new ResourceLoader<>();
    private final ResourceLoader<MessageResponse> passwordLoader = new ResourceLoader<>();
    private final ResourceLoader<TwoFactorSetupResponse> twoFactorSetupLoader = new ResourceLoader<>();
    private final ResourceLoader<MessageResponse> twoFactorVerifyLoader = new ResourceLoader<>();

    private final MediatorLiveData<Resource<OperatorSettingsResponse>> settings = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> saving = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<MessageResponse>> twoFactorVerify = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> dirty = new MutableLiveData<>(false);
    /** Goes up whenever the whole draft is replaced (loaded, saved, cancelled), so the screen refills its boxes. */
    private final MutableLiveData<Integer> draftVersion = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> twoFactorEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> successMessages = new MutableLiveData<>();
    private final MutableLiveData<Event<Resource<Void>>> errors = new MutableLiveData<>();

    @Nullable
    private OperatorSettingsResponse applied;
    @Nullable
    private SettingsDraft original;
    @NonNull
    private SettingsDraft draft = new SettingsDraft();

    public SettingsViewModel() {
        settings.addSource(loader.asLiveData(), resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null && resource.data != applied) {
                applied = resource.data;
                twoFactorEnabled.setValue(resource.data.getAccountDetails().isTwoFactorEnabled());
                replaceDraft(SettingsDraft.from(resource.data));
            }
            settings.setValue(resource);
        });
        saving.addSource(saveLoader.asLiveData(), this::onSaveResult);
        twoFactorVerify.addSource(twoFactorVerifyLoader.asLiveData(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                twoFactorEnabled.setValue(true);
                successMessages.setValue(new Event<>(R.string.settings_2fa_enabled));
            }
            twoFactorVerify.setValue(resource);
        });
        refresh();
    }

    // ---------------------------------------------------------------- what the screen observes

    /** The loaded settings (loading, error, or success with the server data). */
    public LiveData<Resource<OperatorSettingsResponse>> getSettings() {
        return settings;
    }

    /** True while "Save All Settings" is running. */
    public LiveData<Boolean> isSaving() {
        return saving;
    }

    /** True when the draft differs from what the server has. */
    public LiveData<Boolean> isDirty() {
        return dirty;
    }

    public LiveData<Integer> getDraftVersion() {
        return draftVersion;
    }

    public LiveData<Boolean> isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    /** One-time "saved" style messages. */
    public LiveData<Event<Integer>> getSuccessMessages() {
        return successMessages;
    }

    /** One-time errors that belong in a snackbar. */
    public LiveData<Event<Resource<Void>>> getErrors() {
        return errors;
    }

    public LiveData<Resource<MessageResponse>> getPasswordState() {
        return passwordLoader.asLiveData();
    }

    public LiveData<Resource<TwoFactorSetupResponse>> getTwoFactorSetupState() {
        return twoFactorSetupLoader.asLiveData();
    }

    public LiveData<Resource<MessageResponse>> getTwoFactorVerifyState() {
        return twoFactorVerify;
    }

    /** The draft being edited. The screen reads it to fill the boxes and writes into it as the person types. */
    @NonNull
    public SettingsDraft getDraft() {
        return draft;
    }

    /** People who could still be added as approvers. */
    @NonNull
    public List<ApprovalWorkflows.OrgUser> getAvailableApprovers() {
        List<ApprovalWorkflows.OrgUser> result = new ArrayList<>();
        if (applied == null) {
            return result;
        }
        for (ApprovalWorkflows.OrgUser user : applied.getAvailableUsers()) {
            if (user.getId() != null && !isApprover(user.getId())) {
                result.add(user);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- loading, editing, saving

    /** Loads the settings again and throws away unsaved edits. */
    public void refresh() {
        loader.load(repository.getSettings());
    }

    /** The screen calls this after every edit. */
    public void onDraftEdited() {
        updateDirty();
    }

    public void addApprover(@NonNull ApprovalWorkflows.OrgUser user) {
        if (user.getId() == null || isApprover(user.getId())) {
            return;
        }
        draft.approvers.add(new SettingsDraft.Approver(user.getId(), user.getFullName(), user.getJobTitle()));
        updateDirty();
    }

    public void removeApprover(int position) {
        if (position >= 0 && position < draft.approvers.size()) {
            draft.approvers.remove(position);
            updateDirty();
        }
    }

    public void moveApprover(int from, int to) {
        if (from >= 0 && to >= 0 && from < draft.approvers.size() && to < draft.approvers.size()) {
            Collections.swap(draft.approvers, from, to);
            updateDirty();
        }
    }

    /** "Cancel Changes": back to what the server has. */
    public void discardChanges() {
        if (original != null) {
            replaceDraft(original);
        }
    }

    /** "Save All Settings": sends the draft with PUT settings. */
    public void save() {
        if (original == null || Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        if (draft.facilityName.trim().isEmpty()) {
            errors.setValue(new Event<>(Resource.<Void>error(R.string.settings_error_facility_required)));
            return;
        }
        saveLoader.load(repository.updateSettings(draft.toJson()));
    }

    private void onSaveResult(Resource<OperatorSettingsResponse> resource) {
        switch (resource.status) {
            case LOADING:
                saving.setValue(true);
                break;
            case SUCCESS:
                // The server accepted the draft: its answer is the new "saved" copy.
                OperatorSettingsResponse body = resource.data;
                replaceDraft(body != null && body.hasSettings() ? SettingsDraft.from(body) : draft);
                saving.setValue(false);
                successMessages.setValue(new Event<>(R.string.settings_saved));
                saveLoader.reset();
                break;
            case ERROR:
                saving.setValue(false);
                errors.setValue(new Event<>(resource.<Void>withData(null)));
                saveLoader.reset();
                break;
            default:
                saving.setValue(false);
                break;
        }
    }

    // ---------------------------------------------------------------- password and 2FA

    public void changePassword(@NonNull String current, @NonNull String newPassword, @NonNull String confirm) {
        passwordLoader.load(repository.changePassword(new ChangePasswordRequest(current, newPassword, confirm)));
    }

    /** The password dialog is finished (success shown or dialog closed): forget the result. */
    public void clearPasswordState(boolean announceSuccess) {
        Resource<MessageResponse> state = passwordLoader.asLiveData().getValue();
        if (announceSuccess && state != null && state.status == Resource.Status.SUCCESS) {
            successMessages.setValue(new Event<>(R.string.settings_password_changed));
        }
        passwordLoader.reset();
    }

    public void startTwoFactorSetup() {
        twoFactorSetupLoader.load(repository.setUpTwoFactor());
    }

    public void verifyTwoFactor(@NonNull String code) {
        twoFactorVerifyLoader.load(repository.verifyTwoFactor(code));
    }

    /** The 2FA dialog is finished or cancelled: forget both results. */
    public void clearTwoFactor() {
        twoFactorSetupLoader.reset();
        twoFactorVerifyLoader.reset();
    }

    // ---------------------------------------------------------------- helpers

    private boolean isApprover(@NonNull String userId) {
        for (SettingsDraft.Approver approver : draft.approvers) {
            if (approver.userId.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /** Makes the given server copy the "saved" one and starts a fresh draft from it. */
    private void replaceDraft(@NonNull SettingsDraft saved) {
        original = saved.copy();
        draft = saved.copy();
        Integer version = draftVersion.getValue();
        draftVersion.setValue(version == null ? 1 : version + 1);
        updateDirty();
    }

    private void updateDirty() {
        boolean isDirty = original != null && !draft.equals(original);
        if (!Boolean.valueOf(isDirty).equals(dirty.getValue())) {
            dirty.setValue(isDirty);
        }
    }
}
