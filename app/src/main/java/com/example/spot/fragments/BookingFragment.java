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
import com.example.spot.databinding.FragmentBookingBinding;
import com.example.spot.models.Booking;
import com.example.spot.models.CafeTable;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

public class BookingFragment extends Fragment {

    private FragmentBookingBinding binding;
    private String cafeId;
    private String cafeName;
    private float pricePerHour;
    private String selectedDate;
    private int startHour = -1, startMinute = -1;
    private int endHour = -1, endMinute = -1;
    private int guestCount = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            cafeId = getArguments().getString("cafeId");
            cafeName = getArguments().getString("cafeName", "Café");
            pricePerHour = getArguments().getFloat("pricePerHour", 20f);
        }

        binding.tvCafeName.setText(cafeName);
        binding.tvPricePerHour.setText(String.format(Locale.US, "%.0f SAR", pricePerHour));
        binding.tvGuests.setText(String.valueOf(guestCount));

        setupListeners();
    }

    private void setupListeners() {
        binding.btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        binding.btnSelectDate.setText(selectedDate);
                        updatePrice();
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis());
            dialog.show();
        });

        binding.btnStartTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (view, h, m) -> {
                startHour = h;
                startMinute = m;
                binding.btnStartTime.setText(String.format(Locale.US, "Start: %02d:%02d", h, m));
                updatePrice();
            }, cal.get(Calendar.HOUR_OF_DAY), 0, true).show();
        });

        binding.btnEndTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (view, h, m) -> {
                endHour = h;
                endMinute = m;
                binding.btnEndTime.setText(String.format(Locale.US, "End: %02d:%02d", h, m));
                updatePrice();
            }, cal.get(Calendar.HOUR_OF_DAY), 0, true).show();
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (guestCount > 1) {
                guestCount--;
                binding.tvGuests.setText(String.valueOf(guestCount));
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (guestCount < 20) {
                guestCount++;
                binding.tvGuests.setText(String.valueOf(guestCount));
            }
        });

        binding.btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    private void updatePrice() {
        if (startHour >= 0 && endHour >= 0) {
            double durationHours = calculateDurationHours();
            double total = durationHours * pricePerHour;
            binding.tvDuration.setText(String.format(Locale.US, "%.1f hours", durationHours));
            binding.tvTotalPrice.setText(String.format(Locale.US, "%.0f SAR", total));
        }
    }

    private double calculateDurationHours() {
        double durationHours = (endHour + endMinute / 60.0) - (startHour + startMinute / 60.0);
        if (durationHours <= 0) durationHours += 24;
        return durationHours;
    }

    private void confirmBooking() {
        if (selectedDate == null) {
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startHour < 0 || endHour < 0) {
            Toast.makeText(requireContext(), "Please select start and end times", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cafeId == null || cafeId.isEmpty()) {
            Toast.makeText(requireContext(), "Cafe not found", Toast.LENGTH_SHORT).show();
            return;
        }

        double durationHours = calculateDurationHours();
        if (durationHours < 1.0) {
            Toast.makeText(requireContext(), "Minimum reservation is 1 hour", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnConfirm.setEnabled(false);

        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) {
            if (binding != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnConfirm.setEnabled(true);
            }
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String bookingId = FirebaseHelper.getInstance().getBookingsRef().push().getKey();
        String startTime = String.format(Locale.US, "%02d:%02d", startHour, startMinute);
        String endTime = String.format(Locale.US, "%02d:%02d", endHour, endMinute);
        double totalPrice = durationHours * pricePerHour;

        validateAvailabilityAndCreate(uid, bookingId, startTime, endTime, totalPrice);
    }

    private void validateAvailabilityAndCreate(String uid, String bookingId, String startTime,
                                                String endTime, double totalPrice) {
        FirebaseHelper.getInstance().getTablesRef().child(cafeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot tableSnapshot) {
                        final List<CafeTable> matchingTables = new ArrayList<>();
                        for (DataSnapshot child : tableSnapshot.getChildren()) {
                            CafeTable table = child.getValue(CafeTable.class);
                            if (table != null && table.getSeats() >= guestCount) {
                                table.setTableId(child.getKey());
                                matchingTables.add(table);
                            }
                        }

                        if (matchingTables.isEmpty()) {
                            finishBookingValidationWithError("No available table matches this request now.");
                            return;
                        }

                        FirebaseHelper.getInstance().getBookingsRef()
                                .orderByChild("cafeId").equalTo(cafeId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
                                        // Find busy tables for this time slot
                                        List<String> busyTableIds = new ArrayList<>();
                                        for (DataSnapshot child : bookingSnapshot.getChildren()) {
                                            Booking existing = child.getValue(Booking.class);
                                            if (existing == null) continue;
                                            if (!selectedDate.equals(existing.getDate())) continue;

                                            String status = existing.getStatus() != null ? existing.getStatus() : "";
                                            boolean blocksCapacity = "confirmed".equals(status)
                                                    || "awaiting_customer_decision".equals(status);
                                            if (!blocksCapacity) continue;

                                            if (timesOverlap(startTime, endTime,
                                                    existing.getStartTime(), existing.getEndTime())) {
                                                String tid = existing.getAssignedTableId();
                                                if (tid != null && !tid.isEmpty()) {
                                                    busyTableIds.add(tid);
                                                }
                                            }
                                        }

                                        // Find first available table
                                        CafeTable availableTable = null;
                                        for (CafeTable table : matchingTables) {
                                            if (!busyTableIds.contains(table.getTableId())) {
                                                availableTable = table;
                                                break;
                                            }
                                        }

                                        createRequestedBooking(uid, bookingId, startTime, endTime, totalPrice);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        finishBookingValidationWithError(error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finishBookingValidationWithError(error.getMessage());
                    }
                });
    }

    private boolean timesOverlap(String startA, String endA, String startB, String endB) {
        int aStart = toMinutes(startA);
        int aEnd = toMinutes(endA);
        int bStart = toMinutes(startB);
        int bEnd = toMinutes(endB);
        return aStart < bEnd && bStart < aEnd;
    }

    private int toMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void createAutoConfirmedBooking(String uid, String bookingId, String startTime,
                                              String endTime, double totalPrice, CafeTable availableTable) {
        Booking booking = new Booking(bookingId, uid, cafeId, cafeName,
                selectedDate, startTime, endTime, guestCount, totalPrice, "confirmed");
        booking.setAssignedTableId(availableTable.getTableId());
        booking.setAssignedTableLabel("T" + availableTable.getTableNumber() + " - " + availableTable.getZone());
        booking.setNotes("Reservation confirmed. Your table is ready.");

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getName() != null) {
                            booking.setCustomerName(user.getName());
                        }
                        persistAutoConfirmedBooking(booking, availableTable);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        persistAutoConfirmedBooking(booking, availableTable);
                    }
                });
    }

    private void persistAutoConfirmedBooking(Booking booking, CafeTable table) {
        String bid = booking.getBookingId();
        FirebaseHelper.getInstance().getBookingsRef().child(bid).setValue(booking)
                .addOnSuccessListener(aVoid -> {
                    FirebaseHelper.getInstance().assignTableToBooking(
                            booking, cafeId, table.getTableId(), booking.getAssignedTableLabel(),
                            (success, message) -> {
                                if (!isAdded()) return;
                                if (binding != null) {
                                    binding.progressBar.setVisibility(View.GONE);
                                }
                                if (success) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("bookingId", bid);
                                    bundle.putString("cafeName", cafeName);
                                    bundle.putString("dateTime", selectedDate + " " + booking.getStartTime() + " - " + booking.getEndTime());
                                    bundle.putString("status", "confirmed");
                                    bundle.putString("cafeId", cafeId != null ? cafeId : "");
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_booking_to_confirmation, bundle);
                                } else {
                                    finishBookingValidationWithError(message != null ? message : "Table assignment failed");
                                }
                            });
                })
                .addOnFailureListener(e -> finishBookingValidationWithError(e.getMessage()));
    }

    private void createRequestedBooking(String uid, String bookingId, String startTime,
                                        String endTime, double totalPrice) {
        Booking booking = new Booking(bookingId, uid, cafeId, cafeName,
                selectedDate, startTime, endTime, guestCount, totalPrice, "requested");

        FirebaseHelper.getInstance().getUsersRef().child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getName() != null) {
                            booking.setCustomerName(user.getName());
                        }
                        booking.setNotes(getString(R.string.awaiting_provider_review));
                        saveBooking(booking, bookingId, startTime, endTime);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        booking.setNotes(getString(R.string.awaiting_provider_review));
                        saveBooking(booking, bookingId, startTime, endTime);
                    }
                });
    }

    private void saveBooking(Booking booking, String bookingId, String startTime, String endTime) {
        FirebaseHelper.getInstance().getBookingsRef().child(bookingId).setValue(booking)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);

                    Bundle bundle = new Bundle();
                    bundle.putString("bookingId", bookingId);
                    bundle.putString("cafeName", cafeName);
                    bundle.putString("dateTime", selectedDate + " " + startTime + " - " + endTime);
                    bundle.putString("status", booking.getStatus() != null ? booking.getStatus() : "requested");
                    bundle.putString("cafeId", cafeId != null ? cafeId : "");
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_booking_to_confirmation, bundle);
                })
                .addOnFailureListener(e -> finishBookingValidationWithError(e.getMessage()));
    }

    private void finishBookingValidationWithError(String message) {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnConfirm.setEnabled(true);
        }
        Toast.makeText(requireContext(),
                message != null && !message.isEmpty() ? message : getString(R.string.error),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
