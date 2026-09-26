package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateReviewRequest {
    private Long bookingId;
    private Integer rating;
    private String comment;

    public Long getBookingId() { return bookingId; }
    @JsonProperty("booking_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getRating() { return rating; }
    @JsonProperty("rating")
    @JsonDeserialize(using = RequestDeserializers.ReviewRating.class)
    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() { return comment; }
    @JsonProperty("comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

}
