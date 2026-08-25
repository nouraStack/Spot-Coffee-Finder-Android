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
import com.example.spot.databinding.FragmentBookingConfirmationBinding;
import com.example.spot.utils.FirebaseHelper;

public class BookingConfirmationFragment extends Fragment {

    private FragmentBookingConfirmationBinding binding;
    private String bookingId;
    private String cafeId;
    private String status;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            bookingId = getArguments().getString("bookingId", "");
            String cafeName = getArguments().getString("cafeName", "");
            String dateTime = getArguments().getString("dateTime", "");
            status = getArguments().getString("status", "requested");
            cafeId = getArguments().getString("cafeId", "");

            binding.tvBookingId.setText(bookingId.length() > 8 ? bookingId.substring(0, 8) : bookingId);
            binding.tvCafeName.setText(cafeName);
            binding.tvDateTime.setText(dateTime);
        }

        boolean isConfirmed = "confirmed".equals(status);
        if (isConfirmed) {
            binding.tvBookingTitle.setText("Booking Confirmed!");
            binding.tvBookingTitle.setTextColor(requireContext().getColor(R.color.status_confirmed));
            binding.tvBookingSubtitle.setText("Your table is confirmed and ready.");
            binding.btnChat.setVisibility(View.VISIBLE);
        } else {
            binding.tvBookingTitle.setText(R.string.booking_requested);
            binding.tvBookingTitle.setTextColor(requireContext().getColor(R.color.status_pending));
            binding.tvBookingSubtitle.setText(R.string.awaiting_provider_review);
            binding.btnChat.setVisibility(View.GONE);
        }

        binding.btnChat.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("bookingId", bookingId);
            args.putString("cafeId", cafeId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_bookingConfirmation_to_chat, args);
        });

        binding.btnBackHome.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.homeFragment);
        });

        binding.btnCancel.setOnClickListener(v -> {
            if (bookingId != null) {
                FirebaseHelper.getInstance().cancelBookingAndReleaseTable(bookingId, (success, message) -> {
                    if (!isAdded()) return;
                    if (success) {
                        Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
                    } else {
                        Toast.makeText(requireContext(),
                                message != null ? message : "Failed to cancel booking",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
