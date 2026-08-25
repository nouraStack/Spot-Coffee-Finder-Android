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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spot.R;
import com.example.spot.adapters.BookingAdapter;
import com.example.spot.databinding.FragmentReservationsBinding;
import com.example.spot.models.Booking;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReservationsFragment extends Fragment {

    private FragmentReservationsBinding binding;
    private BookingAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> displayedBookings = new ArrayList<>();
    private boolean showingCurrent = true;
    private ValueEventListener bookingsListener;
    private com.google.firebase.database.Query bookingsQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new BookingAdapter(displayedBookings, new BookingAdapter.OnCancelClickListener() {
            @Override
            public void onCancel(Booking booking) {
                String status = booking.getStatus() != null ? booking.getStatus() : "requested";

                if ("awaiting_customer_decision".equals(status)) {
                    acceptProviderProposal(booking);
                } else {
                    FirebaseHelper.getInstance().cancelBookingAndReleaseTable(booking.getBookingId(), (success, message) -> {
                        if (!isAdded()) return;
                        if (success) {
                            Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    message != null ? message : "Failed to cancel booking",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onChat(Booking booking) {
                Bundle args = new Bundle();
                args.putString("bookingId", booking.getBookingId());
                args.putString("cafeId", booking.getCafeId() != null ? booking.getCafeId() : "");
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_reservations_to_chat, args);
            }
        });

        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(adapter);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingCurrent = tab.getPosition() == 0;
                filterBookings();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadBookings();
    }

    private void acceptProviderProposal(Booking booking) {
        if (booking.getBookingId() == null) return;
        if (booking.getProposalDate() == null || booking.getProposalDate().isEmpty()) {
            Toast.makeText(requireContext(), "No proposed time found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("date")
                .setValue(booking.getProposalDate());
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("startTime")
                .setValue(booking.getProposalStartTime());
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("endTime")
                .setValue(booking.getProposalEndTime());
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("status")
                .setValue("requested");
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("notes")
                .setValue("Customer accepted provider proposal. Waiting for final table assignment.");
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalDate")
                .setValue("");
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalStartTime")
                .setValue("");
        FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalEndTime")
                .setValue("");

        Toast.makeText(requireContext(), getString(com.example.spot.R.string.proposal_accepted), Toast.LENGTH_SHORT).show();
    }

    private void loadBookings() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        if (bookingsQuery != null && bookingsListener != null) {
            bookingsQuery.removeEventListener(bookingsListener);
        }

        bookingsQuery = FirebaseHelper.getInstance().getBookingsRef()
                .orderByChild("userId").equalTo(uid);

        bookingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                allBookings.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Booking booking = child.getValue(Booking.class);
                    if (booking != null) {
                        booking.setBookingId(child.getKey());
                        if ("confirmed".equals(booking.getStatus())
                                && FirebaseHelper.getInstance().isBookingExpired(booking)) {
                            FirebaseHelper.getInstance().completeBookingAndReleaseTable(booking.getBookingId(), null);
                            continue;
                        }
                        allBookings.add(booking);
                    }
                }
                filterBookings();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        };

        bookingsQuery.addValueEventListener(bookingsListener);
    }

    private void filterBookings() {
        if (binding == null) return;

        displayedBookings.clear();

        for (Booking b : allBookings) {
            String status = b.getStatus() != null ? b.getStatus() : "requested";

            boolean isPastStatus =
                    "cancelled".equals(status)
                            || "completed".equals(status)
                            || "rejected".equals(status);

            if (showingCurrent) {
                // Current bookings: requested, confirmed, awaiting_customer_decision
                if (!isPastStatus) {
                    displayedBookings.add(b);
                }
            } else {
                // Past bookings: cancelled, completed, rejected only
                if (isPastStatus) {
                    displayedBookings.add(b);
                }
            }
        }

        adapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(displayedBookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (bookingsQuery != null && bookingsListener != null) {
            bookingsQuery.removeEventListener(bookingsListener);
            bookingsListener = null;
            bookingsQuery = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
