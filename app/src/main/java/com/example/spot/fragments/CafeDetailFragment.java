package com.example.spot.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.spot.R;
import com.example.spot.adapters.ReviewAdapter;
import com.example.spot.databinding.FragmentCafeDetailBinding;
import com.example.spot.models.Cafe;
import com.example.spot.models.Rating;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CafeDetailFragment extends Fragment {

    private FragmentCafeDetailBinding binding;
    private String cafeId;
    private Cafe cafe;
    private boolean isFavorite = false;
    private ReviewAdapter reviewAdapter;
    private List<Rating> reviews = new ArrayList<>();
    private String currentUserName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCafeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            cafeId = getArguments().getString("cafeId");
        }

        reviewAdapter = new ReviewAdapter(reviews);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviews.setAdapter(reviewAdapter);

        setupListeners();
        loadCurrentUser();
        loadCafeDetails();
        checkFavoriteStatus();
        loadReviews();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigateUp();
        });

        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());

        binding.btnBookTable.setOnClickListener(v -> {
            if (cafe == null) return;
            Bundle bundle = new Bundle();
            bundle.putString("cafeId", cafeId);
            bundle.putString("cafeName", cafe.getName());
            bundle.putFloat("pricePerHour", (float) cafe.getPricePerHour());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_cafeDetail_to_booking, bundle);
        });

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void loadCurrentUser() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;
        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            currentUserName = user.getName();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadCafeDetails() {
        if (cafeId == null) return;

        FirebaseHelper.getInstance().getCafesRef().child(cafeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cafe = snapshot.getValue(Cafe.class);
                        if (cafe != null) {
                            cafe.setCafeId(snapshot.getKey());
                            displayCafeDetails();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error loading café", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayCafeDetails() {
        binding.tvCafeName.setText(cafe.getName());

        // Load cafe image from URL using Glide
        String imageUrl = cafe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivCafeImage);
        }

        binding.ratingBar.setRating((float) cafe.getAvgRating());
        binding.tvRating.setText(String.format(Locale.US, "%.1f", cafe.getAvgRating()));
        binding.tvTotalRatings.setText("(" + cafe.getTotalRatings() + " reviews)");
        binding.tvAddress.setText(cafe.getAddress() != null ? cafe.getAddress() : "Address not available");

        // Phone number
        String phone = cafe.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            binding.tvPhone.setText(phone);
            binding.layoutPhone.setVisibility(View.VISIBLE);
            binding.layoutPhone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            });
        } else {
            binding.layoutPhone.setVisibility(View.GONE);
        }

        binding.tvHours.setText(
                (cafe.getOpeningTime() != null ? cafe.getOpeningTime() : "9:00") +
                        " - " +
                        (cafe.getClosingTime() != null ? cafe.getClosingTime() : "22:00"));
        binding.tvDescription.setText(cafe.getDescription() != null ? cafe.getDescription() : "A wonderful café experience.");
        binding.tvScore.setVisibility(View.GONE);

        // Tags
        binding.chipGroupTags.removeAllViews();
        if (cafe.getTags() != null) {
            for (String tag : cafe.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.cream);
                chip.setTextColor(requireContext().getColor(R.color.text_dark));
                chip.setClickable(false);
                binding.chipGroupTags.addView(chip);
            }
        }
    }

    private void checkFavoriteStatus() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null || cafeId == null) return;

        FirebaseHelper.getInstance().getFavoritesRef().child(uid).child(cafeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isFavorite = snapshot.exists();
                        updateFavoriteIcon();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void toggleFavorite() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null || cafeId == null) return;

        if (isFavorite) {
            FirebaseHelper.getInstance().getFavoritesRef().child(uid).child(cafeId).removeValue();
            isFavorite = false;
        } else {
            FirebaseHelper.getInstance().getFavoritesRef().child(uid).child(cafeId).setValue(true);
            isFavorite = true;
        }
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        if (binding != null) {
            binding.ivFavoriteIcon.setImageResource(
                    isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        }
    }

    private void loadReviews() {
        if (cafeId == null) return;

        FirebaseHelper.getInstance().getRatingsRef()
                .orderByChild("cafeId").equalTo(cafeId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        reviews.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Rating rating = child.getValue(Rating.class);
                            if (rating != null) {
                                reviews.add(rating);
                            }
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void submitReview() {
        float score = binding.ratingBarInput.getRating();
        String comment = binding.etReview.getText().toString().trim();

        if (score == 0) {
            Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null || cafeId == null) return;

        String ratingId = FirebaseHelper.getInstance().getRatingsRef().push().getKey();
        Rating rating = new Rating(ratingId, uid, currentUserName, cafeId, score, comment);

        FirebaseHelper.getInstance().getRatingsRef().child(ratingId).setValue(rating)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Review submitted!", Toast.LENGTH_SHORT).show();
                    binding.ratingBarInput.setRating(0);
                    binding.etReview.setText("");
                    updateCafeRating(score);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCafeRating(float newScore) {
        if (cafe == null) return;

        int newTotal = cafe.getTotalRatings() + 1;
        double newAvg = ((cafe.getAvgRating() * cafe.getTotalRatings()) + newScore) / newTotal;

        FirebaseHelper.getInstance().getCafesRef().child(cafeId).child("avgRating").setValue(newAvg);
        FirebaseHelper.getInstance().getCafesRef().child(cafeId).child("totalRatings").setValue(newTotal);

        cafe.setAvgRating(newAvg);
        cafe.setTotalRatings(newTotal);
        displayCafeDetails();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

