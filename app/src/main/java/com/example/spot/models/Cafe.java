package com.example.spot.models;

import java.util.ArrayList;
import java.util.List;

public class Cafe {
    private String cafeId;
    private String ownerId;
    private String ownerEmail;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String description;
    private List<String> tags;
    private double avgRating;
    private int totalRatings;
    private String openingTime;
    private String closingTime;
    private int capacity;
    private double pricePerHour;
    private String phoneNumber;

    public Cafe() {
        tags = new ArrayList<>();
    }

    public Cafe(String cafeId, String ownerId, String name, String address,
                double latitude, double longitude, String description,
                List<String> tags, double pricePerHour) {
        this.cafeId = cafeId;
        this.ownerId = ownerId;
        this.ownerEmail = "";
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.pricePerHour = pricePerHour;
        this.avgRating = 0;
        this.totalRatings = 0;
    }

    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }
    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
