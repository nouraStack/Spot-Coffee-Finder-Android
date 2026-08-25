package com.example.spot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spot.databinding.ActivityLoginBinding;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private boolean isProviderLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();

        // Let first frame render, then do lightweight auth check.
        binding.getRoot().post(() -> {
            auth = FirebaseHelper.getInstance().getAuth();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null && !isFinishing() && !isDestroyed()) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnLogin.setEnabled(false);
                routeLoggedInUser(currentUser.getUid());
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        binding.tvLoginAsProvider.setOnClickListener(v -> {
            isProviderLogin = !isProviderLogin;
            if (isProviderLogin) {
                binding.tvLoginAsProvider.setText(R.string.login_as_user);
                binding.btnLogin.setText(R.string.login_as_provider_button);
            } else {
                binding.tvLoginAsProvider.setText(R.string.login_as_provider);
                binding.btnLogin.setText(R.string.login);
            }
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            if (auth == null) auth = FirebaseHelper.getInstance().getAuth();
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                binding.tilEmail.setError("Enter email to reset password");
                return;
            }
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void performLogin() {
        if (auth == null) {
            auth = FirebaseHelper.getInstance().getAuth();
        }

        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        navigateToMain(uid);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(this, "Login failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void routeLoggedInUser(String uid) {
        // Fast path: avoid waiting on Realtime Database during cold start.
        // If the user manually selected provider mode, honor that. Otherwise default to customer shell.
        Intent intent = isProviderLogin
                ? new Intent(LoginActivity.this, ProviderActivity.class)
                : new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain(String uid) {
        if (uid == null) return;
        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        if (isFinishing() || isDestroyed()) return;
                        binding.progressBar.setVisibility(View.GONE);
                        User user = snapshot.getValue(User.class);
                        Intent intent;
                        if (user != null && "provider".equals(user.getRole())) {
                            intent = new Intent(LoginActivity.this, ProviderActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                        if (isFinishing() || isDestroyed()) return;
                        binding.progressBar.setVisibility(View.GONE);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }
}
