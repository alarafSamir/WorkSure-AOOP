package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {
    private String email;
    private String password;

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

}
