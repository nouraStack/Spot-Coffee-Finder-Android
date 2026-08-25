package com.example.spot.provider;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spot.R;
import com.example.spot.adapters.CafeTableAdapter;
import com.example.spot.adapters.ProviderBookingAdapter;
import com.example.spot.databinding.FragmentProviderBookingsBinding;
import com.example.spot.models.Booking;
import com.example.spot.models.CafeTable;
import com.example.spot.models.User;
import com.example.spot.utils.FirebaseHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProviderBookingsFragment extends Fragment {

    private FragmentProviderBookingsBinding binding;
    private ProviderBookingAdapter bookingAdapter;
    private CafeTableAdapter tableAdapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> displayedBookings = new ArrayList<>();
    private final List<CafeTable> tables = new ArrayList<>();
    private final Set<String> myCafeIds = new HashSet<>();
    private String selectedCafeId = null;
    private boolean showPendingOnly = true;
    private Booking selectedBookingForAssignment = null;
    private final Map<String, ValueEventListener> tableListenersByCafe = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProviderBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupTabs();
        setupBookingsRecycler();
        setupTablesRecycler();
        setupButtons();
        loadProviderCafes();
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.pending_bookings));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.all_bookings));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPendingOnly = tab.getPosition() == 0;
                filterBookings();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupBookingsRecycler() {
        bookingAdapter = new ProviderBookingAdapter(displayedBookings,
                new ProviderBookingAdapter.OnProviderActionListener() {
                    @Override
                    public void onApprove(Booking booking) {
                        selectedBookingForAssignment = booking;
                        Toast.makeText(requireContext(),
                                "Select an available table to confirm this booking",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReject(Booking booking) {
                        rejectBooking(booking);
                    }

                    @Override
                    public void onSuggest(Booking booking) {
                        showSuggestDialog(booking);
                    }

                    @Override
                    public void onChat(Booking booking) {
                        Bundle args = new Bundle();
                        args.putString("bookingId", booking.getBookingId());
                        args.putString("cafeId", booking.getCafeId() != null ? booking.getCafeId() : "");
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_providerBookings_to_chat, args);
                    }
                });
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(bookingAdapter);
    }

    private void setupTablesRecycler() {
        tableAdapter = new CafeTableAdapter(tables, table -> {
            if (selectedBookingForAssignment != null && "available".equals(table.getStatus())) {
                assignTableAndConfirm(selectedBookingForAssignment, table);
            } else {
                showTableStatusDialog(table);
            }
        });
        binding.rvTables.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTables.setAdapter(tableAdapter);
    }

    private void setupButtons() {
        binding.btnAddTable.setOnClickListener(v -> showAddTableDialog());

        binding.btnAssignTable.setOnClickListener(v -> {
            for (Booking b : displayedBookings) {
                if ("requested".equals(b.getStatus())) {
                    selectedBookingForAssignment = b;
                    Toast.makeText(requireContext(),
                            "Tap an available table to confirm for " +
                                    (b.getCustomerName() != null ? b.getCustomerName() : "customer"),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Toast.makeText(requireContext(), "No requested booking to assign", Toast.LENGTH_SHORT).show();
        });

        binding.btnRejectBooking.setOnClickListener(v -> {
            for (Booking b : displayedBookings) {
                if ("requested".equals(b.getStatus())) {
                    rejectBooking(b);
                    return;
                }
            }
            Toast.makeText(requireContext(), "No requested booking to reject", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProviderCafes() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseHelper.getInstance().getCurrentUserId();
        if (uid == null) return;

        FirebaseHelper.getInstance().getCafesRef()
                .orderByChild("ownerId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        myCafeIds.clear();
                        clearTableListeners();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            myCafeIds.add(child.getKey());
                            if (selectedCafeId == null) selectedCafeId = child.getKey();
                        }

                        if (myCafeIds.isEmpty()) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        loadBookings();
                        loadTables();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadBookings() {
        FirebaseHelper.getInstance().getBookingsRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        allBookings.clear();

                        List<String> userIdsToLoad = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Booking booking = child.getValue(Booking.class);
                            if (booking != null && myCafeIds.contains(booking.getCafeId())) {
                                booking.setBookingId(child.getKey());
                                if ("confirmed".equals(booking.getStatus())
                                        && FirebaseHelper.getInstance().isBookingExpired(booking)) {
                                    FirebaseHelper.getInstance().completeBookingAndReleaseTable(booking.getBookingId(), null);
                                    continue;
                                }
                                allBookings.add(booking);
                                if ((booking.getCustomerName() == null || booking.getCustomerName().isEmpty())
                                        && booking.getUserId() != null) {
                                    userIdsToLoad.add(booking.getUserId());
                                }
                            }
                        }

                        if (!userIdsToLoad.isEmpty()) {
                            loadCustomerNames(userIdsToLoad);
                        } else {
                            filterBookings();
                        }

                        boolean hasActionable = false;
                        for (Booking b : allBookings) {
                            if ("requested".equals(b.getStatus())) {
                                hasActionable = true;
                                break;
                            }
                        }
                        binding.layoutBottomActions.setVisibility(hasActionable ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadCustomerNames(List<String> userIds) {
        final int[] loaded = {0};
        Set<String> uniqueIds = new HashSet<>(userIds);

        for (String userId : uniqueIds) {
            FirebaseHelper.getInstance().getUsersRef().child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                for (Booking b : allBookings) {
                                    if (userId.equals(b.getUserId())) {
                                        b.setCustomerName(user.getName());
                                    }
                                }
                            }
                            loaded[0]++;
                            if (loaded[0] == uniqueIds.size()) filterBookings();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                            if (loaded[0] == uniqueIds.size()) filterBookings();
                        }
                    });
        }
    }

    private void loadTables() {
        if (myCafeIds.isEmpty()) return;
        clearTableListeners();
        tables.clear();
        if (tableAdapter != null) {
            tableAdapter.notifyDataSetChanged();
        }

        for (String cafeId : myCafeIds) {
            FirebaseHelper.getInstance().markExpiredConfirmedBookingsForCafe(cafeId);
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (binding == null) return;
                    rebuildTablesList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
            tableListenersByCafe.put(cafeId, listener);
            FirebaseHelper.getInstance().getTablesRef().child(cafeId).addValueEventListener(listener);
        }
    }

    private void rebuildTablesList() {
        FirebaseHelper.getInstance().getTablesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                tables.clear();
                for (String cafeId : myCafeIds) {
                    DataSnapshot cafeTablesSnapshot = snapshot.child(cafeId);
                    for (DataSnapshot child : cafeTablesSnapshot.getChildren()) {
                        CafeTable table = child.getValue(CafeTable.class);
                        if (table != null) {
                            table.setCafeId(cafeId);
                            table.setTableId(child.getKey());
                            tables.add(table);
                        }
                    }
                }
                tableAdapter.notifyDataSetChanged();
                binding.tvNoTables.setVisibility(tables.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void clearTableListeners() {
        for (Map.Entry<String, ValueEventListener> entry : tableListenersByCafe.entrySet()) {
            FirebaseHelper.getInstance().getTablesRef().child(entry.getKey())
                    .removeEventListener(entry.getValue());
        }
        tableListenersByCafe.clear();
    }

    private void filterBookings() {
        if (binding == null) return;
        displayedBookings.clear();
        for (Booking b : allBookings) {
            if (showPendingOnly) {
                String status = b.getStatus() != null ? b.getStatus() : "requested";
                if ("requested".equals(status)) {
                    displayedBookings.add(b);
                }
            } else {
                displayedBookings.add(b);
            }
        }
        bookingAdapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(displayedBookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void rejectBooking(Booking booking) {
        if (booking.getBookingId() == null) return;
        FirebaseHelper.getInstance().getBookingsRef()
                .child(booking.getBookingId())
                .child("status").setValue("rejected")
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.booking_request_rejected, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSuggestDialog(Booking booking) {
        if (!isAdded()) return;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText dateInput = new EditText(requireContext());
        dateInput.setHint("2026-02-25");
        layout.addView(dateInput);

        EditText startInput = new EditText(requireContext());
        startInput.setHint("Start HH:mm");
        layout.addView(startInput);

        EditText endInput = new EditText(requireContext());
        endInput.setHint("End HH:mm");
        layout.addView(endInput);

        EditText noteInput = new EditText(requireContext());
        noteInput.setHint("Reason / note");
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(noteInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.suggest_another_time)
                .setView(layout)
                .setPositiveButton("Send", (dialog, which) -> {
                    String proposedDate = dateInput.getText().toString().trim();
                    String proposedStart = startInput.getText().toString().trim();
                    String proposedEnd = endInput.getText().toString().trim();
                    String note = noteInput.getText().toString().trim();

                    if (TextUtils.isEmpty(proposedDate) || TextUtils.isEmpty(proposedStart) || TextUtils.isEmpty(proposedEnd)) {
                        Toast.makeText(requireContext(), "Enter full proposed time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("status")
                            .setValue("awaiting_customer_decision");
                    FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalDate")
                            .setValue(proposedDate);
                    FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalStartTime")
                            .setValue(proposedStart);
                    FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("proposalEndTime")
                            .setValue(proposedEnd);
                    FirebaseHelper.getInstance().getBookingsRef().child(booking.getBookingId()).child("notes")
                            .setValue(note);

                    Toast.makeText(requireContext(), "Alternative time sent to customer", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void assignTableAndConfirm(Booking booking, CafeTable table) {
        String bookingCafeId = booking.getCafeId();
        if (booking.getBookingId() == null || table.getTableId() == null || bookingCafeId == null) return;
        if (table.getCafeId() == null || !bookingCafeId.equals(table.getCafeId())) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Choose a table from the same cafe as the booking", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String tableLabel = "T" + table.getTableNumber() + " - " + table.getZone();
        FirebaseHelper.getInstance().assignTableToBooking(booking, bookingCafeId, table.getTableId(), tableLabel,
                (success, message) -> {
                    if (!isAdded()) return;
                    if (success) {
                        selectedBookingForAssignment = null;
                        Toast.makeText(requireContext(), R.string.table_assigned, Toast.LENGTH_SHORT).show();
                        // Open chat with customer
                        Bundle args = new Bundle();
                        args.putString("bookingId", booking.getBookingId());
                        args.putString("cafeId", bookingCafeId);
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_providerBookings_to_chat, args);
                    } else {
                        Toast.makeText(requireContext(),
                                message != null ? message : getString(R.string.error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTableStatusDialog(CafeTable table) {
        if (!isAdded() || table.getCafeId() == null || table.getTableId() == null) return;

        String currentStatus = table.getStatus() != null ? table.getStatus() : "available";
        final String[] statuses = {"available", "occupied", "reserved"};
        final String[] labels = {"Available", "Unavailable / Occupied", "Reserved"};

        int checkedItem = -1;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(currentStatus)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Table " + table.getTableNumber() + " - " + table.getZone())
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    String newStatus = statuses[which];
                    if (!newStatus.equals(currentStatus)) {
                        FirebaseHelper.getInstance().getTablesRef()
                                .child(table.getCafeId())
                                .child(table.getTableId())
                                .child("status")
                                .setValue(newStatus)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                "Table status updated to " + labels[which],
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTableDialog() {
        if (!isAdded() || selectedCafeId == null) {
            Toast.makeText(requireContext(), "Please add a café first", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etNumber = new EditText(requireContext());
        etNumber.setHint("Table number (e.g. 1)");
        etNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etNumber);

        EditText etZone = new EditText(requireContext());
        etZone.setHint("Zone (e.g. Window, Quiet Zone)");
        layout.addView(etZone);

        EditText etSeats = new EditText(requireContext());
        etSeats.setHint("Seats (e.g. 2)");
        etSeats.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etSeats);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_table)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String numStr = etNumber.getText().toString().trim();
                    String zone = etZone.getText().toString().trim();
                    String seatsStr = etSeats.getText().toString().trim();

                    if (TextUtils.isEmpty(numStr) || TextUtils.isEmpty(zone) || TextUtils.isEmpty(seatsStr)) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int number = Integer.parseInt(numStr);
                    int seats = Integer.parseInt(seatsStr);
                    String tableId = FirebaseHelper.getInstance().getTablesRef().child(selectedCafeId).push().getKey();
                    if (tableId == null) return;

                    CafeTable table = new CafeTable(tableId, selectedCafeId, number, zone, seats);
                    FirebaseHelper.getInstance().getTablesRef().child(selectedCafeId).child(tableId).setValue(table)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Table added!", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(R.string.cancel_booking, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        clearTableListeners();
        super.onDestroyView();
        binding = null;
    }
}
