package com.Ayurnexa.Ayurnexa.model.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderAddress {

    private String pincode;
    private String landmark;
    private String building;

    @JsonProperty("phoneNumber")
    @JsonAlias({"phone_number", "phone"})
    private String phoneNumber;

    public OrderAddress() {}

    public OrderAddress(String pincode, String landmark, String building, String phoneNumber) {
        this.pincode = pincode;
        this.landmark = landmark;
        this.building = building;
        this.phoneNumber = phoneNumber;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
