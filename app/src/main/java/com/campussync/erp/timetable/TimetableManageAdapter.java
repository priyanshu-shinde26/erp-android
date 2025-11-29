package com.campussync.erp.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class TimetableManageAdapter
        extends RecyclerView.Adapter<TimetableManageAdapter.ManageViewHolder> {

    public interface TimetableActionListener {
        void onEditClicked(TimetableEntry entry);
        void onDeleteClicked(TimetableEntry entry);
    }

    private final List<TimetableEntry> items = new ArrayList<>();
    private final TimetableActionListener listener;

    public TimetableManageAdapter(TimetableActionListener listener) {
        this.listener = listener;
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
        TimetableEntry entry = items.get(position);

        holder.tvTime.setText(entry.getStartTime() + " - " + entry.getEndTime());
        holder.tvSubject.setText(entry.getSubjectName());
        holder.tvTeacher.setText(entry.getTeacherName());
        holder.tvRoom.setText(entry.getClassroom());
        holder.tvDay.setText(entry.getDayOfWeek());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(entry);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(entry);
        });
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

    static class ManageViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvSubject;
        TextView tvTeacher;
        TextView tvRoom;
        TextView tvDay;
        ImageButton btnEdit;
        ImageButton btnDelete;

        ManageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvDay = itemView.findViewById(R.id.tvDay);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
