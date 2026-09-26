package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;

/** Presence flags preserve partial updates. Availability keeps its existing flexible JSON format. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateWorkerProfileRequest {
    private String headline;
    private boolean headlinePresent;
    private String bio;
    private boolean bioPresent;
    private BigDecimal hourlyRate;
    private boolean hourlyRatePresent;
    private Integer serviceRadiusKm;
    private boolean serviceRadiusKmPresent;
    private Integer yearsExperience;
    private boolean yearsExperiencePresent;
    private JsonNode availability;
    private boolean availabilityPresent;

    public String getHeadline() { return headline; }
    @JsonProperty("headline")
    public void setHeadline(String headline) {
        this.headline = headline;
        this.headlinePresent = true;
    }
    public boolean hasHeadline() { return headlinePresent; }

    public String getBio() { return bio; }
    @JsonProperty("bio")
    public void setBio(String bio) {
        this.bio = bio;
        this.bioPresent = true;
    }
    public boolean hasBio() { return bioPresent; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    @JsonProperty("hourly_rate")
    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.hourlyRatePresent = true;
    }
    public boolean hasHourlyRate() { return hourlyRatePresent; }

    public Integer getServiceRadiusKm() { return serviceRadiusKm; }
    @JsonProperty("service_radius_km")
    public void setServiceRadiusKm(Integer serviceRadiusKm) {
        this.serviceRadiusKm = serviceRadiusKm;
        this.serviceRadiusKmPresent = true;
    }
    public boolean hasServiceRadiusKm() { return serviceRadiusKmPresent; }

    public Integer getYearsExperience() { return yearsExperience; }
    @JsonProperty("years_experience")
    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
        this.yearsExperiencePresent = true;
    }
    public boolean hasYearsExperience() { return yearsExperiencePresent; }

    public JsonNode getAvailability() { return availability; }
    @JsonProperty("availability")
    public void setAvailability(JsonNode availability) {
        this.availability = availability;
        this.availabilityPresent = true;
    }
    public boolean hasAvailability() { return availabilityPresent; }

}
