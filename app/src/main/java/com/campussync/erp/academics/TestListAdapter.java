package com.campussync.erp.academics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class TestListAdapter
        extends RecyclerView.Adapter<TestListAdapter.VH> {

    // ================= CALLBACKS =================
    public interface OnTestClick {
        void onTestClick(TestModel test);
    }

    public interface OnDeleteClick {
        void onDelete(TestModel test);
    }

    private final List<TestModel> list = new ArrayList<>();
    private final String role;
    private final OnTestClick testClick;
    private final OnDeleteClick deleteClick;

    // ================= CONSTRUCTOR =================
    public TestListAdapter(
            String role,
            OnTestClick testClick,
            OnDeleteClick deleteClick
    ) {
        this.role = role;
        this.testClick = testClick;
        this.deleteClick = deleteClick;
    }

    // ================= DATA =================
    public void update(List<TestModel> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    // ================= VIEW HOLDER =================
    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_test_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH h,
            int pos) {

        TestModel t = list.get(pos);

        h.tvTestName.setText(t.testName);
        h.tvTestType.setText(t.testType);
        h.tvExamDate.setText(t.examDate);
        h.tvMaxMarks.setText("Max: " + t.maxMarks);

        // ▶ OPEN MARKS ENTRY
        h.itemView.setOnClickListener(v ->
                testClick.onTestClick(t)
        );

        // 🗑 DELETE (ROLE BASED + NULL GUARD)
        // ✅ Added null check to prevent crash if XML ID mismatch occurs
        if (h.ivDelete != null) {
            if ("STUDENT".equals(role)) {
                h.ivDelete.setVisibility(View.GONE);
            } else {
                h.ivDelete.setVisibility(View.VISIBLE);
                h.ivDelete.setOnClickListener(v ->
                        deleteClick.onDelete(t)
                );
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VH =================
    static class VH extends RecyclerView.ViewHolder {

        TextView tvTestName, tvTestType, tvExamDate, tvMaxMarks;
        ImageView ivDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTestName = itemView.findViewById(R.id.tvTestName);
            tvTestType = itemView.findViewById(R.id.tvTestType);
            tvExamDate = itemView.findViewById(R.id.tvExamDate);
            tvMaxMarks = itemView.findViewById(R.id.tvMaxMarks);

            // 🔴 Ensure this ID matches android:id="@+id/btnDelete" in row_test_item.xml
            ivDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}