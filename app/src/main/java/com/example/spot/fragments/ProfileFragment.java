package com.example.spot.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.spot.LoginActivity;
import com.example.spot.R;
import com.example.spot.databinding.FragmentProfileBinding;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadProfile();

        binding.btnEditPreferences.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_profile_to_editPreferences);
        });

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getInstance().getAuth().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Loading profile, please wait...", Toast.LENGTH_SHORT).show();
            loadProfile();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        EditText etName = new EditText(requireContext());
        etName.setHint("Name");
        etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
        layout.addView(etName);

        EditText etPhone = new EditText(requireContext());
        etPhone.setHint("Phone Number");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (currentUser.getPhoneNumber() != null) {
            etPhone.setText(currentUser.getPhoneNumber());
        }
        layout.addView(etPhone);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();

                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = FirebaseHelper.getInstance().getCurrentUserId();
                    if (uid == null) {
                        Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("name", newName);
                    updates.put("phoneNumber", newPhone);

                    FirebaseHelper.getInstance().getUsersRef().child(uid).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                loadProfile();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProfile() {
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) {
            Toast.makeText(requireContext(), "Please log in to view profile", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        User user = parseUserFromSnapshot(snapshot, uid);
                        if (user != null) {
                            currentUser = user;
                            binding.tvName.setText(user.getName() != null ? user.getName() : "");
                            binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                            String phone = user.getPhoneNumber();
                            binding.tvPhone.setText(
                                    phone != null && !phone.isEmpty() ? phone : "No phone number");

                            List<String> prefs = user.getPreferences();
                            binding.chipGroupPreferences.removeAllViews();
                            if (prefs != null && !prefs.isEmpty()) {
                                binding.tvNoPreferences.setVisibility(View.GONE);
                                for (String pref : prefs) {
                                    Chip chip = new Chip(requireContext());
                                    chip.setText(pref);
                                    chip.setChipBackgroundColorResource(R.color.cream);
                                    chip.setTextColor(requireContext().getColor(R.color.text_dark));
                                    chip.setClickable(false);
                                    binding.chipGroupPreferences.addView(chip);
                                }
                            } else {
                                binding.tvNoPreferences.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding != null) {
                            Toast.makeText(requireContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Load booking count
        FirebaseHelper.getInstance().getBookingsRef()
                .orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding != null) {
                            binding.tvBookingsCount.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Load favorites count
        FirebaseHelper.getInstance().getFavoritesRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding != null) {
                            binding.tvFavoritesCount.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private User parseUserFromSnapshot(DataSnapshot snapshot, String uid) {
        if (!snapshot.exists()) {
            return createAndSaveUserFromAuth(uid);
        }

        User user = snapshot.getValue(User.class);
        if (user != null) {
            return user;
        }

        user = new User();
        user.setUid(uid);
        user.setName(snapshot.child("name").getValue(String.class));
        user.setEmail(snapshot.child("email").getValue(String.class));
        user.setPhoneNumber(snapshot.child("phoneNumber").getValue(String.class));
        user.setRole(snapshot.child("role").getValue(String.class));

        List<String> prefs = new ArrayList<>();
        for (DataSnapshot child : snapshot.child("preferences").getChildren()) {
            String val = child.getValue(String.class);
            if (val != null) prefs.add(val);
        }
        user.setPreferences(prefs);

        if (user.getName() == null && user.getEmail() == null) {
            return createAndSaveUserFromAuth(uid);
        }
        return user;
    }

    private User createAndSaveUserFromAuth(String uid) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return null;

        User user = new User();
        user.setUid(uid);
        user.setName(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
        user.setEmail(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
        user.setRole("customer");
        user.setPreferences(new ArrayList<>());

        FirebaseHelper.getInstance().getUsersRef().child(uid).setValue(user);
        Toast.makeText(requireContext(), "Profile created from auth data", Toast.LENGTH_SHORT).show();
        return user;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

