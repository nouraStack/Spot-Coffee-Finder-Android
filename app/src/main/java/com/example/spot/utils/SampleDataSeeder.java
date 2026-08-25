package com.example.spot.utils;

import com.example.spot.models.Cafe;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class to seed sample café data into Firebase Realtime Database.
 * Call SampleDataSeeder.seedCafes() once to populate the database with test data.
 */
public class SampleDataSeeder {

    public static void seedCafes() {
        List<Cafe> cafes = Arrays.asList(
                createCafe("The Cozy Corner", "123 Main St, New York",
                        40.7128, -74.0060,
                        "A warm and inviting café perfect for studying and quiet reading. Fresh pastries daily.",
                        Arrays.asList("Quiet Café", "Study Place", "WiFi Available", "Cozy Atmosphere"),
                        4.5, 87, "07:00", "22:00", 30, 15),

                createCafe("Espresso House", "456 Broadway, New York",
                        40.7160, -74.0055,
                        "Premium specialty coffee roasted in-house. Bold flavors and expert baristas.",
                        Arrays.asList("Strong Coffee", "Specialty Coffee", "WiFi Available"),
                        4.7, 124, "06:30", "21:00", 25, 20),

                createCafe("Garden Brew", "789 Park Ave, New York",
                        40.7200, -74.0020,
                        "Beautiful outdoor seating surrounded by greenery. Pet-friendly with organic options.",
                        Arrays.asList("Outdoor Seating", "Pet Friendly", "Cozy Atmosphere"),
                        4.2, 56, "08:00", "20:00", 40, 12),

                createCafe("The Study Spot", "321 University Pl, New York",
                        40.7100, -74.0080,
                        "Designed for students and remote workers. Power outlets everywhere, fast WiFi.",
                        Arrays.asList("Study Place", "WiFi Available", "Quiet Café"),
                        4.4, 203, "06:00", "00:00", 50, 10),

                createCafe("Velvet Bean", "555 Fifth Ave, New York",
                        40.7180, -74.0040,
                        "Elegant atmosphere with single-origin beans. Perfect for a premium coffee experience.",
                        Arrays.asList("Specialty Coffee", "Strong Coffee", "Cozy Atmosphere"),
                        4.8, 95, "07:30", "21:30", 20, 25),

                createCafe("Pawsome Café", "888 Pet Lane, New York",
                        40.7140, -74.0100,
                        "Bring your furry friends! Dog-friendly café with outdoor patio and treats for pets.",
                        Arrays.asList("Pet Friendly", "Outdoor Seating", "Cozy Atmosphere"),
                        4.1, 78, "09:00", "19:00", 35, 18),

                createCafe("Sunrise Roasters", "222 East Side, New York",
                        40.7090, -74.0030,
                        "Early bird special! Open before dawn with the freshest roasts in the city.",
                        Arrays.asList("Strong Coffee", "WiFi Available", "Study Place"),
                        4.3, 156, "05:00", "18:00", 30, 14),

                createCafe("Quiet Pages Café", "444 Library St, New York",
                        40.7220, -74.0070,
                        "A book-lover's paradise. Browse our collection while sipping artisan coffee.",
                        Arrays.asList("Quiet Café", "Study Place", "Cozy Atmosphere", "WiFi Available"),
                        4.6, 112, "08:00", "22:00", 25, 16)
        );

        for (Cafe cafe : cafes) {
            String key = FirebaseHelper.getInstance().getCafesRef().push().getKey();
            if (key != null) {
                cafe.setCafeId(key);
                FirebaseHelper.getInstance().getCafesRef().child(key).setValue(cafe);
            }
        }
    }

    private static Cafe createCafe(String name, String address, double lat, double lng,
                                    String description, List<String> tags,
                                    double rating, int totalRatings,
                                    String openTime, String closeTime,
                                    int capacity, double price) {
        Cafe cafe = new Cafe("", "", name, address, lat, lng, description, tags, price);
        cafe.setAvgRating(rating);
        cafe.setTotalRatings(totalRatings);
        cafe.setOpeningTime(openTime);
        cafe.setClosingTime(closeTime);
        cafe.setCapacity(capacity);
        return cafe;
    }
}

