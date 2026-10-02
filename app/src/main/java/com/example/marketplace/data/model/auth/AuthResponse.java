package com.example.marketplace.data.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Answer of login / register. When the account uses two-factor login the backend sends
 * requires2FA = true and a pendingToken but no token.
 */
public class AuthResponse {
    @SerializedName("success")
    private Boolean success;
    @SerializedName("message")
    private String message;
    @SerializedName("token")
    private String token;
    @SerializedName("requires2FA")
    private Boolean requires2FA;
    @SerializedName("pendingToken")
    private String pendingToken;
    @SerializedName("user")
    private User user;

    public String getMessage() { return message; }
    public String getToken() { return token; }
    public boolean requiresTwoFactor() { return Boolean.TRUE.equals(requires2FA); }
    public String getPendingToken() { return pendingToken; }
    public User getUser() { return user; }
}
