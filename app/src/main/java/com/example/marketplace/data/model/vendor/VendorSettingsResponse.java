package com.example.marketplace.data.model.vendor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Answer of GET vendor-portal/settings: the company profile, certifications, preferences and the signed-in user. */
public class VendorSettingsResponse {

    /** The server's names for the three listing defaults. */
    public static final String DEFAULT_FOB = "fobShipping";
    public static final String DEFAULT_DDP = "ddpGlobal";
    public static final String DEFAULT_NET30 = "net30Payment";

    @SerializedName("settings")
    private Settings settings;
    @SerializedName("user")
    private User user;

    @Nullable
    public Settings getSettings() { return settings; }

    @Nullable
    public User getUser() { return user; }

    public static class Settings {
        @SerializedName("entityName")
        private String entityName;
        @SerializedName("taxId")
        private String taxId;
        @SerializedName("headquartersAddress")
        private String headquartersAddress;
        @SerializedName("facilityImages")
        private List<String> facilityImages;
        @SerializedName("certifications")
        private List<Certification> certifications;
        @SerializedName("operationalPreferences")
        private OperationalPreferences operationalPreferences;
        @SerializedName("listingDefaults")
        private List<String> listingDefaults;
        @SerializedName("notificationPreferences")
        private NotificationPreferences notificationPreferences;

        @NonNull public String getEntityName() { return entityName == null ? "" : entityName; }
        @NonNull public String getTaxId() { return taxId == null ? "" : taxId; }
        @NonNull public String getHeadquartersAddress() { return headquartersAddress == null ? "" : headquartersAddress; }

        @NonNull
        public List<String> getFacilityImages() {
            return facilityImages == null ? new ArrayList<>() : facilityImages;
        }

        @NonNull
        public List<Certification> getCertifications() {
            return certifications == null ? new ArrayList<>() : certifications;
        }

        @Nullable public OperationalPreferences getOperationalPreferences() { return operationalPreferences; }

        @NonNull
        public List<String> getListingDefaults() {
            return listingDefaults == null ? new ArrayList<>() : listingDefaults;
        }

        @Nullable public NotificationPreferences getNotificationPreferences() { return notificationPreferences; }
    }

    public static class Certification {
        @SerializedName("name")
        private String name;
        @SerializedName("expiryDate")
        private String expiryDate;
        /** "active", "pending" or "expired". */
        @SerializedName("status")
        private String status;

        @Nullable public String getName() { return name; }
        @Nullable public String getExpiryDate() { return expiryDate; }
        @Nullable public String getStatus() { return status; }
    }

    public static class OperationalPreferences {
        @SerializedName("materialYieldTargetPercent")
        private Double materialYieldTargetPercent;
        @SerializedName("plantCapacityMT")
        private Double plantCapacityMT;
        @SerializedName("typicalLeadPurity")
        private String typicalLeadPurity;

        @Nullable public Double getMaterialYield() { return materialYieldTargetPercent; }
        @Nullable public Double getPlantCapacity() { return plantCapacityMT; }
        @Nullable public String getTypicalLeadPurity() { return typicalLeadPurity; }
    }

    public static class NotificationPreferences {
        @SerializedName("newOrderRequests")
        private Boolean newOrderRequests;
        @SerializedName("successfulSales")
        private Boolean successfulSales;
        @SerializedName("logisticsUpdates")
        private Boolean logisticsUpdates;
        @SerializedName("priceAlerts")
        private Boolean priceAlerts;

        public boolean isNewOrderRequests() { return Boolean.TRUE.equals(newOrderRequests); }
        public boolean isSuccessfulSales() { return Boolean.TRUE.equals(successfulSales); }
        public boolean isLogisticsUpdates() { return Boolean.TRUE.equals(logisticsUpdates); }
        public boolean isPriceAlerts() { return Boolean.TRUE.equals(priceAlerts); }
    }

    public static class User {
        @SerializedName("fullName")
        private String fullName;
        @SerializedName("jobTitle")
        private String jobTitle;
        @SerializedName("email")
        private String email;
        @SerializedName("twoFactorEnabled")
        private Boolean twoFactorEnabled;

        @NonNull public String getFullName() { return fullName == null ? "" : fullName; }
        @NonNull public String getJobTitle() { return jobTitle == null ? "" : jobTitle; }
        @NonNull public String getEmail() { return email == null ? "" : email; }
        public boolean isTwoFactorEnabled() { return Boolean.TRUE.equals(twoFactorEnabled); }
    }
}
