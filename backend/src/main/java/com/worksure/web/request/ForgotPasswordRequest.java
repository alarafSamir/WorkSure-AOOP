package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgotPasswordRequest {
    private String email;

    public String getEmail() { return email; }
    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

}
