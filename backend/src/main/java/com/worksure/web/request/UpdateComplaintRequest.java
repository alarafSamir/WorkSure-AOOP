package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateComplaintRequest {
    private String status;
    private String resolutionNote;

    public String getStatus() { return status; }
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNote() { return resolutionNote; }
    @JsonProperty("resolution_note")
    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

}
