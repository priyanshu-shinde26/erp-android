package com.campussync.erp.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campussync.erp.R;
import java.util.ArrayList;
import java.util.List;

public class StudentAttendanceAdapter
        extends RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder> {

    // ✅ FIX: Use your existing interface name
    public interface OnAttendanceClickListener {
        void onAttendanceClick(String rollNo, String status);
        void onViewSummaryClick(String rollNo);
    }

    private final List<StudentModel> students = new ArrayList<>();
    private final OnAttendanceClickListener listener;  // ✅ Match your existing interface

    public StudentAttendanceAdapter(OnAttendanceClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<StudentModel> list) {
        students.clear();
        students.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentModel student = students.get(position);
        holder.name.setText(student.name);
        holder.roll.setText("Roll: " + student.rollNo);

        // ✅ Use your existing method names
        holder.btnPresent.setOnClickListener(v ->
                listener.onAttendanceClick(student.rollNo, "PRESENT"));

        holder.btnAbsent.setOnClickListener(v ->
                listener.onAttendanceClick(student.rollNo, "ABSENT"));

        holder.btnSummary.setOnClickListener(v ->
                listener.onViewSummaryClick(student.rollNo));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, roll;
        Button btnPresent, btnAbsent, btnSummary;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tvStudentName);
            roll = view.findViewById(R.id.tvStudentRoll);
            btnPresent = view.findViewById(R.id.btnPresent);
            btnAbsent = view.findViewById(R.id.btnAbsent);
            btnSummary = view.findViewById(R.id.btnViewSummary);
        }
    }
}
