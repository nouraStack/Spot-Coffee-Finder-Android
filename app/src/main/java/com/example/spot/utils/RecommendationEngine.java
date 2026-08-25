package com.example.spot.utils;

import android.location.Location;

import com.example.spot.models.Cafe;
import com.example.spot.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendationEngine {

    private static RecommendationEngine instance;

    private static final double WEIGHT_PREFERENCE = 0.35;
    private static final double WEIGHT_RATING = 0.25;
    private static final double WEIGHT_DISTANCE = 0.20;
    private static final double WEIGHT_POPULARITY = 0.10;
    private static final double WEIGHT_FAVORITE = 0.10;

    private RecommendationEngine() {
    }

    public static synchronized RecommendationEngine getInstance() {
        if (instance == null) {
            instance = new RecommendationEngine();
        }
        return instance;
    }

    public static class ScoredCafe {
        public Cafe cafe;
        public double score;
        public double distance;
        public String reason;

        public ScoredCafe(Cafe cafe, double score, double distance) {
            this.cafe = cafe;
            this.score = score;
            this.distance = distance;
            this.reason = "";
        }

        public ScoredCafe(Cafe cafe, double score, String reason) {
            this.cafe = cafe;
            this.score = score;
            this.distance = 0;
            this.reason = reason;
        }
    }

    public static List<ScoredCafe> getRecommendations(
            List<Cafe> cafes,
            User user,
            double userLat,
            double userLng,
            Set<String> favoriteCafeIds) {

        List<ScoredCafe> scoredCafes = new ArrayList<>();

        if (cafes == null || cafes.isEmpty()) return scoredCafes;

        List<String> userPrefs = user != null ? user.getPreferences() : new ArrayList<>();
        if (userPrefs == null) userPrefs = new ArrayList<>();
        if (favoriteCafeIds == null) favoriteCafeIds = new HashSet<>();

        for (Cafe cafe : cafes) {
            double prefScore = calculatePreferenceMatch(userPrefs, cafe.getTags());
            double ratingScore = cafe.getAvgRating() / 5.0;
            double distanceKm = calculateDistance(userLat, userLng, cafe.getLatitude(), cafe.getLongitude());
            double distanceScore = 1.0 / (1.0 + distanceKm);
            double popularityScore = Math.min(cafe.getTotalRatings(), 100) / 100.0;
            double favoriteScore = favoriteCafeIds.contains(cafe.getCafeId()) ? 1.0 : 0.0;

            double totalScore = (WEIGHT_PREFERENCE * prefScore)
                    + (WEIGHT_RATING * ratingScore)
                    + (WEIGHT_DISTANCE * distanceScore)
                    + (WEIGHT_POPULARITY * popularityScore)
                    + (WEIGHT_FAVORITE * favoriteScore);

            // Normalize to 0-10 scale
            totalScore = totalScore * 10;

            scoredCafes.add(new ScoredCafe(cafe, totalScore, distanceKm));
        }

        // Sort by score descending
        Collections.sort(scoredCafes, (a, b) -> Double.compare(b.score, a.score));

        return scoredCafes;
    }

    /**
     * Score cafes without location (for map view when location is not available)
     */
    public List<ScoredCafe> scoreCafes(List<Cafe> cafes, User user, Set<String> favoriteCafeIds) {
        List<ScoredCafe> scoredCafes = new ArrayList<>();

        if (cafes == null || cafes.isEmpty()) return scoredCafes;

        List<String> userPrefs = user != null ? user.getPreferences() : new ArrayList<>();
        if (userPrefs == null) userPrefs = new ArrayList<>();
        if (favoriteCafeIds == null) favoriteCafeIds = new HashSet<>();

        for (Cafe cafe : cafes) {
            double prefScore = calculatePreferenceMatch(userPrefs, cafe.getTags());
            double ratingScore = cafe.getAvgRating() / 5.0;
            double popularityScore = Math.min(cafe.getTotalRatings(), 100) / 100.0;
            double favoriteScore = favoriteCafeIds.contains(cafe.getCafeId()) ? 1.0 : 0.0;

            // Without distance, redistribute weights
            double totalScore = (0.45 * prefScore)  // Increased from 0.35
                    + (0.30 * ratingScore)          // Increased from 0.25
                    + (0.15 * popularityScore)      // Increased from 0.10
                    + (0.10 * favoriteScore);

            // Normalize to 0-10 scale
            totalScore = totalScore * 10;

            scoredCafes.add(new ScoredCafe(cafe, totalScore, 0));
        }

        // Sort by score descending
        Collections.sort(scoredCafes, (a, b) -> Double.compare(b.score, a.score));

        return scoredCafes;
    }

    private static double calculatePreferenceMatch(List<String> userPrefs, List<String> cafeTags) {
        if (userPrefs == null || userPrefs.isEmpty() || cafeTags == null || cafeTags.isEmpty()) {
            return 0.0;
        }

        Set<String> userPrefSet = new HashSet<>();
        for (String p : userPrefs) userPrefSet.add(p.toLowerCase().trim());

        Set<String> cafeTagSet = new HashSet<>();
        for (String t : cafeTags) cafeTagSet.add(t.toLowerCase().trim());

        int matches = 0;
        for (String pref : userPrefSet) {
            if (cafeTagSet.contains(pref)) matches++;
        }

        return (double) matches / userPrefSet.size();
    }

    private static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0; // convert to km
    }

    /**
     * Calculates how much a cafe matches the currently selected filter tags.
     * Returned value is 0-100. If no tag filters are selected, marker sizing can
     * fall back to the recommendation score.
     */
    public static double calculateTagMatchPercent(List<String> selectedTags, List<String> cafeTags) {
        if (selectedTags == null || selectedTags.isEmpty()) {
            return 0.0;
        }

        if (cafeTags == null || cafeTags.isEmpty()) {
            return 0.0;
        }

        Set<String> cafeTagSet = new HashSet<>();
        for (String tag : cafeTags) {
            if (tag != null) cafeTagSet.add(tag.toLowerCase().trim());
        }

        int matches = 0;
        for (String selectedTag : selectedTags) {
            if (selectedTag != null && cafeTagSet.contains(selectedTag.toLowerCase().trim())) {
                matches++;
            }
        }

        return (matches * 100.0) / selectedTags.size();
    }

    /**
     * Returns bubble size in dp based on active filter matching percentage.
     * 100% match -> largest marker, 50%+ match -> medium marker, below 50% -> small marker.
     */
    public static int getBubbleSizeByMatchPercent(double matchPercent) {
        if (matchPercent >= 100.0) return 90;
        if (matchPercent >= 50.0) return 65;
        return 42;
    }

    /**
     * Returns bubble size in dp based on score (0-10)
     * Large bubble: score >= 7 -> 80dp
     * Medium bubble: score >= 4 -> 60dp
     * Small bubble: score < 4 -> 40dp
     */
    public static int getBubbleSize(double score) {
        if (score >= 7.0) return 80;
        if (score >= 4.0) return 60;
        return 40;
    }

    /**
     * Returns alpha for marker based on score
     */
    public static float getBubbleAlpha(double score) {
        if (score >= 7.0) return 1.0f;
        if (score >= 4.0) return 0.85f;
        return 0.7f;
    }
}

