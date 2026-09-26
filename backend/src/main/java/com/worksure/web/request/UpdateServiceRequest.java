package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Presence flags preserve partial updates; category and ownership are deliberately not editable. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateServiceRequest {
    private String title;
    private boolean titlePresent;
    private String description;
    private boolean descriptionPresent;
    private BigDecimal basePrice;
    private boolean basePricePresent;
    private Integer durationMinutes;
    private boolean durationMinutesPresent;
    private String tags;
    private boolean tagsPresent;
    private Boolean isActive;
    private boolean isActivePresent;
    private JsonNode images;
    private boolean imagesPresent;

    public String getTitle() { return title; }
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }
    public boolean hasTitle() { return titlePresent; }

    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }
    public boolean hasDescription() { return descriptionPresent; }

    public BigDecimal getBasePrice() { return basePrice; }
    @JsonProperty("base_price")
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
        this.basePricePresent = true;
    }
    public boolean hasBasePrice() { return basePricePresent; }

    public Integer getDurationMinutes() { return durationMinutes; }
    @JsonProperty("duration_minutes")
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
        this.durationMinutesPresent = true;
    }
    public boolean hasDurationMinutes() { return durationMinutesPresent; }

    public String getTags() { return tags; }
    @JsonProperty("tags")
    public void setTags(String tags) {
        this.tags = tags;
        this.tagsPresent = true;
    }
    public boolean hasTags() { return tagsPresent; }

    public Boolean getIsActive() { return isActive; }
    @JsonProperty("is_active")
    @JsonDeserialize(using = RequestDeserializers.LegacyBoolean.class)
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.isActivePresent = true;
    }
    public boolean hasIsActive() { return isActivePresent; }

    public JsonNode getImages() { return images; }
    @JsonProperty("images")
    public void setImages(JsonNode images) {
        this.images = images;
        this.imagesPresent = true;
    }
    public boolean hasImages() { return imagesPresent; }

}
