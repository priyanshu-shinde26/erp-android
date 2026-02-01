package com.campussync.erp.academics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class StudentMarksAdapter
        extends RecyclerView.Adapter<StudentMarksAdapter.VH> {

    private final List<ResultEntity> list = new ArrayList<>();
    public StudentMarksAdapter() {
        // ✅ NO arguments
    }
    // ================= UPDATE DATA =================
    public void update(List<ResultEntity> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    // ================= CREATE VIEW =================
    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.row_student_result, // ✅ MUST EXIST
                        parent,
                        false
                );

        return new VH(v);
    }

    // ================= BIND VIEW =================
    @Override
    public void onBindViewHolder(
            @NonNull VH h,
            int position) {

        ResultEntity e = list.get(position);

        h.tvSubject.setText(e.subject);
        h.tvTest.setText(e.test);
        h.tvMarks.setText(
                e.marks + " / " + e.maxMarks
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDER =================
    static class VH extends RecyclerView.ViewHolder {

        TextView tvSubject, tvTest, tvMarks;

        VH(@NonNull View itemView) {
            super(itemView);

            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTest = itemView.findViewById(R.id.tvTest);
            tvMarks = itemView.findViewById(R.id.tvMarks);
        }
    }
}
