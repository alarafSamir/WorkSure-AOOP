package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String role;

    public String getEmail() { return email; }
    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() { return password; }
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() { return fullName; }
    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() { return phone; }
    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() { return role; }
    @JsonProperty("role")
    public void setRole(String role) {
        this.role = role;
    }

}
