package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockPaymentRequest {
    private Long bookingId;
    private String provider = "";
    private String simulate = "success";

    public Long getBookingId() { return bookingId; }
    @JsonProperty("booking_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getProvider() { return provider; }
    @JsonProperty("provider")
    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSimulate() { return simulate; }
    @JsonProperty("simulate")
    public void setSimulate(String simulate) {
        this.simulate = simulate;
    }

}
