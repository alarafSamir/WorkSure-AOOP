package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendChatMessageRequest {
    private String content = "";

    public String getContent() { return content; }
    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

}
