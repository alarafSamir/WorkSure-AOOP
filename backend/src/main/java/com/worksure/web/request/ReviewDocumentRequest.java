package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewDocumentRequest {
    private String status;
    private String adminNote;

    public String getStatus() { return status; }
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNote() { return adminNote; }
    @JsonProperty("admin_note")
    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

}
