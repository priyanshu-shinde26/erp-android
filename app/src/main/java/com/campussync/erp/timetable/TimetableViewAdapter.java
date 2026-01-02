package com.campussync.erp.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class TimetableViewAdapter extends RecyclerView.Adapter<TimetableViewAdapter.ViewHolder> {

    private final List<SchedulePeriod> items = new ArrayList<>();
    private String currentDay = ""; // ✅ Stores the day name

    public void setItems(List<SchedulePeriod> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ✅ NEW: Method to set the current day from the Activity
    public void setDay(String day) {
        this.currentDay = (day != null) ? day : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SchedulePeriod p = items.get(position);
        if (p == null) return;

        holder.tvTime.setText(safe(p.getDisplayTime()));
        holder.tvSubject.setText("📚 " + safe(p.subject));

        // ✅ Handle Teacher Display
        String teacher = safe(p.teacher);
        if (!teacher.isEmpty()) {
            holder.tvTeacher.setText("👨‍🏫 " + teacher);
            holder.tvTeacher.setVisibility(View.VISIBLE);
        } else {
            holder.tvTeacher.setVisibility(View.GONE);
        }

        // ✅ Handle Day Display
        if (!currentDay.isEmpty()) {
            String capitalizedDay = currentDay.substring(0, 1).toUpperCase() + currentDay.substring(1);
            holder.tvDay.setText(capitalizedDay);
            holder.tvDay.setVisibility(View.VISIBLE);
        } else {
            holder.tvDay.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSubject, tvTeacher, tvDay; // ✅ Added tvDay

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
            tvDay = itemView.findViewById(R.id.tvDay); // ✅ Initialize tvDay
        }
    }
}
