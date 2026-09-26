package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBookingRequest {
    private Long serviceId;
    private String scheduledAt = "";
    private String address = "";
    private String notes = "";

    public Long getServiceId() { return serviceId; }
    @JsonProperty("service_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getScheduledAt() { return scheduledAt; }
    @JsonProperty("scheduled_at")
    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getAddress() { return address; }
    @JsonProperty("address")
    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() { return notes; }
    @JsonProperty("notes")
    public void setNotes(String notes) {
        this.notes = notes;
    }

}
