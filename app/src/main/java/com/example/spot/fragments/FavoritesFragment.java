package com.example.spot.fragments;

import android.os.Bundle;
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
import com.example.spot.databinding.FragmentFavoritesBinding;
import com.example.spot.models.Cafe;
import com.example.spot.utils.FirebaseHelper;
import com.example.spot.utils.RecommendationEngine;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private CafeListAdapter adapter;
    private List<RecommendationEngine.ScoredCafe> favoriteCafes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new CafeListAdapter(favoriteCafes, new CafeListAdapter.OnCafeClickListener() {
            @Override
            public void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe) {
                Bundle bundle = new Bundle();
                bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_favorites_to_cafeDetail, bundle);
            }

            @Override
            public void onBookClick(RecommendationEngine.ScoredCafe scoredCafe) {
                Bundle bundle = new Bundle();
                bundle.putString("cafeId", scoredCafe.cafe.getCafeId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_favorites_to_cafeDetail, bundle);
            }
        });

        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseHelper.getInstance().getFavoritesRef().child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        List<String> cafeIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            cafeIds.add(child.getKey());
                        }

                        if (cafeIds.isEmpty()) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                            favoriteCafes.clear();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        loadCafeDetails(cafeIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadCafeDetails(List<String> cafeIds) {
        favoriteCafes.clear();
        final int[] loaded = {0};

        for (String cafeId : cafeIds) {
            FirebaseHelper.getInstance().getCafesRef().child(cafeId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Cafe cafe = snapshot.getValue(Cafe.class);
                            if (cafe != null) {
                                cafe.setCafeId(snapshot.getKey());
                                favoriteCafes.add(new RecommendationEngine.ScoredCafe(cafe, cafe.getAvgRating(), 0));
                            }

                            loaded[0]++;
                            if (loaded[0] == cafeIds.size()) {
                                if (binding == null) return;
                                binding.progressBar.setVisibility(View.GONE);
                                binding.tvEmpty.setVisibility(favoriteCafes.isEmpty() ? View.VISIBLE : View.GONE);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

