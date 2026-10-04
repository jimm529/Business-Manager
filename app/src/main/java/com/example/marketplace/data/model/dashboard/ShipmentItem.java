package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import java.util.Objects;

/** One order on its way to the buyer, shown under "Incoming Shipments". Any value can be null. */
public class ShipmentItem {

    /** Which coloured dot the row gets. */
    public enum Stage { ON_THE_WAY, PROCESSING, BOOKED }

    private final String id;
    private final String lotNumber;
    private final Stage status;
    private final String materialName;
    /** Expected delivery (or order) date in milliseconds since 1970; null when unknown. */
    @Nullable
    private final Long date;
    private final Double weight;
    private final String unit;

    public ShipmentItem(@Nullable String id, @Nullable String lotNumber, Stage status,
                        @Nullable String materialName, @Nullable Long date,
                        @Nullable Double weight, @Nullable String unit) {
        this.id = id;
        this.lotNumber = lotNumber;
        this.status = status;
        this.materialName = materialName;
        this.date = date;
        this.weight = weight;
        this.unit = unit;
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getLotNumber() { return lotNumber; }
    public Stage getStatus() { return status; }
    @Nullable public String getMaterialName() { return materialName; }
    @Nullable public Long getDate() { return date; }
    @Nullable public Double getWeight() { return weight; }
    @Nullable public String getUnit() { return unit; }

    public boolean sameContentAs(ShipmentItem other) {
        return Objects.equals(lotNumber, other.lotNumber)
                && status == other.status
                && Objects.equals(materialName, other.materialName)
                && Objects.equals(date, other.date)
                && Objects.equals(weight, other.weight)
                && Objects.equals(unit, other.unit);
    }
}
