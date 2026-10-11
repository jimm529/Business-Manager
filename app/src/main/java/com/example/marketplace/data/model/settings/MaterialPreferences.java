package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Card 3: the purity and monthly volume the operator wants, and the low-stock alert. */
public class MaterialPreferences {

    @SerializedName("minimumPurity")
    private Double minimumPurity;
    @SerializedName("targetVolume")
    private Double targetVolume;
    @SerializedName("lowStockAlertEnabled")
    private boolean lowStockAlertEnabled;
    @SerializedName("lowStockThreshold")
    private Double lowStockThreshold;

    @Nullable
    public Double getMinimumPurity() {
        return minimumPurity;
    }

    @Nullable
    public Double getTargetVolume() {
        return targetVolume;
    }

    public boolean isLowStockAlertEnabled() {
        return lowStockAlertEnabled;
    }

    @Nullable
    public Double getLowStockThreshold() {
        return lowStockThreshold;
    }
}
