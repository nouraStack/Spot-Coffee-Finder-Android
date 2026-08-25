package com.example.spot.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.spot.R;
import com.example.spot.adapters.CafeCardAdapter;
import com.example.spot.adapters.CafeListAdapter;
import com.example.spot.databinding.FragmentGoogleMapBinding;
import com.example.spot.models.Cafe;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.example.spot.utils.RecommendationEngine;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GoogleMapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentGoogleMapBinding binding;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private BottomSheetBehavior<View> filterBottomSheetBehavior;

    private User currentUser;
    private Set<String> favoriteCafeIds = new HashSet<>();

    private List<RecommendationEngine.ScoredCafe> allScoredCafes = new ArrayList<>();
    private List<RecommendationEngine.ScoredCafe> displayedScoredCafes = new ArrayList<>();
    private List<RecommendationEngine.ScoredCafe> recommendedCafes = new ArrayList<>();
    private List<RecommendationEngine.ScoredCafe> nearbyCafes = new ArrayList<>();

    private Map<Marker, RecommendationEngine.ScoredCafe> markerCafeMap = new HashMap<>();

    private final Set<String> activeTagFilters = new HashSet<>();
    private final Set<String> detailedCategoryFilters = new HashSet<>();
    private final Map<Integer, String> quickChipTagMap = new HashMap<>();

    private double minRatingFilter = 0.0;
    private double maxPriceFilter = 200.0;
    private double maxDistanceFilter = 2000.0;

    private Location userLocation;

    private CafeCardAdapter recommendedAdapter;
    private CafeListAdapter nearbyAdapter;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final double NEARBY_RADIUS_KM = 5.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGoogleMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            MapsInitializer.initialize(requireContext());
        } catch (Exception ignored) {
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupBottomSheet();
        setupRecyclerViews();
        setupClickListeners();
        setupDetailedFilters();
        setupSearchView();
        setupQuickFilterChips();
        loadUserAndCafes();
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        filterBottomSheetBehavior = BottomSheetBehavior.from(binding.filterBottomSheet);
        filterBottomSheetBehavior.setHideable(true);
        filterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        filterBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    showMapControls();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    private void setupRecyclerViews() {
        binding.rvRecommended.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendedAdapter = new CafeCardAdapter(recommendedCafes, this::onCafeClick);
        binding.rvRecommended.setAdapter(recommendedAdapter);

        binding.rvNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyAdapter = new CafeListAdapter(nearbyCafes, this::onCafeClick);
        binding.rvNearby.setAdapter(nearbyAdapter);
    }

    private void setupClickListeners() {
        binding.fabMyLocation.setOnClickListener(v -> moveToUserLocation());
        binding.btnLocation.setOnClickListener(v -> moveToUserLocation());

        binding.btnFilter.setOnClickListener(v -> {
            if (filterBottomSheetBehavior == null) return;

            if (filterBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                hideMapControls();
                filterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                filterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                showMapControls();
            }
        });
    }

    private void setupDetailedFilters() {
        binding.sliderRating.setProgress((int) minRatingFilter);
        binding.sliderPrice.setProgress((int) maxPriceFilter);
        binding.sliderDistance.setProgress((int) maxDistanceFilter);

        binding.tvRatingValue.setText(String.valueOf((int) minRatingFilter));
        binding.tvPriceValue.setText((int) maxPriceFilter + " SAR");
        binding.tvDistanceValue.setText((int) maxDistanceFilter + " km");

        binding.sliderRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvRatingValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.sliderPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvPriceValue.setText(progress + " SAR");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.sliderDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvDistanceValue.setText(progress + " km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.btnApplyFilters.setOnClickListener(v -> {
            minRatingFilter = binding.sliderRating.getProgress();
            maxPriceFilter = binding.sliderPrice.getProgress();
            maxDistanceFilter = binding.sliderDistance.getProgress();

            detailedCategoryFilters.clear();

            if (binding.filterQuiet.isChecked()) detailedCategoryFilters.add("quiet café");
            if (binding.filterStrong.isChecked()) detailedCategoryFilters.add("strong coffee");
            if (binding.filterStudy.isChecked()) detailedCategoryFilters.add("study place");
            if (binding.filterOutdoor.isChecked()) detailedCategoryFilters.add("outdoor seating");
            if (binding.filterWifi.isChecked()) detailedCategoryFilters.add("wifi available");
            if (binding.filterPet.isChecked()) detailedCategoryFilters.add("pet friendly");
            if (binding.filterSpecialty.isChecked()) detailedCategoryFilters.add("specialty coffee");
            if (binding.filterCozy.isChecked()) detailedCategoryFilters.add("cozy atmosphere");

            applySearchFilter(binding.searchView.getQuery() == null
                    ? ""
                    : binding.searchView.getQuery().toString());

            filterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            showMapControls();
        });

        binding.btnResetFilters.setOnClickListener(v -> {
            minRatingFilter = 0.0;
            maxDistanceFilter = 2000.0;
            binding.sliderDistance.setProgress(2000);

            detailedCategoryFilters.clear();

            binding.sliderRating.setProgress(0);
            binding.sliderPrice.setProgress(200);
            binding.sliderDistance.setProgress(50);

            binding.filterQuiet.setChecked(false);
            binding.filterStrong.setChecked(false);
            binding.filterStudy.setChecked(false);
            binding.filterOutdoor.setChecked(false);
            binding.filterWifi.setChecked(false);
            binding.filterPet.setChecked(false);
            binding.filterSpecialty.setChecked(false);
            binding.filterCozy.setChecked(false);

            applySearchFilter(binding.searchView.getQuery() == null
                    ? ""
                    : binding.searchView.getQuery().toString());

            filterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            showMapControls();
        });
    }

    private void hideMapControls() {
        binding.searchBarCard.setVisibility(View.GONE);
        binding.chipScroll.setVisibility(View.GONE);
        binding.fabMyLocation.setVisibility(View.GONE);
    }

    private void showMapControls() {
        binding.searchBarCard.setVisibility(View.VISIBLE);
        binding.chipScroll.setVisibility(View.VISIBLE);
        binding.fabMyLocation.setVisibility(View.VISIBLE);
    }

    private void setupQuickFilterChips() {
        quickChipTagMap.put(R.id.chip_filter_quiet, "Quiet Café");
        quickChipTagMap.put(R.id.chip_filter_strong_coffee, "Strong Coffee");
        quickChipTagMap.put(R.id.chip_filter_study, "Study Place");
        quickChipTagMap.put(R.id.chip_filter_specialty, "Specialty Coffee");
        quickChipTagMap.put(R.id.chip_filter_wifi, "WiFi Available");

        binding.chipFilterAll.setChecked(true);

        binding.chipFilterAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activeTagFilters.clear();

                for (int chipId : quickChipTagMap.keySet()) {
                    Chip chip = binding.getRoot().findViewById(chipId);
                    if (chip != null) chip.setChecked(false);
                }

                applySearchFilter(binding.searchView.getQuery() == null
                        ? ""
                        : binding.searchView.getQuery().toString());
            }
        });

        for (Map.Entry<Integer, String> entry : quickChipTagMap.entrySet()) {
            Chip chip = binding.getRoot().findViewById(entry.getKey());

            if (chip != null) {
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String tag = entry.getValue().toLowerCase(Locale.getDefault()).trim();

                    if (isChecked) {
                        activeTagFilters.add(tag);
                        binding.chipFilterAll.setChecked(false);
                    } else {
                        activeTagFilters.remove(tag);

                        if (activeTagFilters.isEmpty()) {
                            binding.chipFilterAll.setChecked(true);
                        }
                    }

                    applySearchFilter(binding.searchView.getQuery() == null
                            ? ""
                            : binding.searchView.getQuery().toString());
                });
            }
        }
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applySearchFilter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applySearchFilter(newText);
                return true;
            }
        });
    }

    private void applySearchFilter(String query) {
        displayedScoredCafes.clear();

        String lowerQuery = query == null ? "" : query.toLowerCase(Locale.getDefault()).trim();

        Set<String> categoryFilters = new HashSet<>();
        categoryFilters.addAll(activeTagFilters);
        categoryFilters.addAll(detailedCategoryFilters);

        for (RecommendationEngine.ScoredCafe sc : allScoredCafes) {
            Cafe cafe = sc.cafe;

            if (!lowerQuery.isEmpty()) {
                String name = cafe.getName();

                if (name == null || !name.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    continue;
                }
            }

            if (minRatingFilter > 0 && cafe.getAvgRating() > 0 && cafe.getAvgRating() < minRatingFilter) {
                continue;
            }

            if (cafe.getPricePerHour() > maxPriceFilter) {
                continue;
            }

            if (userLocation != null && maxDistanceFilter < 2000) {
                double distance = calculateDistance(
                        userLocation.getLatitude(),
                        userLocation.getLongitude(),
                        cafe.getLatitude(),
                        cafe.getLongitude()
                );

                if (distance > maxDistanceFilter) {
                    continue;
                }
            }

            if (!categoryFilters.isEmpty()) {
                boolean categoryMatched = false;

                if (cafe.getTags() != null) {
                    for (String tag : cafe.getTags()) {
                        if (tag != null && categoryFilters.contains(tag.toLowerCase(Locale.getDefault()).trim())) {
                            categoryMatched = true;
                            break;
                        }
                    }
                }

                if (!categoryMatched) {
                    continue;
                }
            }

            displayedScoredCafes.add(sc);
        }

        updateCafeLists();
        addMarkersToMap();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        enableMyLocation();

        googleMap.setOnMarkerClickListener(marker -> {
            RecommendationEngine.ScoredCafe scoredCafe = markerCafeMap.get(marker);

            if (scoredCafe != null) {
                onCafeClick(scoredCafe);
                return true;
            }

            return false;
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        if (googleMap != null) {
            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = location;

                        LatLng currentLatLng = new LatLng(
                                24.8178,
                                46.7038
                        );

                        if (googleMap != null) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));
                        }

                        loadCafes();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "Unable to get location",
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void moveToUserLocation() {
        if (userLocation != null && googleMap != null) {
            LatLng currentLatLng = new LatLng(
                    24.8178,
                    46.7038
            );

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        } else {
            getCurrentLocation();
        }
    }

    private void loadUserAndCafes() {
        String userId = FirebaseHelper.getInstance().getCurrentUserId();

        if (userId == null) {
            loadCafes();
            return;
        }

        FirebaseHelper.getInstance().getUsersRef().child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);

                        if (currentUser != null && currentUser.getFavoriteCafeIds() != null) {
                            favoriteCafeIds = new HashSet<>(currentUser.getFavoriteCafeIds());
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
        FirebaseHelper.getInstance().getCafesRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Cafe> allCafes = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Cafe cafe = ds.getValue(Cafe.class);

                    if (cafe != null) {
                        if (cafe.getCafeId() == null || cafe.getCafeId().isEmpty()) {
                            cafe.setCafeId(ds.getKey());
                        }

                        allCafes.add(cafe);
                    }
                }

                if (allCafes.isEmpty()) {
                    allCafes.addAll(getDummyCafes());
                }

                if (currentUser != null && userLocation != null) {
                    allScoredCafes = RecommendationEngine.getRecommendations(
                            allCafes,
                            currentUser,
                            userLocation.getLatitude(),
                            userLocation.getLongitude(),
                            favoriteCafeIds
                    );
                } else if (currentUser != null) {
                    allScoredCafes = RecommendationEngine.getInstance()
                            .scoreCafes(allCafes, currentUser, favoriteCafeIds);
                } else {
                    allScoredCafes = new ArrayList<>();

                    for (Cafe cafe : allCafes) {
                        allScoredCafes.add(
                                new RecommendationEngine.ScoredCafe(cafe, 0, "No user data")
                        );
                    }
                }

                displayedScoredCafes = new ArrayList<>(allScoredCafes);

                updateCafeLists();
                addMarkersToMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                List<Cafe> allCafes = getDummyCafes();

                allScoredCafes = new ArrayList<>();

                for (Cafe cafe : allCafes) {
                    allScoredCafes.add(
                            new RecommendationEngine.ScoredCafe(cafe, 0, "No user data")
                    );
                }

                displayedScoredCafes = new ArrayList<>(allScoredCafes);

                updateCafeLists();
                addMarkersToMap();

                Toast.makeText(requireContext(), "Using offline data", Toast.LENGTH_SHORT).show();
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
        c1.setAvgRating(4.7);
        c1.setTotalRatings(120);
        c1.setOpeningTime("07:00");
        c1.setClosingTime("23:00");
        c1.setCapacity(45);
        cafes.add(c1);

        Cafe c2 = new Cafe("cafe_002", "owner_002", "جدة كوفي هاوس",
                "شارع التحلية، جدة", 21.4858, 39.1925,
                "مقهى عصري على كورنيش جدة مع إطلالة رائعة على البحر الأحمر.",
                java.util.Arrays.asList("إطلالة بحرية", "موسيقى حية", "حلويات", "قهوة مختصة"), 40.0);
        c2.setImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400");
        c2.setAvgRating(4.5);
        c2.setTotalRatings(85);
        c2.setOpeningTime("08:00");
        c2.setClosingTime("01:00");
        c2.setCapacity(60);
        cafes.add(c2);

        Cafe c3 = new Cafe("cafe_003", "owner_003", "الدمام بريوز",
                "الكورنيش، الدمام", 26.4207, 50.0888,
                "مقهى مريح في الدمام يقدم مشروبات متنوعة ووجبات خفيفة لذيذة.",
                java.util.Arrays.asList("مكتبة", "هادئ", "قهوة فرنسية", "كيك"), 25.0);
        c3.setImageUrl("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400");
        c3.setAvgRating(4.3);
        c3.setTotalRatings(64);
        c3.setOpeningTime("06:30");
        c3.setClosingTime("22:30");
        c3.setCapacity(30);
        cafes.add(c3);

        return cafes;
    }

    private void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe) {
        Bundle args = new Bundle();
        args.putString("cafeId", scoredCafe.cafe.getCafeId());

        Navigation.findNavController(binding.getRoot())
                .navigate(R.id.action_googleMapFragment_to_cafeDetailFragment, args);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int radius = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radius * c;
    }

    private void updateCafeLists() {
        recommendedCafes.clear();
        nearbyCafes.clear();

        int recommendedCount = Math.min(10, displayedScoredCafes.size());

        for (int i = 0; i < recommendedCount; i++) {
            recommendedCafes.add(displayedScoredCafes.get(i));
        }

        if (userLocation != null) {
            for (RecommendationEngine.ScoredCafe scoredCafe : displayedScoredCafes) {
                double distance = calculateDistance(
                        userLocation.getLatitude(),
                        userLocation.getLongitude(),
                        scoredCafe.cafe.getLatitude(),
                        scoredCafe.cafe.getLongitude()
                );

                if (distance <= NEARBY_RADIUS_KM) {
                    nearbyCafes.add(scoredCafe);
                }
            }
        } else {
            nearbyCafes.addAll(displayedScoredCafes);
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                recommendedAdapter.notifyDataSetChanged();
                nearbyAdapter.notifyDataSetChanged();

                binding.tvRecommendedCount.setText(String.valueOf(recommendedCafes.size()));
                binding.tvNearbyCount.setText(String.valueOf(nearbyCafes.size()));
            });
        }
    }

    private void addMarkersToMap() {
        if (googleMap == null) return;

        googleMap.clear();
        markerCafeMap.clear();

        for (RecommendationEngine.ScoredCafe scoredCafe : displayedScoredCafes) {
            Cafe cafe = scoredCafe.cafe;
            LatLng position = new LatLng(cafe.getLatitude(), cafe.getLongitude());

            double matchPercent = getActiveTagMatchPercent(scoredCafe);
            boolean hasTagFilters = !activeTagFilters.isEmpty() || !detailedCategoryFilters.isEmpty();

            int markerSizeDp = hasTagFilters
                    ? RecommendationEngine.getBubbleSizeByMatchPercent(matchPercent)
                    : RecommendationEngine.getBubbleSize(scoredCafe.score);

            markerSizeDp = Math.max(38, markerSizeDp - 18);

            final int finalMarkerSizeDp = markerSizeDp;

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(cafe.getName())
                    .snippet(String.format(Locale.US, "Rating: %.1f", cafe.getAvgRating()))
                    .icon(BitmapDescriptorFactory.fromBitmap(
                            createBubbleMarkerBitmap(null, finalMarkerSizeDp)
                    ));

            Marker marker = googleMap.addMarker(markerOptions);

            if (marker != null) {
                markerCafeMap.put(marker, scoredCafe);

                String imageUrl = cafe.getImageUrl();

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    Glide.with(requireContext())
                            .asBitmap()
                            .load(imageUrl)
                            .centerCrop()
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource,
                                                            @Nullable Transition<? super Bitmap> transition) {
                                    if (markerCafeMap.containsKey(marker)) {
                                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(
                                                createBubbleMarkerBitmap(resource, finalMarkerSizeDp)
                                        ));
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                                }
                            });
                }
            }
        }
    }

    private double getActiveTagMatchPercent(RecommendationEngine.ScoredCafe scoredCafe) {
        Set<String> filters = new HashSet<>();
        filters.addAll(activeTagFilters);
        filters.addAll(detailedCategoryFilters);

        if (filters.isEmpty()) {
            return scoredCafe.score;
        }

        return RecommendationEngine.calculateTagMatchPercent(
                new ArrayList<>(filters),
                scoredCafe.cafe.getTags()
        );
    }

    private Bitmap createBubbleMarkerBitmap(@Nullable Bitmap cafeBitmap, int sizeDp) {
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float radius = sizePx / 2f;
        float cx = radius;
        float cy = radius;

        float imageRadius = radius;

        if (cafeBitmap != null) {
            Path clipPath = new Path();
            clipPath.addCircle(cx, cy, imageRadius, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clipPath);

            Rect srcRect = getCenterCropRect(cafeBitmap);
            RectF dstRect = new RectF(
                    cx - imageRadius,
                    cy - imageRadius,
                    cx + imageRadius,
                    cy + imageRadius
            );

            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawBitmap(cafeBitmap, srcRect, dstRect, imagePaint);

            canvas.restore();
        } else {
            Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerPaint.setColor(androidx.core.content.ContextCompat.getColor(
                    requireContext(),
                    R.color.warm_beige
            ));
            canvas.drawCircle(cx, cy, imageRadius, innerPaint);
        }

        return bitmap;
    }

    private Rect getCenterCropRect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width == height) {
            return new Rect(0, 0, width, height);
        }

        if (width > height) {
            int left = (width - height) / 2;
            return new Rect(left, 0, left + height, height);
        }

        int top = (height - width) / 2;
        return new Rect(0, top, width, top + width);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(
                        requireContext(),
                        "Location permission denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (googleMap != null) {
            googleMap.clear();
            googleMap = null;
        }

        binding = null;
    }
}