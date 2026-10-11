package com.example.marketplace.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.marketplace.data.model.settings.ApprovalWorkflows;
import com.example.marketplace.data.model.settings.LogisticsPartner;
import com.example.marketplace.data.model.settings.MaterialPreferences;
import com.example.marketplace.data.model.settings.OperatorSettingsResponse;
import com.example.marketplace.data.model.settings.PriceFixing;
import com.example.marketplace.data.model.settings.ProcurementProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The editable copy of the settings the person is changing. Numbers are kept as the text typed in the
 * boxes. Comparing the draft with the copy that came from the server tells whether anything is unsaved.
 */
public class SettingsDraft {

    /** One approver row: who it is and how to show them. */
    public static class Approver {
        public final String userId;
        public final String name;
        public final String title;

        public Approver(@NonNull String userId, @Nullable String name, @Nullable String title) {
            this.userId = userId;
            this.name = name;
            this.title = title;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Approver && userId.equals(((Approver) other).userId);
        }

        @Override
        public int hashCode() {
            return userId.hashCode();
        }
    }

    // 1. Procurement profile
    public String facilityName = "";
    public String procurementLimit = "";
    public String paymentTerms = "";
    public String billingCurrency = "";
    // 2. Approval workflows
    public String signoffThreshold = "";
    public List<Approver> approvers = new ArrayList<>();
    // 3. Material preferences
    public String minimumPurity = "";
    public String targetVolume = "";
    public boolean lowStockAlertEnabled;
    /** Not shown on this screen, but sent back unchanged so saving never wipes it. */
    public String lowStockThreshold = "";
    // 4. Logistics partners
    public List<LogisticsPartner> partners = new ArrayList<>();
    // 5. Security (the amount above which a 2FA code is asked; sent back unchanged)
    public String twoFactorThreshold = "";
    // 6. Price fixing
    public String baseMaterialRate = "";
    public String marketAdjustment = "";
    public boolean manualPriceOverride;

    /** Builds the draft from a server answer. */
    @NonNull
    public static SettingsDraft from(@NonNull OperatorSettingsResponse response) {
        OperatorSettingsResponse.Settings settings = response.getSettings();
        SettingsDraft draft = new SettingsDraft();

        ProcurementProfile profile = settings.getProcurementProfile();
        draft.facilityName = text(profile.getFacilityName());
        draft.procurementLimit = text(profile.getProcurementLimit());
        draft.paymentTerms = text(profile.getDefaultPaymentTerms());
        draft.billingCurrency = text(profile.getBillingCurrency());

        ApprovalWorkflows workflows = settings.getApprovalWorkflows();
        draft.signoffThreshold = text(workflows.getManagerSignoffThreshold());
        for (ApprovalWorkflows.Approver approver : workflows.getApprovers()) {
            ApprovalWorkflows.OrgUser user = approver.getUser();
            if (user != null && user.getId() != null) {
                draft.approvers.add(new Approver(user.getId(), user.getFullName(), user.getJobTitle()));
            }
        }

        MaterialPreferences materials = settings.getMaterialPreferences();
        draft.minimumPurity = text(materials.getMinimumPurity());
        draft.targetVolume = text(materials.getTargetVolume());
        draft.lowStockAlertEnabled = materials.isLowStockAlertEnabled();
        draft.lowStockThreshold = text(materials.getLowStockThreshold());

        for (LogisticsPartner partner : settings.getLogisticsPartners()) {
            draft.partners.add(new LogisticsPartner(partner.getName(),
                    partner.isConnected() ? LogisticsPartner.CONNECTED : LogisticsPartner.INACTIVE));
        }

        draft.twoFactorThreshold = text(settings.getSecurityAccess().getTwoFactorThreshold());

        PriceFixing pricing = settings.getPriceFixing();
        draft.baseMaterialRate = text(pricing.getBaseMaterialRate());
        draft.marketAdjustment = text(pricing.getMarketAdjustment());
        draft.manualPriceOverride = pricing.isManualPriceOverrideEnabled();
        return draft;
    }

    /** A separate copy, so editing one never changes the other. */
    @NonNull
    public SettingsDraft copy() {
        SettingsDraft c = new SettingsDraft();
        c.facilityName = facilityName;
        c.procurementLimit = procurementLimit;
        c.paymentTerms = paymentTerms;
        c.billingCurrency = billingCurrency;
        c.signoffThreshold = signoffThreshold;
        c.approvers = new ArrayList<>(approvers);
        c.minimumPurity = minimumPurity;
        c.targetVolume = targetVolume;
        c.lowStockAlertEnabled = lowStockAlertEnabled;
        c.lowStockThreshold = lowStockThreshold;
        for (LogisticsPartner partner : partners) {
            c.partners.add(new LogisticsPartner(partner.getName(),
                    partner.isConnected() ? LogisticsPartner.CONNECTED : LogisticsPartner.INACTIVE));
        }
        c.twoFactorThreshold = twoFactorThreshold;
        c.baseMaterialRate = baseMaterialRate;
        c.marketAdjustment = marketAdjustment;
        c.manualPriceOverride = manualPriceOverride;
        return c;
    }

    /** The body for PUT settings. An empty number box is sent as "" which the server stores as "no value". */
    @NonNull
    public JsonObject toJson() {
        JsonObject profile = new JsonObject();
        profile.addProperty("facilityName", facilityName.trim());
        profile.addProperty("procurementLimit", procurementLimit.trim());
        profile.addProperty("defaultPaymentTerms", paymentTerms);
        profile.addProperty("billingCurrency", billingCurrency);

        JsonArray approverIds = new JsonArray();
        for (Approver approver : approvers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("userId", approver.userId);
            approverIds.add(entry);
        }
        JsonObject workflows = new JsonObject();
        workflows.addProperty("managerSignoffThreshold", signoffThreshold.trim());
        workflows.add("approvers", approverIds);

        JsonObject materials = new JsonObject();
        materials.addProperty("minimumPurity", minimumPurity.trim());
        materials.addProperty("targetVolume", targetVolume.trim());
        materials.addProperty("lowStockAlertEnabled", lowStockAlertEnabled);
        materials.addProperty("lowStockThreshold", lowStockThreshold.trim());

        JsonArray carriers = new JsonArray();
        for (LogisticsPartner partner : partners) {
            JsonObject entry = new JsonObject();
            entry.addProperty("carrierName", partner.getName());
            entry.addProperty("apiStatus", partner.isConnected() ? LogisticsPartner.CONNECTED : LogisticsPartner.INACTIVE);
            carriers.add(entry);
        }

        JsonObject security = new JsonObject();
        security.addProperty("twoFactorThreshold", twoFactorThreshold.trim());

        JsonObject pricing = new JsonObject();
        pricing.addProperty("baseMaterialRate", baseMaterialRate.trim());
        pricing.addProperty("marketAdjustment", marketAdjustment.trim());
        pricing.addProperty("manualPriceOverride", manualPriceOverride);

        JsonObject body = new JsonObject();
        body.add("procurementProfile", profile);
        body.add("approvalWorkflows", workflows);
        body.add("materialPreferences", materials);
        body.add("logisticsPartners", carriers);
        body.add("securityAccess", security);
        body.add("priceFixing", pricing);
        return body;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof SettingsDraft)) {
            return false;
        }
        SettingsDraft o = (SettingsDraft) other;
        return lowStockAlertEnabled == o.lowStockAlertEnabled
                && manualPriceOverride == o.manualPriceOverride
                && facilityName.trim().equals(o.facilityName.trim())
                && procurementLimit.trim().equals(o.procurementLimit.trim())
                && paymentTerms.equals(o.paymentTerms)
                && billingCurrency.equals(o.billingCurrency)
                && signoffThreshold.trim().equals(o.signoffThreshold.trim())
                && approvers.equals(o.approvers)
                && minimumPurity.trim().equals(o.minimumPurity.trim())
                && targetVolume.trim().equals(o.targetVolume.trim())
                && lowStockThreshold.trim().equals(o.lowStockThreshold.trim())
                && partners.equals(o.partners)
                && twoFactorThreshold.trim().equals(o.twoFactorThreshold.trim())
                && baseMaterialRate.trim().equals(o.baseMaterialRate.trim())
                && marketAdjustment.trim().equals(o.marketAdjustment.trim());
    }

    @Override
    public int hashCode() {
        return Objects.hash(facilityName, procurementLimit, paymentTerms, billingCurrency, signoffThreshold,
                approvers, minimumPurity, targetVolume, lowStockAlertEnabled, partners, baseMaterialRate,
                marketAdjustment, manualPriceOverride);
    }

    @NonNull
    private static String text(@Nullable String value) {
        return value == null ? "" : value;
    }

    /** 250000.0 -> "250000", 99.97 -> "99.97" (no trailing zeros, no exponent). */
    @NonNull
    private static String text(@Nullable Double value) {
        return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
