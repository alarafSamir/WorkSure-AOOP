package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCartItemRequest {
    private Long serviceId;
    private Integer quantity;
    private String scheduledAt;

    public Long getServiceId() { return serviceId; }
    @JsonProperty("service_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Integer getQuantity() { return quantity; }
    @JsonProperty("quantity")
    @JsonDeserialize(using = RequestDeserializers.OptionalInteger.class)
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getScheduledAt() { return scheduledAt; }
    @JsonProperty("scheduled_at")
    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

}
