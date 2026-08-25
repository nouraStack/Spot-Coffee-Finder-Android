package com.example.spot.provider;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spot.R;
import com.example.spot.databinding.FragmentMyCafesBinding;
import com.example.spot.models.Cafe;
import com.example.spot.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyCafesFragment extends Fragment {

    private FragmentMyCafesBinding binding;
    private final List<Cafe> cafes = new ArrayList<>();
    private CafeProviderAdapter adapter;
    private ValueEventListener cafesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyCafesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new CafeProviderAdapter();
        binding.rvCafes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCafes.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_myCafes_to_addEditCafe);
        });

        loadCafes();
    }

    private void loadCafes() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = FirebaseHelper.getInstance().getAuth().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;
        String email = currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";

        if (TextUtils.isEmpty(uid) && TextUtils.isEmpty(email)) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.VISIBLE);
            cafes.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        cafesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Map<String, Cafe> ownedCafeMap = new LinkedHashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Cafe cafe = child.getValue(Cafe.class);
                    if (cafe == null) continue;
                    String snapshotId = child.getKey();
                    cafe.setCafeId(snapshotId);

                    String ownerId = cafe.getOwnerId() != null ? cafe.getOwnerId().trim() : "";
                    String ownerEmail = cafe.getOwnerEmail() != null
                            ? cafe.getOwnerEmail().trim().toLowerCase(Locale.US) : "";

                    boolean matchesOwnerId = !TextUtils.isEmpty(uid) && uid.equals(ownerId);
                    boolean matchesOwnerEmail = !TextUtils.isEmpty(email) && email.equals(ownerEmail);

                    if (matchesOwnerId || matchesOwnerEmail) {
                        ownedCafeMap.put(snapshotId, cafe);
                    }
                }

                cafes.clear();
                cafes.addAll(ownedCafeMap.values());
                adapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(cafes.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        };

        FirebaseHelper.getInstance().getCafesRef().addValueEventListener(cafesListener);
    }

    private class CafeProviderAdapter extends RecyclerView.Adapter<CafeProviderAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cafe_list, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Cafe cafe = cafes.get(position);
            holder.tvName.setText(cafe.getName());
            holder.tvRating.setText(String.format(Locale.US, "%.1f (%d)", cafe.getAvgRating(), cafe.getTotalRatings()));
            holder.tvDescription.setText(cafe.getDescription() != null ? cafe.getDescription() : "");
            holder.tvScore.setText(String.format(Locale.US, "Cap: %d", cafe.getCapacity()));

            String imageUrl = cafe.getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
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

            holder.btnBook.setText(R.string.edit_cafe);
            holder.btnBook.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("cafeId", cafe.getCafeId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_myCafes_to_addEditCafe, bundle);
            });
        }

        @Override
        public int getItemCount() {
            return cafes.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRating, tvDescription, tvScore;
            ImageView ivCafe;
            com.google.android.material.button.MaterialButton btnBook;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_cafe_name);
                tvRating = itemView.findViewById(R.id.tv_rating);
                tvDescription = itemView.findViewById(R.id.tv_description);
                tvScore = itemView.findViewById(R.id.tv_score);
                ivCafe = itemView.findViewById(R.id.iv_cafe);
                btnBook = itemView.findViewById(R.id.btn_book);
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (cafesListener != null) {
            FirebaseHelper.getInstance().getCafesRef().removeEventListener(cafesListener);
            cafesListener = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
