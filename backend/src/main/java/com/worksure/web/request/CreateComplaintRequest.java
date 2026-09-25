package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateComplaintRequest {
    private String subject = "";
    private String message = "";
    private Long subjectUserId;
    private Long bookingId;

    public String getSubject() { return subject; }
    @JsonProperty("subject")
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() { return message; }
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSubjectUserId() { return subjectUserId; }
    @JsonProperty("subject_user_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setSubjectUserId(Long subjectUserId) {
        this.subjectUserId = subjectUserId;
    }

    public Long getBookingId() { return bookingId; }
    @JsonProperty("booking_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

}
