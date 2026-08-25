package com.example.spot.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.spot.R;
import com.example.spot.databinding.FragmentEditPreferencesBinding;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EditPreferencesFragment extends Fragment {

    private FragmentEditPreferencesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditPreferencesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentPreferences();

        binding.btnSave.setOnClickListener(v -> savePreferences());
    }

    private void loadCurrentPreferences() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getPreferences() != null && binding != null) {
                            List<String> prefs = user.getPreferences();
                            int[] chipIds = {
                                    R.id.chip_quiet, R.id.chip_strong_coffee, R.id.chip_study,
                                    R.id.chip_outdoor, R.id.chip_wifi, R.id.chip_pet,
                                    R.id.chip_specialty, R.id.chip_cozy
                            };
                            for (int id : chipIds) {
                                Chip chip = binding.chipGroupPreferences.findViewById(id);
                                if (chip != null && prefs.contains(chip.getText().toString())) {
                                    chip.setChecked(true);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void savePreferences() {
        List<String> prefs = new ArrayList<>();
        int[] chipIds = {
                R.id.chip_quiet, R.id.chip_strong_coffee, R.id.chip_study,
                R.id.chip_outdoor, R.id.chip_wifi, R.id.chip_pet,
                R.id.chip_specialty, R.id.chip_cozy
        };

        for (int id : chipIds) {
            Chip chip = binding.chipGroupPreferences.findViewById(id);
            if (chip != null && chip.isChecked()) {
                prefs.add(chip.getText().toString());
            }
        }

        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseHelper.getInstance().getUsersRef().child(uid).child("preferences").setValue(prefs)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Preferences saved!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

