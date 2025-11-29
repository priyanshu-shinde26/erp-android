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

public class TimetableViewAdapter extends RecyclerView.Adapter<TimetableViewAdapter.TimetableViewHolder> {

    private final List<TimetableEntry> items = new ArrayList<>();

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable_view, parent, false);
        return new TimetableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimetableViewHolder holder, int position) {
        TimetableEntry entry = items.get(position);

        holder.tvTime.setText(entry.getStartTime() + " - " + entry.getEndTime());
        holder.tvSubject.setText(entry.getSubjectName());
        holder.tvTeacher.setText(entry.getTeacherName());
        holder.tvRoom.setText(entry.getClassroom());
        holder.tvDay.setText(entry.getDayOfWeek());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<TimetableEntry> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvSubject;
        TextView tvTeacher;
        TextView tvRoom;
        TextView tvDay;

        TimetableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvDay = itemView.findViewById(R.id.tvDay);
        }
    }
}
