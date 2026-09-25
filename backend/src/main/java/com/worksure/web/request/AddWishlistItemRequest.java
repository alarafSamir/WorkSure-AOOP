package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddWishlistItemRequest {
    private Long serviceId;

    public Long getServiceId() { return serviceId; }
    @JsonProperty("service_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

}
