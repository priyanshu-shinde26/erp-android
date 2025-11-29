package com.campussync.erp.assignment;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentStatusItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AdminAssignmentAdapter extends RecyclerView.Adapter<AdminAssignmentAdapter.ViewHolder> {

    public interface Listener {
        void onStatusRequested(AssignmentItem item);
        void onSelectionChanged(List<String> selectedIds);
    }

    private final Context context;
    private List<AssignmentItem> items;
    private final Listener listener;

    // assignmentId -> status
    private final Map<String, AssignmentStatusItem> statusMap = new HashMap<>();
    // assignmentIds we already requested status for
    private final HashSet<String> statusRequested = new HashSet<>();
    // selected assignmentIds
    private final HashSet<String> selectedIds = new HashSet<>();

    public AdminAssignmentAdapter(Context context,
                                  List<AssignmentItem> items,
                                  Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<AssignmentItem> newItems) {
        this.items = newItems;
        statusMap.clear();
        statusRequested.clear();
        selectedIds.clear();
        notifySelectionChanged();
        notifyDataSetChanged();
    }

    public void setStatusForAssignment(String assignmentId, AssignmentStatusItem status) {
        if (assignmentId == null || status == null) return;
        statusMap.put(assignmentId, status);
        notifyItemChanged(findPositionById(assignmentId));
    }

    private int findPositionById(String assignmentId) {
        if (items == null) return RecyclerView.NO_POSITION;
        for (int i = 0; i < items.size(); i++) {
            AssignmentItem item = items.get(i);
            if (item.getId() != null && item.getId().equals(assignmentId)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectedIds());
        }
    }

    @NonNull
    @Override
    public AdminAssignmentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminAssignmentAdapter.ViewHolder holder, int position) {
        AssignmentItem item = items.get(position);
        String id = item.getId();

        holder.tvTitle.setText(item.getTitle());
        holder.tvClassSubject.setText("Class: " + item.getClassId() + " • Subject: " + item.getSubject());

        String dateStr = DateFormat.format("dd MMM yyyy, hh:mm a",
                new Date(item.getDueDate())).toString();
        holder.tvDueDate.setText("Due: " + dateStr);

        // Status summary
        AssignmentStatusItem status = statusMap.get(id);
        if (status != null) {
            String text = "Submissions: " + status.getTotalSubmissions()
                    + " | Graded: " + status.getGradedCount()
                    + " | Ungraded: " + status.getUngradedCount();
            holder.tvStatusSummary.setText(text);
        } else {
            holder.tvStatusSummary.setText("Status: loading...");
            // Ask activity to request status once
            if (!statusRequested.contains(id)) {
                statusRequested.add(id);
                if (listener != null) listener.onStatusRequested(item);
            }
        }

        // Selection check box
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedIds.contains(id));
        holder.cbSelect.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (id != null) {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.remove(id);
                }
                notifySelectionChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvTitle;
        TextView tvClassSubject;
        TextView tvDueDate;
        TextView tvStatusSummary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvClassSubject = itemView.findViewById(R.id.tv_class_subject);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvStatusSummary = itemView.findViewById(R.id.tv_status_summary);
        }
    }
}
