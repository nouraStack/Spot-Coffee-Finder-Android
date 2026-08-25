package com.example.spot.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phoneNumber;
    private String role; // "customer" or "provider"
    private List<String> preferences;
    private Map<String, Boolean> favorites;

    public User() {
        preferences = new ArrayList<>();
        favorites = new HashMap<>();
    }

    public User(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.preferences = new ArrayList<>();
        this.favorites = new HashMap<>();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<String> getPreferences() { return preferences; }
    public void setPreferences(List<String> preferences) { this.preferences = preferences; }
    public Map<String, Boolean> getFavorites() { return favorites; }
    public void setFavorites(Map<String, Boolean> favorites) { this.favorites = favorites; }

    // Helper method to get favorite cafe IDs as a list
    public List<String> getFavoriteCafeIds() {
        List<String> favoriteCafeIds = new ArrayList<>();
        if (favorites != null) {
            for (Map.Entry<String, Boolean> entry : favorites.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    favoriteCafeIds.add(entry.getKey());
                }
            }
        }
        return favoriteCafeIds;
    }
}
