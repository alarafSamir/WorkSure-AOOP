package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetPasswordRequest {
    private String token;
    private String password;

    public String getToken() { return token; }
    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() { return password; }
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

}
