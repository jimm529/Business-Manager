package com.example.marketplace.ui.vendorsettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.marketplace.R;
import com.example.marketplace.data.model.listing.ListingOptionsResponse;
import com.example.marketplace.data.model.settings.MessageResponse;
import com.example.marketplace.data.model.vendor.VendorSettingsResponse;
import com.example.marketplace.data.repository.ListingRepository;
import com.example.marketplace.data.repository.VendorSettingsRepository;
import com.example.marketplace.ui.messages.ActionResult;
import com.example.marketplace.utils.Event;
import com.example.marketplace.utils.Formatters;
import com.example.marketplace.utils.Resource;
import com.example.marketplace.utils.ResourceLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Holds the vendor's Settings: what the server sent, the typical-purity choices, and saving each
 * section on its own. A save never reloads the screen, so what you typed in the other sections stays.
 */
public class VendorSettingsViewModel extends ViewModel {

    private final VendorSettingsRepository repository = VendorSettingsRepository.getInstance();
    private final ResourceLoader<VendorSettingsResponse> settings = new ResourceLoader<>();
    private final ResourceLoader<ListingOptionsResponse> options = new ResourceLoader<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Event<ActionResult>> results = new MutableLiveData<>();
    /** A switch whose save failed: the key of the switch to put back. */
    private final MutableLiveData<Event<String>> notificationReverts = new MutableLiveData<>();

    public VendorSettingsViewModel() {
        refresh();
    }

    public LiveData<Resource<VendorSettingsResponse>> getSettings() { return settings.asLiveData(); }

    public LiveData<Resource<ListingOptionsResponse>> getOptions() { return options.asLiveData(); }

    public LiveData<Boolean> isSaving() { return saving; }

    public LiveData<Event<ActionResult>> getResults() { return results; }

    public LiveData<Event<String>> getNotificationReverts() { return notificationReverts; }

    public void refresh() {
        settings.load(repository.getSettings());
        options.load(ListingRepository.getInstance().getOptions());
    }

    /**
     * The usual purity choices, e.g. "99.97%": the distinct purities of the marketplace's material types,
     * highest first. These come from the server, not from a fixed list.
     */
    @NonNull
    public static List<String> purityChoices(@Nullable ListingOptionsResponse response) {
        TreeSet<Double> purities = new TreeSet<>((a, b) -> Double.compare(b, a));
        if (response != null) {
            for (ListingOptionsResponse.Option option : response.getMaterialTypes()) {
                if (option.getDefaultPurity() != null && option.getDefaultPurity() > 0) {
                    purities.add(option.getDefaultPurity());
                }
            }
        }
        List<String> choices = new ArrayList<>();
        for (Double purity : purities) {
            choices.add(Formatters.number(purity) + "%");
        }
        return choices;
    }

    // ---------------------------------------------------------------- saving

    public void saveProfile(@NonNull String entityName, @NonNull String taxId, @NonNull String address) {
        JsonObject body = new JsonObject();
        body.addProperty("entityName", entityName);
        body.addProperty("taxId", taxId);
        body.addProperty("headquartersAddress", address);
        run(repository.saveProfile(body), R.string.vsettings_profile_saved, null);
    }

    public void savePreferences(double materialYield, double plantCapacity, @NonNull String typicalPurity,
                                @NonNull List<String> listingDefaults) {
        JsonObject body = new JsonObject();
        body.addProperty("materialYieldTargetPercent", materialYield);
        body.addProperty("plantCapacityMT", plantCapacity);
        body.addProperty("typicalLeadPurity", typicalPurity);
        JsonArray defaults = new JsonArray();
        for (String value : listingDefaults) {
            defaults.add(value);
        }
        body.add("listingDefaults", defaults);
        run(repository.savePreferences(body), R.string.vsettings_prefs_saved, null);
    }

    /** Saves one notification switch right away; if it fails the switch is put back. */
    public void saveNotification(@NonNull String key, boolean enabled) {
        JsonObject body = new JsonObject();
        body.addProperty(key, enabled);
        run(repository.saveNotifications(body), R.string.vsettings_notification_saved, key);
    }

    public void saveAccount(@NonNull String fullName, @NonNull String jobTitle) {
        JsonObject body = new JsonObject();
        body.addProperty("fullName", fullName);
        body.addProperty("jobTitle", jobTitle);
        run(repository.saveAccount(body), R.string.vsettings_account_saved, null);
    }

    private void run(LiveData<Resource<MessageResponse>> call, @StringRes int successText, @Nullable String revertKey) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            if (revertKey != null) {
                notificationReverts.setValue(new Event<>(revertKey));
            }
            return;
        }
        saving.setValue(true);
        call.observeForever(new Observer<Resource<MessageResponse>>() {
            @Override
            public void onChanged(Resource<MessageResponse> result) {
                if (result.status == Resource.Status.LOADING) {
                    return;
                }
                call.removeObserver(this);
                saving.setValue(false);
                if (result.status == Resource.Status.SUCCESS) {
                    String message = result.data == null ? null : result.data.getMessage();
                    results.setValue(new Event<>(ActionResult.success(message, successText)));
                } else {
                    results.setValue(new Event<>(ActionResult.failure(result)));
                    if (revertKey != null) {
                        notificationReverts.setValue(new Event<>(revertKey));
                    }
                }
            }
        });
    }
}
