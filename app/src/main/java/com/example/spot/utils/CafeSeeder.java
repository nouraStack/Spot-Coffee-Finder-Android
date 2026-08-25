package com.example.spot.utils;


import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CafeSeeder {

    private static final String TAG = "CafeSeeder";
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Cafes");

    public void seedCafes() {
        addCafe("Barn's Coffee", "King Fahd Road, Riyadh", 24.7136, 46.6753,
                "Famous specialty coffee shop with cozy atmosphere",
                Arrays.asList("Specialty Coffee", "WiFi", "Quiet"), "07:00", "01:00", 50, 30.0,
                "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800");

        addCafe("Elixir Bunn", "Olaya District, Riyadh", 24.6908, 46.6855,
                "Upscale cafe with modern design and professional coffee",
                Arrays.asList("Specialty Coffee", "Desserts", "Work Friendly"), "06:30", "00:30", 40, 35.0,
                "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800");

        addCafe("Brew92", "Tahlia Street, Jeddah", 21.5169, 39.2192,
                "Unique coffee experience in the heart of Jeddah",
                Arrays.asList("Specialty Coffee", "Breakfast", "Outdoor Terrace"), "07:00", "02:00", 60, 40.0,
                "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800");

        addCafe("Cafe Bateel", "Corniche, Jeddah", 21.5425, 39.1728,
                "Luxury cafe with beautiful sea view",
                Arrays.asList("Luxury", "Sea View", "Dates", "Desserts"), "08:00", "00:00", 45, 50.0,
                "https://images.unsplash.com/photo-1559305616-3f99cd43e353?w=800");

        addCafe("Dose Cafe", "Prince Mohammed Bin Fahd St, Dammam", 26.4207, 50.0888,
                "Modern cafe in Dammam with excellent specialty coffee",
                Arrays.asList("Specialty Coffee", "WiFi", "Study Friendly"), "06:00", "01:00", 55, 25.0,
                "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800");

        addCafe("The Roasters", "Corniche, Khobar", 26.2172, 50.1971,
                "Roastery and specialty cafe in Khobar",
                Arrays.asList("Roastery", "Specialty Coffee", "Corniche"), "07:00", "00:00", 35, 28.0,
                "https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=800");

        addCafe("Five Elephants", "Al Malqa District, Riyadh", 24.7743, 46.6529,
                "Quiet cafe perfect for remote work",
                Arrays.asList("Quiet", "WiFi", "Cake", "Work Friendly"), "08:00", "23:00", 30, 32.0,
                "https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=800");

        addCafe("Medd Cafe", "King Abdullah Road, Medina", 24.4672, 39.6024,
                "Modern cafe in Medina with comfortable atmosphere",
                Arrays.asList("Specialty Coffee", "WiFi", "Family Friendly"), "07:00", "01:00", 40, 22.0,
                "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=800");

        addCafe("Cloud Cafe", "King Faisal Road, Abha", 18.2164, 42.5053,
                "Cafe in cool Abha weather with mountain view",
                Arrays.asList("Mountain View", "Scenic", "Winter Vibe", "Quiet"), "09:00", "00:00", 25, 20.0,
                "https://images.unsplash.com/photo-1507914372368-b2b085b925a1?w=800");

        addCafe("Mug Cafe", "Shubra Street, Taif", 21.2703, 40.4158,
                "Youth-friendly cafe in Taif with affordable prices",
                Arrays.asList("Youth", "WiFi", "Affordable"), "08:00", "02:00", 45, 18.0,
                "https://images.unsplash.com/photo-1485182708500-e8f1f318ba72?w=800");

        addCafe("Camel Step", "Al Nakheel District, Riyadh", 24.7648, 46.6376,
                "Authentic Saudi specialty coffee brand",
                Arrays.asList("Saudi Brand", "Specialty Coffee", "Roastery"), "06:00", "00:00", 50, 27.0,
                "https://images.unsplash.com/photo-1504627298434-2119d32b7d37?w=800");

        addCafe("Nabt Fenjan", "Al Rawdah District, Jeddah", 21.5574, 39.1844,
                "One of the first specialty coffee roasters in Jeddah",
                Arrays.asList("Roastery", "Specialty Coffee", "Barista Training"), "07:00", "01:00", 35, 33.0,
                "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=800");

        addCafe("Cortado Coffee", "Al Yasmin District, Riyadh", 24.8231, 46.6382,
                "Simple and elegant cafe with excellent coffee",
                Arrays.asList("Coffee", "Simple", "Elegant"), "07:30", "23:30", 28, 25.0,
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800");

        addCafe("Flat White Cafe", "Al Yarmouk District, Khobar", 26.2785, 50.2083,
                "Specialists in flat white and milk-based drinks",
                Arrays.asList("Flat White", "Plant Milk", "WiFi"), "08:00", "00:00", 30, 29.0,
                "https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800");

        addCafe("Norte Cafe", "Prince Fahd Bin Sultan Road, Tabuk", 28.3838, 36.5550,
                "Best cafe in Tabuk with amazing winter vibes",
                Arrays.asList("Winter Vibe", "WiFi", "Quiet", "Work Friendly"), "08:00", "01:00", 35, 20.0,
                "https://images.unsplash.com/photo-1493857671505-72967e2e2760?w=800");
    }

    private void addCafe(String name, String address, double lat, double lng,
                         String description, List<String> tags,
                         String openTime, String closeTime,
                         int capacity, double pricePerHour, String imageUrl) {

        String cafeId = dbRef.push().getKey();
        if (cafeId == null) return;

        Map<String, Object> cafe = new HashMap<>();
        cafe.put("cafeId", cafeId);
        cafe.put("ownerId", "admin_seeder");
        cafe.put("name", name);
        cafe.put("address", address);
        cafe.put("latitude", lat);
        cafe.put("longitude", lng);
        cafe.put("description", description);
        cafe.put("tags", tags);
        cafe.put("openingTime", openTime);
        cafe.put("closingTime", closeTime);
        cafe.put("capacity", capacity);
        cafe.put("pricePerHour", pricePerHour);
        cafe.put("imageUrl", imageUrl);
        cafe.put("avgRating", 0);
        cafe.put("totalRatings", 0);

        dbRef.child(cafeId).setValue(cafe)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Added: " + name))
                .addOnFailureListener(e -> Log.e(TAG, "Failed: " + name + " - " + e.getMessage()));
    }
}
