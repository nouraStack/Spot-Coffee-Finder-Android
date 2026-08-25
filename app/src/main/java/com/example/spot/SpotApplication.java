package com.example.spot;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.spot.models.Cafe;
import com.example.spot.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class SpotApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Note: setPersistenceEnabled removed to avoid ANR on cold start.
        // Firebase Realtime Database offline caching is still available
        // via keepSynced() on individual references if needed.

        // Insert dummy cafes in Saudi Arabia if none exist
        insertDummyCafesIfNeeded();
    }

    private void insertDummyCafesIfNeeded() {
        DatabaseReference cafesRef = FirebaseHelper.getInstance().getCafesRef();
        cafesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    return; // Cafes already exist
                }

                // ========== DUMMY CAFES IN SAUDI ARABIA ==========
                Cafe c1 = new Cafe();
                c1.setCafeId("cafe_001");
                c1.setOwnerId("owner_001");
                c1.setOwnerEmail("owner1@example.com");
                c1.setName("مقهى الرياض بلازا");
                c1.setAddress("طريق الملك فهد، الرياض");
                c1.setLatitude(24.7136);
                c1.setLongitude(46.6753);
                c1.setImageUrl("https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400");
                c1.setDescription("مقهى فاخر في قلب الرياض يقدم أفخر أنواع القهوة العربية والعالمية.");
                c1.setTags(Arrays.asList("قهوة عربية", "واي فاي", "مكيف", "جلسات خارجية"));
                c1.setAvgRating(4.7);
                c1.setTotalRatings(120);
                c1.setOpeningTime("07:00");
                c1.setClosingTime("23:00");
                c1.setCapacity(45);
                c1.setPricePerHour(35.0);

                Cafe c2 = new Cafe();
                c2.setCafeId("cafe_002");
                c2.setOwnerId("owner_002");
                c2.setOwnerEmail("owner2@example.com");
                c2.setName("جدة كوفي هاوس");
                c2.setAddress("شارع التحلية، جدة");
                c2.setLatitude(21.4858);
                c2.setLongitude(39.1925);
                c2.setImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400");
                c2.setDescription("مقهى عصري على كورنيش جدة مع إطلالة رائعة على البحر الأحمر.");
                c2.setTags(Arrays.asList("إطلالة بحرية", "موسيقى حية", "حلويات", "قهوة مختصة"));
                c2.setAvgRating(4.5);
                c2.setTotalRatings(85);
                c2.setOpeningTime("08:00");
                c2.setClosingTime("01:00");
                c2.setCapacity(60);
                c2.setPricePerHour(40.0);

                Cafe c3 = new Cafe();
                c3.setCafeId("cafe_003");
                c3.setOwnerId("owner_003");
                c3.setOwnerEmail("owner3@example.com");
                c3.setName("الدمام بريوز");
                c3.setAddress("الكورنيش، الدمام");
                c3.setLatitude(26.4207);
                c3.setLongitude(50.0888);
                c3.setImageUrl("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400");
                c3.setDescription("مقهى مريح في الدمام يقدم مشروبات متنوعة ووجبات خفيفة لذيذة.");
                c3.setTags(Arrays.asList("مكتبة", "هادئ", "قهوة فرنسية", "كيك"));
                c3.setAvgRating(4.3);
                c3.setTotalRatings(64);
                c3.setOpeningTime("06:30");
                c3.setClosingTime("22:30");
                c3.setCapacity(30);
                c3.setPricePerHour(25.0);

                Cafe c4 = new Cafe();
                c4.setCafeId("cafe_004");
                c4.setOwnerId("owner_004");
                c4.setOwnerEmail("owner4@example.com");
                c4.setName("مكة كافيه البرج");
                c4.setAddress("شارع إبراهيم الخليل، مكة");
                c4.setLatitude(21.3891);
                c4.setLongitude(39.8579);
                c4.setImageUrl("https://images.unsplash.com/photo-1445116572660-236099ec97a0?w=400");
                c4.setDescription("أقرب مقهى إلى الحرم المكي، مثالي للاسترخاء بعد العمرة.");
                c4.setTags(Arrays.asList("قرب الحرم", "عائلي", "عصائر طازجة", "مخبوزات"));
                c4.setAvgRating(4.8);
                c4.setTotalRatings(200);
                c4.setOpeningTime("05:00");
                c4.setClosingTime("00:00");
                c4.setCapacity(80);
                c4.setPricePerHour(30.0);

                Cafe c5 = new Cafe();
                c5.setCafeId("cafe_005");
                c5.setOwnerId("owner_005");
                c5.setOwnerEmail("owner5@example.com");
                c5.setName("المدينة المنورة لاونج");
                c5.setAddress("المنطقة المركزية، المدينة المنورة");
                c5.setLatitude(24.5247);
                c5.setLongitude(39.5692);
                c5.setImageUrl("https://images.unsplash.com/photo-1511920170033-f8396924c348?w=400");
                c5.setDescription("مقهى أنيق في المدينة المنورة بتصميم إسلامي فاخر وأجواء هادئة.");
                c5.setTags(Arrays.asList("تصميم إسلامي", "شاي أخضر", "كعك تمر", "جلسات خاصة"));
                c5.setAvgRating(4.6);
                c5.setTotalRatings(98);
                c5.setOpeningTime("07:00");
                c5.setClosingTime("23:30");
                c5.setCapacity(50);
                c5.setPricePerHour(28.0);

                Cafe c6 = new Cafe();
                c6.setCafeId("cafe_006");
                c6.setOwnerId("owner_006");
                c6.setOwnerEmail("owner6@example.com");
                c6.setName("الخبر كوفي سبوت");
                c6.setAddress("الخبر الشمالية، شارع الأمير تركي");
                c6.setLatitude(26.2172);
                c6.setLongitude(50.1971);
                c6.setImageUrl("https://images.unsplash.com/photo-1498804103079-a6351b050096?w=400");
                c6.setDescription("مقهى شاب في الخبر يقدم قهوة مختصة وبيئة عمل مشتركة.");
                c6.setTags(Arrays.asList("كوفي وورك", "واي فاي سريع", "قهوة مختصة", "لابتوب فريندلي"));
                c6.setAvgRating(4.4);
                c6.setTotalRatings(55);
                c6.setOpeningTime("08:00");
                c6.setClosingTime("00:00");
                c6.setCapacity(35);
                c6.setPricePerHour(32.0);

                Cafe c7 = new Cafe();
                c7.setCafeId("cafe_007");
                c7.setOwnerId("owner_007");
                c7.setOwnerEmail("owner7@example.com");
                c7.setName("أبها هيلز كافيه");
                c7.setAddress("السودة، أبها");
                c7.setLatitude(18.2208);
                c7.setLongitude(42.5053);
                c7.setImageUrl("https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=400");
                c7.setDescription("مقهى في أعالي أبها مع جو بارد وإطلالة خلابة على الجبال الضبابية.");
                c7.setTags(Arrays.asList("جو بارد", "إطلالة جبلية", "شوكولاتة ساخنة", "ألعاب لوحية"));
                c7.setAvgRating(4.9);
                c7.setTotalRatings(150);
                c7.setOpeningTime("09:00");
                c7.setClosingTime("23:00");
                c7.setCapacity(40);
                c7.setPricePerHour(20.0);

                Cafe c8 = new Cafe();
                c8.setCafeId("cafe_008");
                c8.setOwnerId("owner_008");
                c8.setOwnerEmail("owner8@example.com");
                c8.setName("تبوك ساند كافيه");
                c8.setAddress("طريق الملك عبدالله، تبوك");
                c8.setLatitude(28.3835);
                c8.setLongitude(36.5662);
                c8.setImageUrl("https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=400");
                c8.setDescription("مقهى صحراوي أنيق في تبوك يقدم تجربة فريدة من نوعها.");
                c8.setTags(Arrays.asList("تصميم صحراوي", "قهوة تركية", "شيشة", "جلسات أرضية"));
                c8.setAvgRating(4.2);
                c8.setTotalRatings(42);
                c8.setOpeningTime("06:00");
                c8.setClosingTime("22:00");
                c8.setCapacity(25);
                c8.setPricePerHour(18.0);

                Cafe c9 = new Cafe();
                c9.setCafeId("cafe_009");
                c9.setOwnerId("owner_009");
                c9.setOwnerEmail("owner9@example.com");
                c9.setName("الطائف روز كافيه");
                c9.setAddress("الشفا، الطائف");
                c9.setLatitude(21.2854);
                c9.setLongitude(40.4262);
                c9.setImageUrl("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=400");
                c9.setDescription("مقهى رومانسي في الشفا مع إطلالة على مزارع الورد الطائفي.");
                c9.setTags(Arrays.asList("مزارع ورد", "قهوة فرنسية", "كيك ورد", "جلسات رومانسية"));
                c9.setAvgRating(4.7);
                c9.setTotalRatings(110);
                c9.setOpeningTime("08:00");
                c9.setClosingTime("22:00");
                c9.setCapacity(30);
                c9.setPricePerHour(22.0);

                Cafe c10 = new Cafe();
                c10.setCafeId("cafe_010");
                c10.setOwnerId("owner_010");
                c10.setOwnerEmail("owner10@example.com");
                c10.setName("بريدة كوفي كورنر");
                c10.setAddress("شارع الملك عبدالعزيز، بريدة");
                c10.setLatitude(26.3345);
                c10.setLongitude(43.9740);
                c10.setImageUrl("https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=400");
                c10.setDescription("مقهى acogedor في بريدة يقدم أجود أنواع القهوة المحمصة محلياً.");
                c10.setTags(Arrays.asList("تحميص محلي", "كباتشينو", "كرواسان", "تصميم مودرن"));
                c10.setAvgRating(4.5);
                c10.setTotalRatings(77);
                c10.setOpeningTime("07:30");
                c10.setClosingTime("23:00");
                c10.setCapacity(38);
                c10.setPricePerHour(26.0);

                Cafe c11 = new Cafe();
                c11.setCafeId("cafe_011");
                c11.setOwnerId("owner_011");
                c11.setOwnerEmail("owner11@example.com");
                c11.setName("حائل كافيه");
                c11.setAddress("وسط حائل، شارع البطحاء");
                c11.setLatitude(27.5114);
                c11.setLongitude(41.7208);
                c11.setImageUrl("https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=400");
                c11.setDescription("مقهى تقليدي في حائل يجمع بين الأصالة والمعاصرة.");
                c11.setTags(Arrays.asList("تراثي", "قهوة سعودية", "تمور حائل", "جلسات عائلية"));
                c11.setAvgRating(4.3);
                c11.setTotalRatings(50);
                c11.setOpeningTime("06:00");
                c11.setClosingTime("21:00");
                c11.setCapacity(32);
                c11.setPricePerHour(20.0);

                Cafe c12 = new Cafe();
                c12.setCafeId("cafe_012");
                c12.setOwnerId("owner_012");
                c12.setOwnerEmail("owner12@example.com");
                c12.setName("نجران كوفي لاند");
                c12.setAddress("حي الفيصلية، نجران");
                c12.setLatitude(17.5656);
                c12.setLongitude(44.2289);
                c12.setImageUrl("https://images.unsplash.com/photo-1525193612562-0ec53b0e5d7c?w=400");
                c12.setDescription("مقهى واسع في نجران مناسب للعائلات والمجموعات الكبيرة.");
                c12.setTags(Arrays.asList("عائلي", "مساحة واسعة", "ألعاب أطفال", "بوفيه"));
                c12.setAvgRating(4.1);
                c12.setTotalRatings(38);
                c12.setOpeningTime("07:00");
                c12.setClosingTime("23:00");
                c12.setCapacity(70);
                c12.setPricePerHour(24.0);

                // Push all cafes to Firebase
                cafesRef.child(c1.getCafeId()).setValue(c1);
                cafesRef.child(c2.getCafeId()).setValue(c2);
                cafesRef.child(c3.getCafeId()).setValue(c3);
                cafesRef.child(c4.getCafeId()).setValue(c4);
                cafesRef.child(c5.getCafeId()).setValue(c5);
                cafesRef.child(c6.getCafeId()).setValue(c6);
                cafesRef.child(c7.getCafeId()).setValue(c7);
                cafesRef.child(c8.getCafeId()).setValue(c8);
                cafesRef.child(c9.getCafeId()).setValue(c9);
                cafesRef.child(c10.getCafeId()).setValue(c10);
                cafesRef.child(c11.getCafeId()).setValue(c11);
                cafesRef.child(c12.getCafeId()).setValue(c12);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }
}
