package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuspendUserRequest {
    private String until;

    public String getUntil() { return until; }
    @JsonProperty("until")
    public void setUntil(String until) {
        this.until = until;
    }

}
