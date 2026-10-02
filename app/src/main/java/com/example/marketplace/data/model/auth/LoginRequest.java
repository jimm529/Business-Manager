package com.example.marketplace.data.model.auth;

import com.google.gson.annotations.SerializedName;

/** Body of POST auth/login. "role" is the selected portal: "vendor" or "operator". */
public class LoginRequest {
    @SerializedName("email")
    private final String email;
    @SerializedName("password")
    private final String password;
    @SerializedName("role")
    private final String role;

    public LoginRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
