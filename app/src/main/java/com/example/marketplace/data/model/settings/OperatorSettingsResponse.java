package com.example.marketplace.data.model.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Answer of GET settings (and, with fewer fields, PUT settings): the saved sections, the choices for
 * the two dropdowns, the people who can be approvers, and the signed-in person's account details.
 */
public class OperatorSettingsResponse {

    @SerializedName("settings")
    private Settings settings;
    @SerializedName("options")
    private Options options;
    @SerializedName("accountDetails")
    private AccountDetails accountDetails;
    @SerializedName("availableUsers")
    private List<ApprovalWorkflows.OrgUser> availableUsers;

    /** False when the server answer carried no settings section at all. */
    public boolean hasSettings() {
        return settings != null;
    }

    /** Never null. */
    @NonNull
    public Settings getSettings() {
        return settings == null ? new Settings() : settings;
    }

    /** Never null. */
    @NonNull
    public Options getOptions() {
        return options == null ? new Options() : options;
    }

    /** Never null. */
    @NonNull
    public AccountDetails getAccountDetails() {
        return accountDetails == null ? new AccountDetails() : accountDetails;
    }

    /** Never null. */
    @NonNull
    public List<ApprovalWorkflows.OrgUser> getAvailableUsers() {
        return availableUsers == null ? new ArrayList<>() : availableUsers;
    }

    /** All editable sections. */
    public static class Settings {
        @SerializedName("procurementProfile")
        private ProcurementProfile procurementProfile;
        @SerializedName("approvalWorkflows")
        private ApprovalWorkflows approvalWorkflows;
        @SerializedName("materialPreferences")
        private MaterialPreferences materialPreferences;
        @SerializedName("logisticsPartners")
        private List<LogisticsPartner> logisticsPartners;
        @SerializedName("securityAccess")
        private SecuritySettings securityAccess;
        @SerializedName("priceFixing")
        private PriceFixing priceFixing;
        @SerializedName("whitelistedVendors")
        private List<WhitelistedVendor> whitelistedVendors;

        @NonNull
        public ProcurementProfile getProcurementProfile() {
            return procurementProfile == null ? new ProcurementProfile() : procurementProfile;
        }

        @NonNull
        public ApprovalWorkflows getApprovalWorkflows() {
            return approvalWorkflows == null ? new ApprovalWorkflows() : approvalWorkflows;
        }

        @NonNull
        public MaterialPreferences getMaterialPreferences() {
            return materialPreferences == null ? new MaterialPreferences() : materialPreferences;
        }

        @NonNull
        public List<LogisticsPartner> getLogisticsPartners() {
            return logisticsPartners == null ? new ArrayList<>() : logisticsPartners;
        }

        @NonNull
        public SecuritySettings getSecurityAccess() {
            return securityAccess == null ? new SecuritySettings() : securityAccess;
        }

        @NonNull
        public PriceFixing getPriceFixing() {
            return priceFixing == null ? new PriceFixing() : priceFixing;
        }

        @NonNull
        public List<WhitelistedVendor> getWhitelistedVendors() {
            return whitelistedVendors == null ? new ArrayList<>() : whitelistedVendors;
        }
    }

    /** The choices for Default Payment Terms and Billing Currency. */
    public static class Options {
        @SerializedName("paymentTerms")
        private List<Option> paymentTerms;
        @SerializedName("billingCurrencies")
        private List<Option> billingCurrencies;

        @NonNull
        public List<Option> getPaymentTerms() {
            return paymentTerms == null ? new ArrayList<>() : paymentTerms;
        }

        @NonNull
        public List<Option> getBillingCurrencies() {
            return billingCurrencies == null ? new ArrayList<>() : billingCurrencies;
        }
    }

    /** One dropdown choice: "value" is what is saved, "label" is what is shown. */
    public static class Option {
        @SerializedName("value")
        private String value;
        @SerializedName("label")
        private String label;

        @Nullable
        public String getValue() {
            return value;
        }

        @Nullable
        public String getLabel() {
            return label;
        }
    }

    /** Who is signed in (Card 8) and whether 2FA is on. */
    public static class AccountDetails {
        @SerializedName("fullName")
        private String fullName;
        @SerializedName("role")
        private String role;
        @SerializedName("email")
        private String email;
        @SerializedName("twoFactorEnabled")
        private boolean twoFactorEnabled;

        @Nullable
        public String getFullName() {
            return fullName;
        }

        @Nullable
        public String getRole() {
            return role;
        }

        @Nullable
        public String getEmail() {
            return email;
        }

        public boolean isTwoFactorEnabled() {
            return twoFactorEnabled;
        }
    }
}
