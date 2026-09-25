package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Request fields only; authorization and business validation remain in the controller. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateServiceRequest {
    private String title = "";
    private String description = "";
    private Long categoryId;
    private BigDecimal basePrice;
    private Integer durationMinutes;
    private JsonNode images;
    private String tags;

    public String getTitle() { return title; }
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() { return categoryId; }
    @JsonProperty("category_id")
    @JsonDeserialize(using = RequestDeserializers.Identifier.class)
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getBasePrice() { return basePrice; }
    @JsonProperty("base_price")
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDurationMinutes() { return durationMinutes; }
    @JsonProperty("duration_minutes")
    @JsonDeserialize(using = RequestDeserializers.OptionalInteger.class)
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public JsonNode getImages() { return images; }
    @JsonProperty("images")
    public void setImages(JsonNode images) {
        this.images = images;
    }

    public String getTags() { return tags; }
    @JsonProperty("tags")
    public void setTags(String tags) {
        this.tags = tags;
    }

}
