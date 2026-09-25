package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutRequest {
    private String defaultAddress;

    public String getDefaultAddress() { return defaultAddress; }
    @JsonProperty("default_address")
    public void setDefaultAddress(String defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

}
