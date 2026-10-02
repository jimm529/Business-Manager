package com.example.marketplace.data.model.common;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Answer of GET health: tells whether the server is up and whether it reached its database. */
public class HealthResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("message")
    private String message;
    /** "connected" or "disconnected". */
    @SerializedName("database")
    private String database;

    @Nullable public String getStatus() { return status; }
    @Nullable public String getMessage() { return message; }
    @Nullable public String getDatabase() { return database; }

    public boolean isDatabaseConnected() {
        return "connected".equalsIgnoreCase(database);
    }
}
