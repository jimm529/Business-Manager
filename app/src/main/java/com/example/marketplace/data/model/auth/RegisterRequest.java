package com.example.marketplace.data.model.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Body of POST auth/register. The backend requires exactly these six fields
 * (it has no company-name field, so the form asks for a job title instead).
 */
public class RegisterRequest {
    @SerializedName("fullName")
    private final String fullName;
    @SerializedName("jobTitle")
    private final String jobTitle;
    @SerializedName("email")
    private final String email;
    @SerializedName("password")
    private final String password;
    @SerializedName("confirmPassword")
    private final String confirmPassword;
    @SerializedName("role")
    private final String role;

    public RegisterRequest(String fullName, String jobTitle, String email,
                           String password, String confirmPassword, String role) {
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.role = role;
    }
}
