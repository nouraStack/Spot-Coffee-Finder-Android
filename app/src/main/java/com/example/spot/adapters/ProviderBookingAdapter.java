package com.example.spot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spot.R;
import com.example.spot.models.Booking;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ProviderBookingAdapter extends RecyclerView.Adapter<ProviderBookingAdapter.ViewHolder> {

    private final List<Booking> bookings;
    private final OnProviderActionListener listener;

    public interface OnProviderActionListener {
        void onApprove(Booking booking);
        void onReject(Booking booking);
        void onSuggest(Booking booking);
        void onChat(Booking booking);
    }

    public ProviderBookingAdapter(List<Booking> bookings, OnProviderActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        String name = booking.getCustomerName() != null && !booking.getCustomerName().isEmpty()
                ? booking.getCustomerName() : "Customer";
        holder.tvCustomerName.setText(name);
        holder.tvAvatar.setText(name.substring(0, 1).toUpperCase());

        holder.tvDate.setText(booking.getDate() != null ? booking.getDate() : "");
        String time = (booking.getStartTime() != null ? booking.getStartTime() : "") +
                " — " +
                (booking.getEndTime() != null ? booking.getEndTime() : "");
        holder.tvTime.setText(time);

        holder.tvGuests.setText(String.format(Locale.US, "%d Guest%s",
                booking.getGuests(), booking.getGuests() > 1 ? "s" : ""));

        String notes = booking.getNotes();
        if (notes != null && !notes.isEmpty()) {
            holder.layoutNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(notes);
        } else {
            holder.layoutNotes.setVisibility(View.GONE);
        }

        holder.tvPrice.setText(String.format(Locale.US, "SAR%.0f", booking.getTotalPrice()));

        String status = booking.getStatus() != null ? booking.getStatus() : "requested";
        String statusLabel;
        int statusColor;

        switch (status) {
            case "confirmed":
                statusLabel = "Confirmed";
                statusColor = R.color.status_confirmed;
                holder.layoutActions.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.VISIBLE);
                break;
            case "requested":
                statusLabel = "Requested";
                statusColor = R.color.status_pending;
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnChat.setVisibility(View.GONE);
                break;
            case "awaiting_customer_decision":
                statusLabel = "Awaiting Customer";
                statusColor = R.color.gold_dark;
                holder.layoutActions.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.GONE);
                break;
            case "cancelled":
            case "rejected":
                statusLabel = "Rejected";
                statusColor = R.color.status_cancelled;
                holder.layoutActions.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.GONE);
                break;
            default:
                statusLabel = "Completed";
                statusColor = R.color.text_medium;
                holder.layoutActions.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.VISIBLE);
                break;
        }

        holder.tvStatus.setText(statusLabel);
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(booking));
        holder.btnReject.setOnClickListener(v -> listener.onReject(booking));
        holder.btnSuggest.setOnClickListener(v -> listener.onSuggest(booking));
        holder.btnChat.setOnClickListener(v -> listener.onChat(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvCustomerName, tvStatus, tvDate, tvTime, tvGuests, tvNotes, tvPrice;
        LinearLayout layoutNotes, layoutActions;
        MaterialButton btnApprove, btnReject, btnChat;
        TextView btnSuggest;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvGuests = itemView.findViewById(R.id.tv_guests);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            tvPrice = itemView.findViewById(R.id.tv_price);
            layoutNotes = itemView.findViewById(R.id.layout_notes);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnSuggest = itemView.findViewById(R.id.btn_suggest);
            btnChat = itemView.findViewById(R.id.btn_chat);
        }
    }
}
