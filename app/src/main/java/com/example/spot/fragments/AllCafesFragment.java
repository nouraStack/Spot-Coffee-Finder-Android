package com.example.spot.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spot.R;
import com.example.spot.adapters.CafeListAdapter;
import com.example.spot.databinding.FragmentAllCafesBinding;
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

public class AllCafesFragment extends Fragment {

    private FragmentAllCafesBinding binding;
    private CafeListAdapter adapter;
    private final List<RecommendationEngine.ScoredCafe> cafes = new ArrayList<>();
    private final Set<String> favoriteCafeIds = new HashSet<>();
    private User currentUser;
    private String searchQuery = "";
    private String selectedDate;
    private String selectedStartTime;
    private String selectedEndTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAllCafesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArguments();
        setupList();
        setupHeader();
        loadUserData();
    }

    private void readArguments() {
        if (getArguments() == null) return;
        searchQuery = getArguments().getString("searchQuery", "");
        selectedDate = getArguments().getString("selectedDate");
        selectedStartTime = getArguments().getString("selectedStartTime");
        selectedEndTime = getArguments().getString("selectedEndTime");
    }

    private void setupList() {
        adapter = new CafeListAdapter(cafes, new CafeListAdapter.OnCafeClickListener() {
            @Override
            public void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe) {
                openCafeDetail(scoredCafe.cafe.getCafeId());
            }

            @Override
            public void onBookClick(RecommendationEngine.ScoredCafe scoredCafe) {
                openCafeDetail(scoredCafe.cafe.getCafeId());
            }
        });
        binding.rvAllCafes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllCafes.setAdapter(adapter);
    }

    private void setupHeader() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());

        if (!TextUtils.isEmpty(searchQuery)) {
            binding.tvSubtitle.setText("Showing results for \"" + searchQuery + "\"");
            binding.tvSubtitle.setVisibility(View.VISIBLE);
        } else if (!TextUtils.isEmpty(selectedDate)
                && !TextUtils.isEmpty(selectedStartTime)
                && !TextUtils.isEmpty(selectedEndTime)) {
            binding.tvSubtitle.setText(getString(
                    R.string.selected_time_format,
                    selectedDate,
                    selectedStartTime,
                    selectedEndTime));
            binding.tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            binding.tvSubtitle.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) {
            loadCafes();
            return;
        }

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
                                cafe.setCafeId(child.getKey());
                                allCafes.add(cafe);
                            }
                        }

                        cafes.clear();
                        if (!allCafes.isEmpty()) {
                            double userLat = 24.7136;
                            double userLng = 46.6753;
                            List<RecommendationEngine.ScoredCafe> scored = RecommendationEngine.getRecommendations(
                                    allCafes, currentUser, userLat, userLng, favoriteCafeIds);
                            for (RecommendationEngine.ScoredCafe scoredCafe : scored) {
                                if (matchesSearch(scoredCafe, searchQuery)) {
                                    cafes.add(scoredCafe);
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        binding.tvEmpty.setVisibility(cafes.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
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

    private void openCafeDetail(String cafeId) {
        Bundle bundle = new Bundle();
        bundle.putString("cafeId", cafeId);
        Navigation.findNavController(requireView())
                .navigate(R.id.cafeDetailFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
