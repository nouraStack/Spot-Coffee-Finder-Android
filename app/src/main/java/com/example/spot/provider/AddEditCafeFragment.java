package com.example.spot.provider;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.spot.R;
import com.example.spot.databinding.FragmentAddEditCafeBinding;
import com.example.spot.models.Cafe;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditCafeFragment extends Fragment {

    private FragmentAddEditCafeBinding binding;
    private String cafeId;
    private String openingTime = "";
    private String closingTime = "";
    private boolean isEditMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditCafeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            cafeId = getArguments().getString("cafeId", "");
            if (!cafeId.isEmpty()) {
                isEditMode = true;
                binding.tvTitle.setText(R.string.edit_cafe);
                loadCafeData();
            }
        }

        binding.btnOpeningTime.setOnClickListener(v -> showTimePicker(true));
        binding.btnClosingTime.setOnClickListener(v -> showTimePicker(false));
        binding.btnSave.setOnClickListener(v -> saveCafe());
        binding.btnPreviewImage.setOnClickListener(v -> loadImagePreview());
    }

    private void loadImagePreview() {
        if (binding == null) return;
        String url = binding.etImageUrl.getText() != null
                ? binding.etImageUrl.getText().toString().trim() : "";
        if (!url.isEmpty() && isAdded()) {
            Glide.with(this)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivCafePreview);
        }
    }

    private void loadCafeData() {
        FirebaseHelper.getInstance().getCafesRef().child(cafeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Cafe cafe = snapshot.getValue(Cafe.class);
                        if (cafe != null && binding != null) {
                            binding.etName.setText(cafe.getName());
                            binding.etAddress.setText(cafe.getAddress());
                            binding.etDescription.setText(cafe.getDescription());
                            binding.etLatitude.setText(String.valueOf(cafe.getLatitude()));
                            binding.etLongitude.setText(String.valueOf(cafe.getLongitude()));
                            binding.etCapacity.setText(String.valueOf(cafe.getCapacity()));
                            binding.etPrice.setText(String.valueOf(cafe.getPricePerHour()));

                            // Phone Number
                            if (cafe.getPhoneNumber() != null) {
                                binding.etPhone.setText(cafe.getPhoneNumber());
                            }

                            // Image URL
                            if (cafe.getImageUrl() != null && !cafe.getImageUrl().isEmpty()) {
                                binding.etImageUrl.setText(cafe.getImageUrl());
                                loadImagePreview();
                            }

                            if (cafe.getOpeningTime() != null) {
                                openingTime = cafe.getOpeningTime();
                                binding.btnOpeningTime.setText("Opens: " + openingTime);
                            }
                            if (cafe.getClosingTime() != null) {
                                closingTime = cafe.getClosingTime();
                                binding.btnClosingTime.setText("Closes: " + closingTime);
                            }

                            // Set tags
                            if (cafe.getTags() != null) {
                                int[] chipIds = {
                                        R.id.chip_quiet, R.id.chip_strong_coffee, R.id.chip_study,
                                        R.id.chip_outdoor, R.id.chip_wifi, R.id.chip_pet,
                                        R.id.chip_specialty, R.id.chip_cozy
                                };
                                for (int id : chipIds) {
                                    Chip chip = binding.chipGroupTags.findViewById(id);
                                    if (chip != null && cafe.getTags().contains(chip.getText().toString())) {
                                        chip.setChecked(true);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showTimePicker(boolean isOpening) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, h, m) -> {
            String time = String.format(Locale.US, "%02d:%02d", h, m);
            if (isOpening) {
                openingTime = time;
                binding.btnOpeningTime.setText("Opens: " + time);
            } else {
                closingTime = time;
                binding.btnClosingTime.setText("Closes: " + time);
            }
        }, cal.get(Calendar.HOUR_OF_DAY), 0, true).show();
    }

    private void saveCafe() {
        String name = binding.etName.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String latStr = binding.etLatitude.getText().toString().trim();
        String lngStr = binding.etLongitude.getText().toString().trim();
        String capStr = binding.etCapacity.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String phone = binding.etPhone.getText() != null
                ? binding.etPhone.getText().toString().trim() : "";
        String imageUrl = binding.etImageUrl.getText() != null
                ? binding.etImageUrl.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            binding.tilAddress.setError("Address is required");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        double lat = 0, lng = 0;
        int capacity = 20;
        double price = 20;

        try {
            if (!TextUtils.isEmpty(latStr)) lat = Double.parseDouble(latStr);
            if (!TextUtils.isEmpty(lngStr)) lng = Double.parseDouble(lngStr);
            if (!TextUtils.isEmpty(capStr)) capacity = Integer.parseInt(capStr);
            if (!TextUtils.isEmpty(priceStr)) price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            // Use defaults
        }

        List<String> tags = getSelectedTags();
        FirebaseUser currentUser = FirebaseHelper.getInstance().getAuth().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;
        String ownerEmail = currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";

        String id = isEditMode ? cafeId : FirebaseHelper.getInstance().getCafesRef().push().getKey();

        Cafe cafe = new Cafe(id, uid, name, address, lat, lng, description, tags, price);
        cafe.setOwnerEmail(ownerEmail);
        cafe.setCapacity(capacity);
        cafe.setOpeningTime(openingTime);
        cafe.setClosingTime(closingTime);
        cafe.setImageUrl(imageUrl);
        cafe.setPhoneNumber(phone);

        FirebaseHelper.getInstance().getCafesRef().child(id).setValue(cafe)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            isEditMode ? "Café updated!" : "Café added!",
                            Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private List<String> getSelectedTags() {
        List<String> tags = new ArrayList<>();
        int[] chipIds = {
                R.id.chip_quiet, R.id.chip_strong_coffee, R.id.chip_study,
                R.id.chip_outdoor, R.id.chip_wifi, R.id.chip_pet,
                R.id.chip_specialty, R.id.chip_cozy
        };
        for (int id : chipIds) {
            Chip chip = binding.chipGroupTags.findViewById(id);
            if (chip != null && chip.isChecked()) {
                tags.add(chip.getText().toString());
            }
        }
        return tags;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
