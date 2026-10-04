package com.example.marketplace.data.model.dashboard;

import androidx.annotation.Nullable;

import java.util.Objects;

/** One supplier tile under "Regular Suppliers". */
public class SupplierSummary {

    private final String id;
    private final String name;
    private final String imageUrl;

    public SupplierSummary(@Nullable String id, @Nullable String name, @Nullable String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getName() { return name; }
    @Nullable public String getImageUrl() { return imageUrl; }

    public boolean sameContentAs(SupplierSummary other) {
        return Objects.equals(name, other.name) && Objects.equals(imageUrl, other.imageUrl);
    }
}
