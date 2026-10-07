package com.example.marketplace.data.model.bid;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.Objects;

/**
 * One deal (the server calls it a bid): the batch, quantity, rate and total, and whose move it is.
 * The same shape arrives inside chat messages and in the list of the buyer's deals.
 */
public class NegotiationDetail {

    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_COUNTERED = "countered";

    @SerializedName("_id")
    private String id;
    @SerializedName("lotId")
    private String lotId;
    @SerializedName("batchTitle")
    private String batchTitle;
    @SerializedName("material")
    private String material;
    @SerializedName("quantityMT")
    private Double quantityMT;
    @SerializedName("unitPricePerKg")
    private Double unitPricePerKg;
    @SerializedName("amount")
    private Double amount;
    @SerializedName("currency")
    private String currency;
    @SerializedName("status")
    private String status;
    @SerializedName("currentTurn")
    private String currentTurn;
    @SerializedName("notes")
    private String notes;
    @SerializedName("updatedAt")
    private String updatedAt;

    @Nullable
    public String getBidId() {
        return id;
    }

    /** The batch label without a leading "#", e.g. "8829". */
    @Nullable
    public String getBatchNumber() {
        if (lotId == null || lotId.trim().isEmpty()) {
            return null;
        }
        return lotId.trim().replaceFirst("^#", "");
    }

    @Nullable
    public String getBatchTitle() {
        return batchTitle;
    }

    @Nullable
    public String getCommodity() {
        return material;
    }

    @Nullable
    public Double getQuantityMT() {
        return quantityMT;
    }

    /** Quantity in kilograms (1 MT = 1000 kg). */
    @Nullable
    public Double getQuantityKg() {
        return quantityMT == null ? null : quantityMT * 1000d;
    }

    @Nullable
    public Double getCurrentRatePerKg() {
        return unitPricePerKg;
    }

    @Nullable
    public Double getTotalValue() {
        return amount;
    }

    @Nullable
    public String getCurrency() {
        return currency;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isClosed() {
        return statusIs(STATUS_ACCEPTED) || statusIs(STATUS_DECLINED);
    }

    public boolean statusIs(String expected) {
        return status != null && status.toLowerCase(Locale.ROOT).equals(expected);
    }

    /** True when the buyer is the one who has to answer (older records without a turn fall back to the status). */
    public boolean isBuyerTurn() {
        if (isClosed()) {
            return false;
        }
        String turn = currentTurn;
        if (turn == null || turn.isEmpty()) {
            turn = statusIs(STATUS_COUNTERED) ? "vendor" : "operator";
        }
        return "operator".equalsIgnoreCase(turn);
    }

    public boolean sameContentAs(NegotiationDetail other) {
        return Objects.equals(id, other.id) && Objects.equals(status, other.status)
                && Objects.equals(amount, other.amount) && Objects.equals(updatedAt, other.updatedAt)
                && Objects.equals(currentTurn, other.currentTurn);
    }
}
