package com.example.marketplace.data.model.settings;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/** Card 4: one carrier and whether its API link is switched on. The server sends no icon, the screen uses one icon for all. */
public class LogisticsPartner {

    public static final String CONNECTED = "Connected";
    public static final String INACTIVE = "Inactive";

    @SerializedName("carrierName")
    private String name;
    /** "Connected" or "Inactive". */
    @SerializedName("apiStatus")
    private String status;

    public LogisticsPartner(@Nullable String name, @Nullable String status) {
        this.name = name;
        this.status = status;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public boolean isConnected() {
        return CONNECTED.equals(status);
    }

    public void setConnected(boolean connected) {
        status = connected ? CONNECTED : INACTIVE;
    }

    /** Two partners are equal when name and on/off state match; this is how unsaved changes are noticed. */
    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof LogisticsPartner)) {
            return false;
        }
        LogisticsPartner that = (LogisticsPartner) other;
        return Objects.equals(name, that.name) && isConnected() == that.isConnected();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isConnected());
    }
}
