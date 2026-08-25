package com.example.spot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spot.databinding.ActivityRegisterBinding;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseHelper.getInstance().getAuth();

        setupListeners();
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> performRegistration());

        binding.tvLogin.setOnClickListener(v -> {
            finish();
        });

        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isCustomer = checkedId == R.id.rb_customer;
            binding.tvPreferencesLabel.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
            binding.chipGroupPreferences.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        });
    }

    private void performRegistration() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Clear errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        String role = binding.rbProvider.isChecked() ? "provider" : "customer";
        List<String> preferences = getSelectedPreferences();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        User user = new User(uid, name, email, role);
                        user.setPreferences(preferences);

                        FirebaseHelper.getInstance().getUsersRef().child(uid).setValue(user)
                                .addOnCompleteListener(dbTask -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                                        Intent intent;
                                        if ("provider".equals(role)) {
                                            intent = new Intent(RegisterActivity.this, ProviderActivity.class);
                                        } else {
                                            intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        }
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        binding.btnRegister.setEnabled(true);
                                        Toast.makeText(this, "Error saving user: " +
                                                (dbTask.getException() != null ? dbTask.getException().getMessage() : "Unknown error"),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(this, "Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private List<String> getSelectedPreferences() {
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
        return prefs;
    }
}

