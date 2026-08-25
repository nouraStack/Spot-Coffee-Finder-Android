package com.example.spot;

import android.os.Bundle;
import android.content.SharedPreferences;

import com.example.spot.utils.CafeSeeder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.spot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

// Seed cafes only once (to correct "Cafes" node)
        SharedPreferences prefs = getSharedPreferences("spot_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("cafes_seeded_v3", false)) {
            new CafeSeeder().seedCafes();
            prefs.edit().putBoolean("cafes_seeded_v3", true).apply();
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}