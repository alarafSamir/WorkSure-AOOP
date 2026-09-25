package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmPaymentRequest {
    private Long bookingId;
    private String paymentIntentId;

    public Long getBookingId() { return bookingId; }
    @JsonProperty("booking_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getPaymentIntentId() { return paymentIntentId; }
    @JsonProperty("payment_intent_id")
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

}
