package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Card 1: the facility name, spending limit per order, payment terms and billing currency. */
public class ProcurementProfile {

    @SerializedName("facilityName")
    private String facilityName;
    @SerializedName("procurementLimit")
    private Double procurementLimit;
    /** The option value (see OperatorSettingsResponse.Options), not the label. */
    @SerializedName("defaultPaymentTerms")
    private String defaultPaymentTerms;
    @SerializedName("billingCurrency")
    private String billingCurrency;

    @Nullable
    public String getFacilityName() {
        return facilityName;
    }

    @Nullable
    public Double getProcurementLimit() {
        return procurementLimit;
    }

    @Nullable
    public String getDefaultPaymentTerms() {
        return defaultPaymentTerms;
    }

    @Nullable
    public String getBillingCurrency() {
        return billingCurrency;
    }
}
