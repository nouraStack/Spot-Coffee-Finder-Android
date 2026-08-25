package com.example.spot.fragments;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.spot.R;
import com.example.spot.databinding.FragmentMapBinding;
import com.example.spot.models.Cafe;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.example.spot.utils.RecommendationEngine;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapView mapView;
    private Map<Marker, RecommendationEngine.ScoredCafe> markerCafeMap = new HashMap<>();
    private User currentUser;
    private Set<String> favoriteCafeIds = new HashSet<>();

    // Store all scored cafes for filtering
    private List<RecommendationEngine.ScoredCafe> allScoredCafes = new ArrayList<>();

    // Quick-chip tag filters
    private Set<String> activeTagFilters = new HashSet<>();
    private String searchQuery = "";

    // Bottom-sheet advanced filters
    private float filterMinRating = 0f;
    private float filterMaxPrice = 200f;
    private float filterMaxDistance = 500f;
    private Set<String> sheetTagFilters = new HashSet<>();

    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Chip ID → tag string mapping (reused)
    private final Map<Integer, String> quickChipTagMap = new HashMap<>();
    private final Map<Integer, String> sheetChipTagMap = new HashMap<>();

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private double userLat = 24.7136;
    private double userLng = 46.6753;
    private boolean locationLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // OSMDroid configuration
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
                        loadUserAndCafes();
                    }
                });

        setupMap();
        setupTopBar();
        setupSearchView();
        setupQuickFilterChips();
        setupBottomSheetFilters();
        requestLocationIfNeeded();
    }

    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            loadUserAndCafes();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        locationLoaded = true;
                        GeoPoint point = new GeoPoint(userLat, userLng);
                        mapView.getController().animateTo(point, 13.0, 1000L);
                    }
                    loadUserAndCafes();
                })
                .addOnFailureListener(e -> loadUserAndCafes());
    }

    /* ───────────────────── MAP ───────────────────── */

    private void setupMap() {
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        // Default position - Riyadh, Saudi Arabia
        GeoPoint startPoint = new GeoPoint(24.7136, 46.6753);
        mapController.setZoom(6.0);
        mapController.setCenter(startPoint);

        // Tap on map hides preview
        mapView.setOnClickListener(v -> {
            if (binding != null) binding.cardCafePreview.setVisibility(View.GONE);
        });
    }

    /* ───────────────── TOP BAR ───────────────────── */

    private void setupTopBar() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.btnFilter.setOnClickListener(v -> {
            if (bottomSheetBehavior != null) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
    }


    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query == null ? "" : query.trim();
                applyAllFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText == null ? "" : newText.trim();
                applyAllFilters();
                return true;
            }
        });
    }

    /* ──────────────── QUICK CHIPS ────────────────── */

    private void setupQuickFilterChips() {
        quickChipTagMap.put(R.id.chip_filter_quiet, "Quiet Café");
        quickChipTagMap.put(R.id.chip_filter_strong_coffee, "Strong Coffee");
        quickChipTagMap.put(R.id.chip_filter_study, "Study Place");
        quickChipTagMap.put(R.id.chip_filter_outdoor, "Outdoor Seating");
        quickChipTagMap.put(R.id.chip_filter_wifi, "WiFi Available");
        quickChipTagMap.put(R.id.chip_filter_pet, "Pet Friendly");
        quickChipTagMap.put(R.id.chip_filter_specialty, "Specialty Coffee");
        quickChipTagMap.put(R.id.chip_filter_cozy, "Cozy Atmosphere");

        // "All" chip is checked by default
        binding.chipFilterAll.setChecked(true);

        // Handle "All" chip
        binding.chipFilterAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck all other chips
                activeTagFilters.clear();
                for (int chipId : quickChipTagMap.keySet()) {
                    Chip chip = binding.getRoot().findViewById(chipId);
                    if (chip != null) chip.setChecked(false);
                }
                applyAllFilters();
            }
        });

        // Handle tag filter chips
        for (Map.Entry<Integer, String> entry : quickChipTagMap.entrySet()) {
            Chip chip = binding.getRoot().findViewById(entry.getKey());
            if (chip != null) {
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        activeTagFilters.add(entry.getValue().toLowerCase().trim());
                        binding.chipFilterAll.setChecked(false);
                    } else {
                        activeTagFilters.remove(entry.getValue().toLowerCase().trim());
                        // If no filters active, check "All"
                        if (activeTagFilters.isEmpty()) {
                            binding.chipFilterAll.setChecked(true);
                        }
                    }
                    applyAllFilters();
                });
            }
        }
    }

    /* ──────────── BOTTOM SHEET FILTERS ───────────── */

    private void setupBottomSheetFilters() {
        View sheet = binding.filterBottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(sheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Sheet chip mapping
        sheetChipTagMap.put(R.id.sheet_chip_quiet, "Quiet Café");
        sheetChipTagMap.put(R.id.sheet_chip_strong_coffee, "Strong Coffee");
        sheetChipTagMap.put(R.id.sheet_chip_study, "Study Place");
        sheetChipTagMap.put(R.id.sheet_chip_outdoor, "Outdoor Seating");
        sheetChipTagMap.put(R.id.sheet_chip_wifi, "WiFi Available");
        sheetChipTagMap.put(R.id.sheet_chip_pet, "Pet Friendly");
        sheetChipTagMap.put(R.id.sheet_chip_specialty, "Specialty Coffee");
        sheetChipTagMap.put(R.id.sheet_chip_cozy, "Cozy Atmosphere");

        // Slider listeners
        binding.sliderRating.addOnChangeListener((slider, value, fromUser) ->
                binding.tvRatingValue.setText(String.format(Locale.US, "%.1f", value)));

        binding.sliderPrice.addOnChangeListener((slider, value, fromUser) ->
                binding.tvPriceValue.setText(String.format(Locale.US, "%.0f$", value)));

        binding.sliderDistance.addOnChangeListener((slider, value, fromUser) ->
                binding.tvDistanceValue.setText(String.format(Locale.US, "%.0fkm", value)));

        // Apply button
        binding.btnApplyFilters.setOnClickListener(v -> {
            filterMinRating = binding.sliderRating.getValue();
            filterMaxPrice = binding.sliderPrice.getValue();
            filterMaxDistance = binding.sliderDistance.getValue();

            sheetTagFilters.clear();
            for (Map.Entry<Integer, String> entry : sheetChipTagMap.entrySet()) {
                Chip chip = binding.getRoot().findViewById(entry.getKey());
                if (chip != null && chip.isChecked()) {
                    sheetTagFilters.add(entry.getValue().toLowerCase().trim());
                }
            }

            // Also sync quick chips with sheet tags
            syncQuickChipsFromSheet();

            applyAllFilters();
            updateFilterBadge();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });

        // Reset button
        binding.btnResetFilters.setOnClickListener(v -> {
            binding.sliderRating.setValue(0);
            binding.sliderPrice.setValue(200);
            binding.sliderDistance.setValue(500);

            for (int chipId : sheetChipTagMap.keySet()) {
                Chip chip = binding.getRoot().findViewById(chipId);
                if (chip != null) chip.setChecked(false);
            }

            filterMinRating = 0;
            filterMaxPrice = 200;
            filterMaxDistance = 500;
            sheetTagFilters.clear();
            activeTagFilters.clear();

            binding.chipFilterAll.setChecked(true);

            applyAllFilters();
            updateFilterBadge();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
    }

    /** Sync the quick-chip bar to mirror sheet tag selections */
    private void syncQuickChipsFromSheet() {
        activeTagFilters.clear();
        activeTagFilters.addAll(sheetTagFilters);

        // Update quick chip states without triggering listeners
        for (Map.Entry<Integer, String> entry : quickChipTagMap.entrySet()) {
            Chip chip = binding.getRoot().findViewById(entry.getKey());
            if (chip != null) {
                chip.setOnCheckedChangeListener(null);
                chip.setChecked(activeTagFilters.contains(entry.getValue().toLowerCase().trim()));
            }
        }

        binding.chipFilterAll.setOnCheckedChangeListener(null);
        binding.chipFilterAll.setChecked(activeTagFilters.isEmpty());

        // Re-attach listeners
        setupQuickFilterChips();
    }

    /* ──────────────── FILTER BADGE ───────────────── */

    private void updateFilterBadge() {
        int count = 0;
        if (filterMinRating > 0) count++;
        if (filterMaxPrice < 200) count++;
        if (filterMaxDistance < 500) count++;
        count += sheetTagFilters.size();

        if (count > 0) {
            binding.tvFilterBadge.setVisibility(View.VISIBLE);
            binding.tvFilterBadge.setText(String.valueOf(count));
        } else {
            binding.tvFilterBadge.setVisibility(View.GONE);
        }
    }

    /* ───────── COMBINED FILTER APPLICATION ────────── */

    private void applyAllFilters() {
        if (allScoredCafes.isEmpty()) return;

        List<RecommendationEngine.ScoredCafe> filtered = new ArrayList<>();
        String query = searchQuery == null ? "" : searchQuery.toLowerCase(Locale.getDefault()).trim();

        for (RecommendationEngine.ScoredCafe sc : allScoredCafes) {
            Cafe cafe = sc.cafe;

            // Search filter
            if (!query.isEmpty()) {
                String cafeName = cafe.getName() == null ? "" : cafe.getName().toLowerCase(Locale.getDefault());
                String cafeAddress = cafe.getAddress() == null ? "" : cafe.getAddress().toLowerCase(Locale.getDefault());
                if (!cafeName.contains(query) && !cafeAddress.contains(query)) continue;
            }

            // Rating filter
            if (cafe.getAvgRating() < filterMinRating) continue;

            // Price filter
            if (cafe.getPricePerHour() > filterMaxPrice) continue;

            // Distance filter (sc.distance is in km)
            if (sc.distance > filterMaxDistance) continue;

            // Do not hide cafes by tag here. Tag chips are used to calculate the
            // dynamic marker size so the user can visually compare all cafes.
            filtered.add(sc);
        }

        addMarkersToMap(filtered);
    }

    /* ─────────────── DATA LOADING ────────────────── */

    private void loadUserAndCafes() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);
                        loadFavorites();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadCafes();
                    }
                });
    }

    private void loadFavorites() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
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
                        List<Cafe> cafes = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Cafe cafe = child.getValue(Cafe.class);
                            if (cafe != null) {
                                cafe.setCafeId(child.getKey());
                                cafes.add(cafe);
                            }
                        }

                        allScoredCafes = RecommendationEngine.getRecommendations(
                                cafes, currentUser, userLat, userLng, favoriteCafeIds);

                        // Sort by distance so nearest cafes appear first
                        java.util.Collections.sort(allScoredCafes,
                                (a, b) -> Double.compare(a.distance, b.distance));

                        applyAllFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /* ──────────────── MAP MARKERS ────────────────── */

    private void addMarkersToMap(List<RecommendationEngine.ScoredCafe> scoredCafes) {
        if (mapView == null || !isAdded()) return;

        // Clear existing overlays
        mapView.getOverlays().clear();
        markerCafeMap.clear();

        // Hide preview card when markers change
        if (binding != null) {
            binding.cardCafePreview.setVisibility(View.GONE);
        }

        for (RecommendationEngine.ScoredCafe sc : scoredCafes) {
            Cafe cafe = sc.cafe;
            GeoPoint point = new GeoPoint(cafe.getLatitude(), cafe.getLongitude());

            double matchPercent = getActiveTagMatchPercent(sc);
            int bubbleSize = hasActiveTagFilters()
                    ? RecommendationEngine.getBubbleSizeByMatchPercent(matchPercent)
                    : RecommendationEngine.getBubbleSize(sc.score);
            String markerLabel = hasActiveTagFilters()
                    ? String.format(Locale.US, "%.0f%%", matchPercent)
                    : String.format(Locale.US, "%.1f", sc.score);

            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(cafe.getName());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            // Set default bubble icon first
            Drawable defaultIcon = createBubbleMarkerDrawable(
                    markerLabel,
                    bubbleSize, null);
            marker.setIcon(defaultIcon);

            // Disable default info window
            marker.setInfoWindow(null);

            // Handle marker click
            marker.setOnMarkerClickListener((clickedMarker, map) -> {
                RecommendationEngine.ScoredCafe scoredCafe = markerCafeMap.get(clickedMarker);
                if (scoredCafe != null) {
                    navigateToCafeDetails(scoredCafe);
                }
                return true;
            });

            mapView.getOverlays().add(marker);
            markerCafeMap.put(marker, sc);

            // Load cafe image into marker using Glide
            String imageUrl = cafe.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && isAdded()) {
                final Marker markerRef = marker;
                final String label = markerLabel;
                final int size = bubbleSize;
                Glide.with(this)
                        .asBitmap()
                        .load(imageUrl)
                        .circleCrop()
                        .override(size * 2, size * 2)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                if (!isAdded() || mapView == null) return;
                                Drawable icon = createBubbleMarkerDrawable(
                                        label,
                                        size, resource);
                                markerRef.setIcon(icon);
                                mapView.invalidate();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
        }

        mapView.invalidate();
    }


    private boolean hasActiveTagFilters() {
        return !activeTagFilters.isEmpty() || !sheetTagFilters.isEmpty();
    }

    private List<String> getCombinedActiveTags() {
        Set<String> combinedTags = new HashSet<>(activeTagFilters);
        combinedTags.addAll(sheetTagFilters);
        return new ArrayList<>(combinedTags);
    }

    private double getActiveTagMatchPercent(RecommendationEngine.ScoredCafe scoredCafe) {
        return RecommendationEngine.calculateTagMatchPercent(
                getCombinedActiveTags(),
                scoredCafe.cafe.getTags());
    }

    private void navigateToCafeDetails(RecommendationEngine.ScoredCafe scoredCafe) {
        if (binding == null || scoredCafe == null || scoredCafe.cafe == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_map_to_cafeDetail, bundle);
    }

    /* ────────────── MARKER DRAWING ───────────────── */

    private Drawable createBubbleMarkerDrawable(String score, int sizeDp, @Nullable Bitmap photoBitmap) {
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);
        int badgeSize = (int) (sizePx * 0.4f);
        int canvasSize = sizePx + badgeSize / 2;

        Bitmap bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float cx = sizePx / 2f;
        float cy = sizePx / 2f + badgeSize / 4f;
        float radius = sizePx / 2f;

        if (photoBitmap != null) {
            // Border circle
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(ContextCompat.getColor(requireContext(), R.color.burgundy));
            canvas.drawCircle(cx, cy, radius, borderPaint);

            // Circular photo
            Bitmap scaledPhoto = Bitmap.createScaledBitmap(photoBitmap, sizePx, sizePx, true);
            BitmapShader shader = new BitmapShader(scaledPhoto, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            photoPaint.setShader(shader);

            float photoRadius = radius - 3 * density;
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setTranslate(cx - sizePx / 2f, cy - sizePx / 2f);
            shader.setLocalMatrix(matrix);
            canvas.drawCircle(cx, cy, photoRadius, photoPaint);

            // Score badge
            float badgeCx = cx + radius * 0.7f;
            float badgeCy = cy - radius * 0.7f;
            float badgeRadius = badgeSize / 2f;

            Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeBgPaint.setColor(ContextCompat.getColor(requireContext(), R.color.burgundy));
            canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeBgPaint);

            Paint badgeInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeInnerPaint.setColor(ContextCompat.getColor(requireContext(), R.color.warm_beige));
            canvas.drawCircle(badgeCx, badgeCy, badgeRadius - 2 * density, badgeInnerPaint);

            Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeTextPaint.setColor(ContextCompat.getColor(requireContext(), R.color.burgundy));
            badgeTextPaint.setTextSize(badgeSize * 0.4f);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
            badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

            float textY = badgeCy - ((badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f);
            canvas.drawText(score, badgeCx, textY, badgeTextPaint);

        } else {
            // Solid circle fallback
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(ContextCompat.getColor(requireContext(), R.color.burgundy));
            canvas.drawCircle(cx, cy, radius, bgPaint);

            Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerPaint.setColor(ContextCompat.getColor(requireContext(), R.color.warm_beige));
            canvas.drawCircle(cx, cy, radius - 4 * density, innerPaint);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(ContextCompat.getColor(requireContext(), R.color.burgundy));
            textPaint.setTextSize(sizePx * 0.3f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            float textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f);
            canvas.drawText(score, cx, textY, textPaint);
        }

        return new BitmapDrawable(getResources(), bitmap);
    }

    /* ─────────────── CAFE PREVIEW ────────────────── */

    private void showCafePreview(RecommendationEngine.ScoredCafe scoredCafe) {
        if (binding == null || !isAdded()) return;
        binding.cardCafePreview.setVisibility(View.VISIBLE);
        binding.tvCafeNamePreview.setText(scoredCafe.cafe.getName());
        binding.tvRatingPreview.setText(String.format(Locale.US, "%.1f", scoredCafe.cafe.getAvgRating()));
        binding.tvScorePreview.setText(String.format(Locale.US, "Score: %.1f", scoredCafe.score));

        // Load cafe image into preview card
        String imageUrl = scoredCafe.cafe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivCafePreview);
        } else {
            binding.ivCafePreview.setImageResource(R.drawable.ic_launcher_foreground);
        }

        binding.btnViewDetails.setOnClickListener(v -> navigateToCafeDetails(scoredCafe));
    }

    /* ──────────────── LIFECYCLE ──────────────────── */

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) mapView.onDetach();
        binding = null;
    }
}
