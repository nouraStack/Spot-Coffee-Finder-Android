package com.example.spot.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.spot.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private FirebaseDatabase database;
    private FirebaseAuth auth;

    private FirebaseHelper() {
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    private FirebaseDatabase getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
        }
        return database;
    }

    public DatabaseReference getUsersRef() {
        return getDatabase().getReference("Users");
    }

    public DatabaseReference getCafesRef() {
        return getDatabase().getReference("Cafes");
    }

    public DatabaseReference getBookingsRef() {
        return getDatabase().getReference("Bookings");
    }

    public DatabaseReference getRatingsRef() {
        return getDatabase().getReference("Ratings");
    }

    public DatabaseReference getFavoritesRef() {
        return getDatabase().getReference("Favorites");
    }

    public DatabaseReference getTablesRef() {
        return getDatabase().getReference("Tables");
    }

    public String getCurrentUserId() {
        FirebaseAuth firebaseAuth = getAuth();
        if (firebaseAuth.getCurrentUser() != null) {
            return firebaseAuth.getCurrentUser().getUid();
        }
        return null;
    }

    public DatabaseReference getCurrentUserRef() {
        String uid = getCurrentUserId();
        if (uid != null) {
            return getUsersRef().child(uid);
        }
        return null;
    }

    public interface BookingActionCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    public void cancelBookingAndReleaseTable(@Nullable String bookingId, @Nullable BookingActionCallback callback) {
        updateBookingStatusAndTable(bookingId, "cancelled", "Booking cancelled by customer", true, callback);
    }

    public void completeBookingAndReleaseTable(@Nullable String bookingId, @Nullable BookingActionCallback callback) {
        updateBookingStatusAndTable(bookingId, "completed", "Reservation time ended", true, callback);
    }

    public void markExpiredConfirmedBookingsForCafe(@Nullable String cafeId) {
        if (cafeId == null || cafeId.trim().isEmpty()) return;
        getBookingsRef().orderByChild("cafeId").equalTo(cafeId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Booking booking = child.getValue(Booking.class);
                            if (booking == null) continue;
                            booking.setBookingId(child.getKey());
                            if (!"confirmed".equals(booking.getStatus())) continue;
                            if (isBookingExpired(booking)) {
                                completeBookingAndReleaseTable(booking.getBookingId(), null);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
                });
    }

    public boolean isBookingExpired(@Nullable Booking booking) {
        if (booking == null) return false;
        String date = booking.getDate();
        String endTime = booking.getEndTime();
        if (date == null || endTime == null || date.isEmpty() || endTime.isEmpty()) return false;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getDefault());
        try {
            java.util.Date parsedDate = format.parse(date + " " + endTime);
            if (parsedDate == null) return false;
            long endMillis = parsedDate.getTime();
            return System.currentTimeMillis() >= endMillis;
        } catch (ParseException e) {
            return false;
        }
    }

    public void assignTableToBooking(@Nullable Booking booking,
                                     @Nullable String cafeId,
                                     @Nullable String tableId,
                                     @Nullable String tableLabel,
                                     @Nullable BookingActionCallback callback) {
        if (booking == null || booking.getBookingId() == null || cafeId == null || tableId == null) {
            if (callback != null) callback.onComplete(false, "Missing booking or table data");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        String bookingPath = "/Bookings/" + booking.getBookingId() + "/";
        String tablePath = "/Tables/" + cafeId + "/" + tableId + "/";

        updates.put(bookingPath + "assignedTableId", tableId);
        updates.put(bookingPath + "assignedTableLabel", tableLabel != null ? tableLabel : "");
        updates.put(bookingPath + "status", "confirmed");
        updates.put(bookingPath + "notes", "Reservation confirmed. Your table is ready.");

        updates.put(tablePath + "status", "reserved");
        updates.put(tablePath + "assignedBookingId", booking.getBookingId());
        updates.put(tablePath + "reservedDate", booking.getDate() != null ? booking.getDate() : "");
        updates.put(tablePath + "reservedStartTime", booking.getStartTime() != null ? booking.getStartTime() : "");
        updates.put(tablePath + "reservedEndTime", booking.getEndTime() != null ? booking.getEndTime() : "");

        getDatabase().getReference().updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onComplete(false, e.getMessage());
                });
    }

    private void updateBookingStatusAndTable(@Nullable String bookingId,
                                             @NonNull String newStatus,
                                             @NonNull String note,
                                             boolean clearProposal,
                                             @Nullable BookingActionCallback callback) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            if (callback != null) callback.onComplete(false, "Booking not found");
            return;
        }

        getBookingsRef().child(bookingId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Booking booking = snapshot.getValue(Booking.class);
                if (booking == null) {
                    if (callback != null) callback.onComplete(false, "Booking not found");
                    return;
                }
                booking.setBookingId(snapshot.getKey());

                Map<String, Object> updates = new HashMap<>();
                String bookingPath = "/Bookings/" + bookingId + "/";
                updates.put(bookingPath + "status", newStatus);
                updates.put(bookingPath + "notes", note);

                if (clearProposal) {
                    updates.put(bookingPath + "proposalDate", "");
                    updates.put(bookingPath + "proposalStartTime", "");
                    updates.put(bookingPath + "proposalEndTime", "");
                }

                String cafeId = booking.getCafeId();
                String assignedTableId = booking.getAssignedTableId();
                if (cafeId != null && !cafeId.isEmpty() && assignedTableId != null && !assignedTableId.isEmpty()) {
                    String tablePath = "/Tables/" + cafeId + "/" + assignedTableId + "/";
                    updates.put(tablePath + "status", "available");
                    updates.put(tablePath + "assignedBookingId", "");
                    updates.put(tablePath + "reservedDate", "");
                    updates.put(tablePath + "reservedStartTime", "");
                    updates.put(tablePath + "reservedEndTime", "");

                    updates.put(bookingPath + "assignedTableId", "");
                    updates.put(bookingPath + "assignedTableLabel", "");
                }

                getDatabase().getReference().updateChildren(updates)
                        .addOnSuccessListener(unused -> {
                            if (callback != null) callback.onComplete(true, null);
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onComplete(false, e.getMessage());
                        });
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                if (callback != null) callback.onComplete(false, error.getMessage());
            }
        });
    }
}
