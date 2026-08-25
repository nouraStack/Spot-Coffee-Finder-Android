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

import java.util.List;
import java.util.Locale;

public class CafeCardAdapter extends RecyclerView.Adapter<CafeCardAdapter.ViewHolder> {

    private final List<RecommendationEngine.ScoredCafe> cafes;
    private final OnCafeCardClickListener listener;

    public interface OnCafeCardClickListener {
        void onClick(RecommendationEngine.ScoredCafe scoredCafe);
    }

    public CafeCardAdapter(List<RecommendationEngine.ScoredCafe> cafes, OnCafeCardClickListener listener) {
        this.cafes = cafes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cafe_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationEngine.ScoredCafe sc = cafes.get(position);
        holder.tvCafeName.setText(sc.cafe.getName());
        holder.tvRating.setText(String.format(Locale.US, "%.1f (%d)", sc.cafe.getAvgRating(), sc.cafe.getTotalRatings()));
        holder.tvDescription.setText(sc.cafe.getDescription() != null ? sc.cafe.getDescription() : "");
        holder.tvScore.setText(String.format(Locale.US, "%.1f", sc.score));

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

        holder.itemView.setOnClickListener(v -> listener.onClick(sc));
    }

    @Override
    public int getItemCount() {
        return cafes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCafe;
        TextView tvCafeName, tvRating, tvDescription, tvScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCafe = itemView.findViewById(R.id.iv_cafe);
            tvCafeName = itemView.findViewById(R.id.tv_cafe_name);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvScore = itemView.findViewById(R.id.tv_score);
        }
    }
}

