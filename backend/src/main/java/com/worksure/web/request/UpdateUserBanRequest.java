package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserBanRequest {
    private Boolean isBanned;

    public Boolean getIsBanned() { return isBanned; }
    @JsonProperty("is_banned")
    @JsonDeserialize(using = RequestDeserializers.LegacyBoolean.class)
    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }

}
