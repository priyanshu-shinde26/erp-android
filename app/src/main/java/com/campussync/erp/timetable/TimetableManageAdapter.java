package com.campussync.erp.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TimetableManageAdapter extends RecyclerView.Adapter<TimetableManageAdapter.ManageViewHolder> {

    public interface TimetableActionListener {
        void onEditClicked(SchedulePeriod period, String periodId);
        void onDeleteClicked(SchedulePeriod period, String periodId);
    }

    private final List<SchedulePeriod> items = new ArrayList<>();
    private final TimetableActionListener listener;

    private boolean editable = true;
    private String day = ""; // ✅ for showing day

    public TimetableManageAdapter(TimetableActionListener listener) {
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyDataSetChanged();
    }

    // ✅ call from activity: adapter.setDay(selectedDay)
    public void setDay(String day) {
        this.day = day == null ? "" : day.trim();
        notifyDataSetChanged();
    }

    public void setItems(List<SchedulePeriod> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_manage, parent, false);
        return new ManageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageViewHolder holder, int position) {
        SchedulePeriod period = items.get(position);
        if (period == null) return;

        holder.tvTime.setText(safe(period.getDisplayTime()));
        holder.tvSubject.setText("📚 " + safe(period.subject));

        // ✅ teacher name
        String teacher = safe(period.teacher);
        if (!teacher.isEmpty()) {
            holder.tvTeacher.setText("👨‍🏫 " + teacher);
            holder.tvTeacher.setVisibility(View.VISIBLE);
        } else {
            holder.tvTeacher.setVisibility(View.GONE);
        }

        // ✅ day
        if (!day.isEmpty()) {
            holder.tvDay.setText(capitalize(day));
            holder.tvDay.setVisibility(View.VISIBLE);
        } else {
            holder.tvDay.setVisibility(View.GONE);
        }

        // actions
        holder.btnEdit.setVisibility(editable ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(editable ? View.VISIBLE : View.GONE);

        String periodId = safe(period.id); // must come from backend
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(period, periodId);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(period, periodId);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static class ManageViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDay, tvSubject, tvTeacher;
        MaterialButton btnEdit, btnDelete;

        ManageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
