package com.example.spot.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spot.R;
import com.example.spot.adapters.CafeCardAdapter;
import com.example.spot.adapters.CafeListAdapter;
import com.example.spot.databinding.FragmentHomeBinding;
import com.example.spot.models.Cafe;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.example.spot.utils.RecommendationEngine;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CafeCardAdapter recommendedAdapter;
    private CafeListAdapter nearbyAdapter;
    private final List<RecommendationEngine.ScoredCafe> recommendedCafes = new ArrayList<>();
    private final List<RecommendationEngine.ScoredCafe> nearbyCafes = new ArrayList<>();
    private final List<RecommendationEngine.ScoredCafe> allScoredCafes = new ArrayList<>();
    private User currentUser;
    private final Set<String> favoriteCafeIds = new HashSet<>();
    private String currentSearchQuery = "";

    // Stored time selection
    private String selectedDate;
    private String selectedStartTime;
    private String selectedEndTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerViews();
        setupListeners();
        loadUserData();
    }

    private void setupRecyclerViews() {
        recommendedAdapter = new CafeCardAdapter(recommendedCafes, scoredCafe -> {
            Bundle bundle = new Bundle();
            bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_home_to_cafeDetail, bundle);
        });
        binding.rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecommended.setAdapter(recommendedAdapter);

        nearbyAdapter = new CafeListAdapter(nearbyCafes, new CafeListAdapter.OnCafeClickListener() {
            @Override
            public void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe) {
                Bundle bundle = new Bundle();
                bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_cafeDetail, bundle);
            }

            @Override
            public void onBookClick(RecommendationEngine.ScoredCafe scoredCafe) {
                Bundle bundle = new Bundle();
                bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_home_to_cafeDetail, bundle);
            }
        });
        binding.rvNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNearby.setAdapter(nearbyAdapter);
    }

    private void setupListeners() {
        binding.btnSelectTime.setOnClickListener(v -> Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_timeSelection));

        binding.tvViewAll.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("searchQuery", currentSearchQuery);
            bundle.putString("selectedDate", selectedDate);
            bundle.putString("selectedStartTime", selectedStartTime);
            bundle.putString("selectedEndTime", selectedEndTime);
            Navigation.findNavController(requireView())
                    .navigate(R.id.allCafesFragment, bundle);
        });

        binding.btnClearTime.setOnClickListener(v -> {
            selectedDate = null;
            selectedStartTime = null;
            selectedEndTime = null;
            binding.cardSelectedTime.setVisibility(View.GONE);
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                if (binding == null) return;
                binding.btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
        });

        // Check if we got time selection data back
        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate");
            selectedStartTime = getArguments().getString("selectedStartTime");
            selectedEndTime = getArguments().getString("selectedEndTime");
            if (selectedDate != null && selectedStartTime != null && selectedEndTime != null) {
                binding.cardSelectedTime.setVisibility(View.VISIBLE);
                binding.tvSelectedTime.setText(getString(
                        R.string.selected_time_format,
                        selectedDate,
                        selectedStartTime,
                        selectedEndTime));
            }
        }
    }

    private void loadUserData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) {
            // No logged-in user: just load cafes without user preferences
            loadCafes();
            return;
        }

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        currentUser = snapshot.getValue(User.class);
                        if (currentUser != null && currentUser.getName() != null) {
                            binding.tvGreeting.setText(getString(R.string.home_greeting, currentUser.getName()));
                        }
                        loadFavorites();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        loadCafes();
                    }
                });
    }

    private void loadFavorites() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) {
            loadCafes();
            return;
        }
        FirebaseHelper.getInstance().getFavoritesRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoriteCafeIds.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            favoriteCafeIds.add(child.getKey());
                        }
                        loadCafes();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadCafes();
                    }
                });
    }

    private void loadCafes() {
        FirebaseHelper.getInstance().getCafesRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);

                        List<Cafe> allCafes = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Cafe cafe = child.getValue(Cafe.class);
                            if (cafe != null) {
                                if (cafe.getCafeId() == null || cafe.getCafeId().isEmpty()) {
                                    cafe.setCafeId(child.getKey());
                                }
                                allCafes.add(cafe);
                            }
                        }

                        // If Firebase is empty, use local dummy data
                        if (allCafes.isEmpty()) {
                            allCafes.addAll(getDummyCafes());
                        }

                        allScoredCafes.clear();
                        recommendedCafes.clear();
                        nearbyCafes.clear();

                        double userLat = 24.7136;
                        double userLng = 46.6753;

                        List<RecommendationEngine.ScoredCafe> scored =
                                RecommendationEngine.getRecommendations(
                                        allCafes, currentUser, userLat, userLng, favoriteCafeIds);

                        allScoredCafes.addAll(scored);
                        applyFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        // Use local dummy data on error too
                        List<Cafe> allCafes = getDummyCafes();
                        allScoredCafes.clear();
                        recommendedCafes.clear();
                        nearbyCafes.clear();

                        double userLat = 24.7136;
                        double userLng = 46.6753;

                        List<RecommendationEngine.ScoredCafe> scored =
                                RecommendationEngine.getRecommendations(
                                        allCafes, currentUser, userLat, userLng, favoriteCafeIds);

                        allScoredCafes.addAll(scored);
                        applyFilters();
                    }
                });
    }

    private List<Cafe> getDummyCafes() {
        List<Cafe> cafes = new ArrayList<>();

        Cafe c1 = new Cafe("cafe_001", "owner_001", "مقهى الرياض بلازا",
                "طريق الملك فهد، الرياض", 24.7136, 46.6753,
                "مقهى فاخر في قلب الرياض يقدم أفخر أنواع القهوة العربية والعالمية.",
                java.util.Arrays.asList("قهوة عربية", "واي فاي", "مكيف", "جلسات خارجية"), 35.0);
        c1.setImageUrl("https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400");
        c1.setAvgRating(4.7); c1.setTotalRatings(120);
        c1.setOpeningTime("07:00"); c1.setClosingTime("23:00"); c1.setCapacity(45);
        cafes.add(c1);

        Cafe c2 = new Cafe("cafe_002", "owner_002", "جدة كوفي هاوس",
                "شارع التحلية، جدة", 21.4858, 39.1925,
                "مقهى عصري على كورنيش جدة مع إطلالة رائعة على البحر الأحمر.",
                java.util.Arrays.asList("إطلالة بحرية", "موسيقى حية", "حلويات", "قهوة مختصة"), 40.0);
        c2.setImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400");
        c2.setAvgRating(4.5); c2.setTotalRatings(85);
        c2.setOpeningTime("08:00"); c2.setClosingTime("01:00"); c2.setCapacity(60);
        cafes.add(c2);

        Cafe c3 = new Cafe("cafe_003", "owner_003", "الدمام بريوز",
                "الكورنيش، الدمام", 26.4207, 50.0888,
                "مقهى مريح في الدمام يقدم مشروبات متنوعة ووجبات خفيفة لذيذة.",
                java.util.Arrays.asList("مكتبة", "هادئ", "قهوة فرنسية", "كيك"), 25.0);
        c3.setImageUrl("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400");
        c3.setAvgRating(4.3); c3.setTotalRatings(64);
        c3.setOpeningTime("06:30"); c3.setClosingTime("22:30"); c3.setCapacity(30);
        cafes.add(c3);

        Cafe c4 = new Cafe("cafe_004", "owner_004", "مكة كافيه البرج",
                "شارع إبراهيم الخليل، مكة", 21.3891, 39.8579,
                "أقرب مقهى إلى الحرم المكي، مثالي للاسترخاء بعد العمرة.",
                java.util.Arrays.asList("قرب الحرم", "عائلي", "عصائر طازجة", "مخبوزات"), 30.0);
        c4.setImageUrl("https://images.unsplash.com/photo-1445116572660-236099ec97a0?w=400");
        c4.setAvgRating(4.8); c4.setTotalRatings(200);
        c4.setOpeningTime("05:00"); c4.setClosingTime("00:00"); c4.setCapacity(80);
        cafes.add(c4);

        Cafe c5 = new Cafe("cafe_005", "owner_005", "المدينة المنورة لاونج",
                "المنطقة المركزية، المدينة المنورة", 24.5247, 39.5692,
                "مقهى أنيق في المدينة المنورة بتصميم إسلامي فاخر وأجواء هادئة.",
                java.util.Arrays.asList("تصميم إسلامي", "شاي أخضر", "كعك تمر", "جلسات خاصة"), 28.0);
        c5.setImageUrl("https://images.unsplash.com/photo-1511920170033-f8396924c348?w=400");
        c5.setAvgRating(4.6); c5.setTotalRatings(98);
        c5.setOpeningTime("07:00"); c5.setClosingTime("23:30"); c5.setCapacity(50);
        cafes.add(c5);

        Cafe c6 = new Cafe("cafe_006", "owner_006", "الخبر كوفي سبوت",
                "الخبر الشمالية، شارع الأمير تركي", 26.2172, 50.1971,
                "مقهى شاب في الخبر يقدم قهوة مختصة وبيئة عمل مشتركة.",
                java.util.Arrays.asList("كوفي وورك", "واي فاي سريع", "قهوة مختصة", "لابتوب فريندلي"), 32.0);
        c6.setImageUrl("https://images.unsplash.com/photo-1498804103079-a6351b050096?w=400");
        c6.setAvgRating(4.4); c6.setTotalRatings(55);
        c6.setOpeningTime("08:00"); c6.setClosingTime("00:00"); c6.setCapacity(35);
        cafes.add(c6);

        Cafe c7 = new Cafe("cafe_007", "owner_007", "أبها هيلز كافيه",
                "السودة، أبها", 18.2208, 42.5053,
                "مقهى في أعالي أبها مع جو بارد وإطلالة خلابة على الجبال الضبابية.",
                java.util.Arrays.asList("جو بارد", "إطلالة جبلية", "شوكولاتة ساخنة", "ألعاب لوحية"), 20.0);
        c7.setImageUrl("https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=400");
        c7.setAvgRating(4.9); c7.setTotalRatings(150);
        c7.setOpeningTime("09:00"); c7.setClosingTime("23:00"); c7.setCapacity(40);
        cafes.add(c7);

        Cafe c8 = new Cafe("cafe_008", "owner_008", "تبوك ساند كافيه",
                "طريق الملك عبدالله، تبوك", 28.3835, 36.5662,
                "مقهى صحراوي أنيق في تبوك يقدم تجربة فريدة من نوعها.",
                java.util.Arrays.asList("تصميم صحراوي", "قهوة تركية", "شيشة", "جلسات أرضية"), 18.0);
        c8.setImageUrl("https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=400");
        c8.setAvgRating(4.2); c8.setTotalRatings(42);
        c8.setOpeningTime("06:00"); c8.setClosingTime("22:00"); c8.setCapacity(25);
        cafes.add(c8);

        Cafe c9 = new Cafe("cafe_009", "owner_009", "الطائف روز كافيه",
                "الشفا، الطائف", 21.2854, 40.4262,
                "مقهى رومانسي في الشفا مع إطلالة على مزارع الورد الطائفي.",
                java.util.Arrays.asList("مزارع ورد", "قهوة فرنسية", "كيك ورد", "جلسات رومانسية"), 22.0);
        c9.setImageUrl("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=400");
        c9.setAvgRating(4.7); c9.setTotalRatings(110);
        c9.setOpeningTime("08:00"); c9.setClosingTime("22:00"); c9.setCapacity(30);
        cafes.add(c9);

        Cafe c10 = new Cafe("cafe_010", "owner_010", "بريدة كوفي كورنر",
                "شارع الملك عبدالعزيز، بريدة", 26.3345, 43.9740,
                "مقهى مريح في بريدة يقدم أجود أنواع القهوة المحمصة محلياً.",
                java.util.Arrays.asList("تحميص محلي", "كباتشينو", "كرواسان", "تصميم مودرن"), 26.0);
        c10.setImageUrl("https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=400");
        c10.setAvgRating(4.5); c10.setTotalRatings(77);
        c10.setOpeningTime("07:30"); c10.setClosingTime("23:00"); c10.setCapacity(38);
        cafes.add(c10);

        Cafe c11 = new Cafe("cafe_011", "owner_011", "حائل كافيه",
                "وسط حائل، شارع البطحاء", 27.5114, 41.7208,
                "مقهى تقليدي في حائل يجمع بين الأصالة والمعاصرة.",
                java.util.Arrays.asList("تراثي", "قهوة سعودية", "تمور حائل", "جلسات عائلية"), 20.0);
        c11.setImageUrl("https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=400");
        c11.setAvgRating(4.3); c11.setTotalRatings(50);
        c11.setOpeningTime("06:00"); c11.setClosingTime("21:00"); c11.setCapacity(32);
        cafes.add(c11);

        Cafe c12 = new Cafe("cafe_012", "owner_012", "نجران كوفي لاند",
                "حي الفيصلية، نجران", 17.5656, 44.2289,
                "مقهى واسع في نجران مناسب للعائلات والمجموعات الكبيرة.",
                java.util.Arrays.asList("عائلي", "مساحة واسعة", "ألعاب أطفال", "بوفيه"), 24.0);
        c12.setImageUrl("https://images.unsplash.com/photo-1525193612562-0ec53b0e5d7c?w=400");
        c12.setAvgRating(4.1); c12.setTotalRatings(38);
        c12.setOpeningTime("07:00"); c12.setClosingTime("23:00"); c12.setCapacity(70);
        cafes.add(c12);

        return cafes;
    }

    private void applyFilters() {
        if (binding == null) return;

        List<RecommendationEngine.ScoredCafe> filtered = new ArrayList<>();
        for (RecommendationEngine.ScoredCafe scoredCafe : allScoredCafes) {
            if (matchesSearch(scoredCafe, currentSearchQuery)) {
                filtered.add(scoredCafe);
            }
        }

        recommendedCafes.clear();
        nearbyCafes.clear();

        for (int i = 0; i < Math.min(5, filtered.size()); i++) {
            recommendedCafes.add(filtered.get(i));
        }
        nearbyCafes.addAll(filtered);

        recommendedAdapter.notifyDataSetChanged();
        nearbyAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesSearch(RecommendationEngine.ScoredCafe scoredCafe, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        Cafe cafe = scoredCafe.cafe;
        String normalizedQuery = query.trim().toLowerCase(Locale.US);
        if (contains(cafe.getName(), normalizedQuery)
                || contains(cafe.getDescription(), normalizedQuery)
                || contains(cafe.getAddress(), normalizedQuery)) {
            return true;
        }

        List<String> tags = cafe.getTags();
        if (tags != null) {
            for (String tag : tags) {
                if (contains(tag, normalizedQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.US).contains(query);
    }

    private void updateEmptyState() {
        if (binding == null) return;
        boolean hasResults = !nearbyCafes.isEmpty() || !recommendedCafes.isEmpty();
        binding.tvEmpty.setVisibility(hasResults ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
