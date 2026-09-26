package com.worksure.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Presence flags distinguish an omitted field (leave unchanged) from explicit null (clear it). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProfileRequest {
    private String fullName;
    private boolean fullNamePresent;
    private String phone;
    private boolean phonePresent;
    private String address;
    private boolean addressPresent;
    private String city;
    private boolean cityPresent;
    private String country;
    private boolean countryPresent;
    private BigDecimal latitude;
    private boolean latitudePresent;
    private BigDecimal longitude;
    private boolean longitudePresent;

    public String getFullName() { return fullName; }
    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.fullNamePresent = true;
    }
    public boolean hasFullName() { return fullNamePresent; }

    public String getPhone() { return phone; }
    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.phone = phone;
        this.phonePresent = true;
    }
    public boolean hasPhone() { return phonePresent; }

    public String getAddress() { return address; }
    @JsonProperty("address")
    public void setAddress(String address) {
        this.address = address;
        this.addressPresent = true;
    }
    public boolean hasAddress() { return addressPresent; }

    public String getCity() { return city; }
    @JsonProperty("city")
    public void setCity(String city) {
        this.city = city;
        this.cityPresent = true;
    }
    public boolean hasCity() { return cityPresent; }

    public String getCountry() { return country; }
    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
        this.countryPresent = true;
    }
    public boolean hasCountry() { return countryPresent; }

    public BigDecimal getLatitude() { return latitude; }
    @JsonProperty("latitude")
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
        this.latitudePresent = true;
    }
    public boolean hasLatitude() { return latitudePresent; }

    public BigDecimal getLongitude() { return longitude; }
    @JsonProperty("longitude")
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
        this.longitudePresent = true;
    }
    public boolean hasLongitude() { return longitudePresent; }

}
