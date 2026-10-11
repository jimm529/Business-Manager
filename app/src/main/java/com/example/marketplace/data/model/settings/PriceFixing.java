package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Card 6: the base rate per kg, the market adjustment in percent, and the manual-override switch. */
public class PriceFixing {

    @SerializedName("baseMaterialRate")
    private Double baseMaterialRate;
    @SerializedName("marketAdjustment")
    private Double marketAdjustment;
    /** The server calls the switch "manualPriceOverride". */
    @SerializedName("manualPriceOverride")
    private boolean manualPriceOverrideEnabled;

    @Nullable
    public Double getBaseMaterialRate() {
        return baseMaterialRate;
    }

    @Nullable
    public Double getMarketAdjustment() {
        return marketAdjustment;
    }

    public boolean isManualPriceOverrideEnabled() {
        return manualPriceOverrideEnabled;
    }
}
