package com.example.marketplace.data.model.auth;

import com.google.gson.annotations.SerializedName;

/** Answer of GET auth/me: the signed-in person's profile. */
public class MeResponse {
    @SerializedName("success")
    private Boolean success;
    @SerializedName("user")
    private User user;

    public User getUser() { return user; }
}
