package com.example.spot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spot.R;
import com.example.spot.models.CafeTable;

import java.util.List;

public class CafeTableAdapter extends RecyclerView.Adapter<CafeTableAdapter.ViewHolder> {

    private final List<CafeTable> tables;
    private final OnTableClickListener listener;

    public interface OnTableClickListener {
        void onTableClick(CafeTable table);
    }

    public CafeTableAdapter(List<CafeTable> tables, OnTableClickListener listener) {
        this.tables = tables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cafe_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CafeTable table = tables.get(position);

        holder.tvTableBadge.setText("T" + table.getTableNumber());
        holder.tvTableZone.setText(table.getZone() != null ? table.getZone() : "Table " + table.getTableNumber());
        holder.tvTableSeats.setText(table.getSeats() + " seats");

        String status = table.getStatus() != null ? table.getStatus() : "available";
        holder.tvTableStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

        int statusColor;
        switch (status) {
            case "available":
                statusColor = R.color.status_confirmed;
                break;
            case "reserved":
                statusColor = R.color.status_pending;
                break;
            case "occupied":
                statusColor = R.color.status_cancelled;
                break;
            default:
                statusColor = R.color.text_medium;
                break;
        }
        holder.tvTableStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));
        holder.viewStatusDot.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTableClick(table);
        });
    }

    @Override
    public int getItemCount() {
        return tables.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTableBadge, tvTableZone, tvTableSeats, tvTableStatus;
        View viewStatusDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableBadge = itemView.findViewById(R.id.tv_table_badge);
            tvTableZone = itemView.findViewById(R.id.tv_table_zone);
            tvTableSeats = itemView.findViewById(R.id.tv_table_seats);
            tvTableStatus = itemView.findViewById(R.id.tv_table_status);
            viewStatusDot = itemView.findViewById(R.id.view_status_dot);
        }
    }
}

