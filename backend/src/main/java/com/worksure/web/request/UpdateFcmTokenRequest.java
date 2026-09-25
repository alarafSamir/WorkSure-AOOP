package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateFcmTokenRequest {
    private String fcmToken;

    public String getFcmToken() { return fcmToken; }
    @JsonProperty("fcm_token")
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

}
