package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import java.util.Objects;

/** One open negotiation shown as a card under "Active Deals". Any text can be null. */
public class ActiveDeal {

    /** Whose move it is in the negotiation. */
    public enum Turn { YOURS, VENDOR }

    private final String id;
    private final String lotNumber;
    private final String materialName;
    private final Double quantity;
    /** Offered price per metric ton (rupees). */
    private final Double pricePerUnit;
    private final String status;
    private final Turn turnStatus;
    /** True when the last offer on the table came from this buyer (a counter offer was sent). */
    private final boolean offerSentByYou;
    /** Milliseconds since 1970 when the deal closes; always null for now, the backend has no deadline. */
    @Nullable
    private final Long closingTime;

    public ActiveDeal(@Nullable String id, @Nullable String lotNumber, @Nullable String materialName,
                      @Nullable Double quantity, @Nullable Double pricePerUnit, @Nullable String status,
                      Turn turnStatus, boolean offerSentByYou, @Nullable Long closingTime) {
        this.id = id;
        this.lotNumber = lotNumber;
        this.materialName = materialName;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.status = status;
        this.turnStatus = turnStatus;
        this.offerSentByYou = offerSentByYou;
        this.closingTime = closingTime;
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getLotNumber() { return lotNumber; }
    @Nullable public String getMaterialName() { return materialName; }
    @Nullable public Double getQuantity() { return quantity; }
    @Nullable public Double getPricePerUnit() { return pricePerUnit; }
    @Nullable public String getStatus() { return status; }
    public Turn getTurnStatus() { return turnStatus; }
    public boolean isOfferSentByYou() { return offerSentByYou; }
    @Nullable public Long getClosingTime() { return closingTime; }

    public boolean sameContentAs(ActiveDeal other) {
        return Objects.equals(lotNumber, other.lotNumber)
                && Objects.equals(materialName, other.materialName)
                && Objects.equals(quantity, other.quantity)
                && Objects.equals(pricePerUnit, other.pricePerUnit)
                && Objects.equals(status, other.status)
                && turnStatus == other.turnStatus
                && offerSentByYou == other.offerSentByYou;
    }
}
