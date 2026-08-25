package com.example.spot.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.spot.databinding.FragmentTimeSelectionBinding;

import java.util.Calendar;
import java.util.Locale;

public class TimeSelectionFragment extends Fragment {

    private FragmentTimeSelectionBinding binding;
    private String selectedDate;
    private String selectedStartTime;
    private String selectedEndTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimeSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectDate.setOnClickListener(v -> showDatePicker());
        binding.btnStartTime.setOnClickListener(v -> showTimePicker(true));
        binding.btnEndTime.setOnClickListener(v -> showTimePicker(false));

        binding.btnFindCafes.setOnClickListener(v -> {
            if (selectedDate == null || selectedStartTime == null || selectedEndTime == null) {
                Toast.makeText(requireContext(), "Please select date and times", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString("selectedDate", selectedDate);
            bundle.putString("selectedStartTime", selectedStartTime);
            bundle.putString("selectedEndTime", selectedEndTime);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_timeSelection_to_home, bundle);
        });

        binding.btnSkip.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_timeSelection_to_home);
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    binding.btnSelectDate.setText(selectedDate);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar cal = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                    if (isStartTime) {
                        selectedStartTime = time;
                        binding.btnStartTime.setText("Start: " + time);
                    } else {
                        selectedEndTime = time;
                        binding.btnEndTime.setText("End: " + time);
                    }
                },
                cal.get(Calendar.HOUR_OF_DAY), 0, true);
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

