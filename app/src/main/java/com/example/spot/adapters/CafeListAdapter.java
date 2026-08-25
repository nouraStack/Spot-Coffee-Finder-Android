package com.example.spot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spot.R;
import com.example.spot.utils.RecommendationEngine;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CafeListAdapter extends RecyclerView.Adapter<CafeListAdapter.ViewHolder> {

    private final List<RecommendationEngine.ScoredCafe> cafes;
    private final OnCafeClickListener listener;

    public interface OnCafeClickListener {
        void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe);
        void onBookClick(RecommendationEngine.ScoredCafe scoredCafe);
    }

    // Simple interface for map usage
    public interface SimpleCafeClickListener {
        void onClick(RecommendationEngine.ScoredCafe scoredCafe);
    }

    public CafeListAdapter(List<RecommendationEngine.ScoredCafe> cafes, OnCafeClickListener listener) {
        this.cafes = cafes;
        this.listener = listener;
    }

    // Constructor for simple click listener (used in map)
    public CafeListAdapter(List<RecommendationEngine.ScoredCafe> cafes, SimpleCafeClickListener simpleListener) {
        this.cafes = cafes;
        this.listener = new OnCafeClickListener() {
            @Override
            public void onCafeClick(RecommendationEngine.ScoredCafe scoredCafe) {
                simpleListener.onClick(scoredCafe);
            }

            @Override
            public void onBookClick(RecommendationEngine.ScoredCafe scoredCafe) {
                simpleListener.onClick(scoredCafe);
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cafe_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationEngine.ScoredCafe sc = cafes.get(position);
        holder.tvCafeName.setText(sc.cafe.getName());
        holder.tvRating.setText(String.format(Locale.US, "%.1f (%d)", sc.cafe.getAvgRating(), sc.cafe.getTotalRatings()));
        holder.tvScore.setText(String.format(Locale.US, "Score: %.1f", sc.score));
        holder.tvDescription.setText(sc.cafe.getDescription() != null ? sc.cafe.getDescription() : "");

        // Load cafe image from URL using Glide
        String imageUrl = sc.cafe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.ivCafe);
        } else {
            holder.ivCafe.setImageResource(R.drawable.ic_launcher_foreground);
            holder.ivCafe.setBackgroundResource(R.color.warm_beige);
        }

        holder.itemView.setOnClickListener(v -> listener.onCafeClick(sc));
        holder.btnBook.setOnClickListener(v -> listener.onBookClick(sc));
    }

    @Override
    public int getItemCount() {
        return cafes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCafe;
        TextView tvCafeName, tvRating, tvScore, tvDescription;
        MaterialButton btnBook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCafe = itemView.findViewById(R.id.iv_cafe);
            tvCafeName = itemView.findViewById(R.id.tv_cafe_name);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvDescription = itemView.findViewById(R.id.tv_description);
            btnBook = itemView.findViewById(R.id.btn_book);
        }
    }
}
