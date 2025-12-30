package com.campussync.erp.assignment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import java.util.ArrayList;
import java.util.List;

public class AdminAssignmentAdapter extends RecyclerView.Adapter<AdminAssignmentAdapter.ViewHolder> {
    private List<AssignmentItem> assignments = new ArrayList<>();
    private List<AssignmentItem> selectedItems = new ArrayList<>();
    private OnItemActionListener listener;
    private OnSelectionChangedListener selectionListener;
    private Context context;

    // 🔥 NEW: Selection change callback to notify Activity
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public interface OnItemActionListener {
        void onItemClick(AssignmentItem item);
        void onBulkDelete(List<AssignmentItem> selected);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setAssignments(List<AssignmentItem> assignments) {
        this.assignments = assignments != null ? assignments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<AssignmentItem> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedItems.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssignmentItem item = assignments.get(position);
        holder.bind(item, selectedItems.contains(item));
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvTitle, tvClassSubject, tvDueDate, tvStatusSummary, tvQuestionIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvClassSubject = itemView.findViewById(R.id.tv_class_subject);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvStatusSummary = itemView.findViewById(R.id.tv_status_summary);
            tvQuestionIndicator = itemView.findViewById(R.id.tv_question_indicator);
        }

        void bind(AssignmentItem item, boolean isSelected) {
            // ✅ ALL GETTERS - PERFECT
            tvTitle.setText(item.getTitle());
            tvClassSubject.setText("👥 Class: " + item.getClassName() + " • Subject: " + item.getSubject());
            tvDueDate.setText("📅 Due: " + item.getDueDateFormatted());

            // ✅ Public fields - Direct access OK
            tvStatusSummary.setText("📊 " + item.totalSubmissions + " subs | "
                    + item.gradedCount + " graded | " + item.pendingCount + " pending");

            tvQuestionIndicator.setVisibility(item.hasQuestionPdf ? View.VISIBLE : View.GONE);

            cbSelect.setChecked(isSelected);

            // 🔥 FIXED: Checkbox click listener (separate from item click)
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                AssignmentItem clickedItem = assignments.get(position);
                if (isChecked) {
                    if (!selectedItems.contains(clickedItem)) {
                        selectedItems.add(clickedItem);
                    }
                } else {
                    selectedItems.remove(clickedItem);
                }
                notifySelectionChanged(); // 🔥 Notify Activity
            });

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                AssignmentItem clickedItem = assignments.get(position);
                boolean newSelected = !selectedItems.contains(clickedItem);

                // 🔥 Toggle selection
                if (newSelected) {
                    selectedItems.add(clickedItem);
                } else {
                    selectedItems.remove(clickedItem);
                }

                cbSelect.setChecked(newSelected);
                notifySelectionChanged(); // 🔥 Notify Activity

                if (listener != null) {
                    listener.onItemClick(clickedItem);
                }
            });
        }
    }
}
