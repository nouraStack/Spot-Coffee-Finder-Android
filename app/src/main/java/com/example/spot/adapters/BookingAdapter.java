package com.example.spot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spot.R;
import com.example.spot.models.Booking;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<Booking> bookings;
    private final OnCancelClickListener listener;

    public interface OnCancelClickListener {
        void onCancel(Booking booking);
        void onChat(Booking booking);
    }

    public BookingAdapter(List<Booking> bookings, OnCancelClickListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        holder.tvCafeName.setText(booking.getCafeName() != null ? booking.getCafeName() : "Café");
        holder.tvDate.setText(booking.getDate() != null ? booking.getDate() : "");
        holder.tvTime.setText(String.format(Locale.US, "%s - %s",
                booking.getStartTime() != null ? booking.getStartTime() : "",
                booking.getEndTime() != null ? booking.getEndTime() : ""));
        holder.tvPrice.setText(String.format(Locale.US, "%.0f SAR", booking.getTotalPrice()));

        String status = booking.getStatus() != null ? booking.getStatus() : "requested";
        String statusLabel;
        int statusColor;

        switch (status) {
            case "confirmed":
                statusLabel = holder.itemView.getContext().getString(R.string.confirmed);
                statusColor = R.color.status_confirmed;
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setText(R.string.cancel);
                holder.btnChat.setVisibility(View.VISIBLE);
                break;
            case "awaiting_customer_decision":
                statusLabel = holder.itemView.getContext().getString(R.string.awaiting_customer_decision);
                statusColor = R.color.status_pending;
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setText(R.string.decline_proposal);
                holder.btnChat.setVisibility(View.GONE);
                break;
            case "requested":
                statusLabel = holder.itemView.getContext().getString(R.string.requested);
                statusColor = R.color.status_pending;
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setText(R.string.cancel);
                holder.btnChat.setVisibility(View.GONE);
                break;
            case "rejected":
                statusLabel = "Rejected";
                statusColor = R.color.status_cancelled;
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.GONE);
                break;
            case "cancelled":
                statusLabel = holder.itemView.getContext().getString(R.string.cancelled);
                statusColor = R.color.status_cancelled;
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.GONE);
                break;
            default:
                statusLabel = holder.itemView.getContext().getString(R.string.completed);
                statusColor = R.color.text_medium;
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnChat.setVisibility(View.VISIBLE);
                break;
        }

        holder.tvStatus.setText(statusLabel);
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));

        if ("awaiting_customer_decision".equals(status)
                && booking.getProposalDate() != null && !booking.getProposalDate().isEmpty()) {
            holder.tvAvailableTimeLabel.setVisibility(View.VISIBLE);
            holder.tvAvailableTime.setVisibility(View.VISIBLE);
            holder.tvAvailableTime.setText(holder.itemView.getContext().getString(
                    R.string.suggested_time_message,
                    booking.getProposalDate(),
                    booking.getProposalStartTime(),
                    booking.getProposalEndTime()));
        } else {
            holder.tvAvailableTimeLabel.setVisibility(View.GONE);
            holder.tvAvailableTime.setVisibility(View.GONE);
        }

        String assignedTableLabel = booking.getAssignedTableLabel();
        if (assignedTableLabel != null && !assignedTableLabel.isEmpty()) {
            holder.tvTableInfo.setVisibility(View.VISIBLE);
            holder.tvTableInfo.setText(holder.itemView.getContext().getString(
                    R.string.assigned_table, assignedTableLabel));
        } else {
            holder.tvTableInfo.setVisibility(View.GONE);
        }

        String notes = booking.getNotes() != null ? booking.getNotes() : "";

        if ("rejected".equals(status)) {
            notes = "Booking request rejected.";
        } else if ("cancelled".equals(status)) {
            notes = "Booking cancelled by customer.";
        } else if ("confirmed".equals(status)) {
            notes = "Reservation confirmed. Your table is ready.";
        } else if ("requested".equals(status)) {
            notes = "Your request is waiting for provider review.";
        }

        if (!notes.isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(notes);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        holder.btnCancel.setOnClickListener(v -> listener.onCancel(booking));
        holder.btnChat.setOnClickListener(v -> listener.onChat(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCafeName, tvStatus, tvDate, tvTime, tvPrice, tvNotes, tvTableInfo,
                tvAvailableTimeLabel, tvAvailableTime;
        MaterialButton btnCancel, btnChat;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCafeName = itemView.findViewById(R.id.tv_cafe_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            tvTableInfo = itemView.findViewById(R.id.tv_table_info);
            tvAvailableTimeLabel = itemView.findViewById(R.id.tv_available_time_label);
            tvAvailableTime = itemView.findViewById(R.id.tv_available_time);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnChat = itemView.findViewById(R.id.btn_chat);
        }
    }
}
