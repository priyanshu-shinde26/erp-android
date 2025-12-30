package com.campussync.erp.assignment;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.google.android.material.button.MaterialButton;

import java.util.Date;
import java.util.List;

/**
 * Adapter for teacher assignment list.
 * Shows question status:
 *   - "Question: attached" if questionFileUrl != null
 *   - "Question: none" otherwise
 */
public class TeacherAssignmentAdapter extends RecyclerView.Adapter<TeacherAssignmentAdapter.ViewHolder> {

    // FIXED: Interface defined HERE in Adapter (not in Activity)
    public interface Listener {
        void onUploadQuestionClicked(AssignmentItem item);
        void onViewSubmissionsClicked(AssignmentItem item);
        void onEditAssignmentClicked(AssignmentItem item);
        void onDeleteAssignmentClicked(AssignmentItem item);
    }

    private final Context context;
    private List<AssignmentItem> items;
    private final Listener listener;

    public TeacherAssignmentAdapter(Context context,
                                    List<AssignmentItem> items,
                                    Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<AssignmentItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeacherAssignmentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_teacher_assignment,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherAssignmentAdapter.ViewHolder holder, int position) {
        AssignmentItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvClassSubject.setText("Class: " + item.getClassId() + " • Subject: " + item.getSubject());
        holder.tvDescription.setText(item.getDescription());

        String dateStr = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(item.getDueDate())).toString();
        holder.tvDueDate.setText("Due: " + dateStr);

        // FIXED: Correct method name
        if (!TextUtils.isEmpty(item.getQuestionFileUrl())) {
            holder.tvQuestionStatus.setText("Question: attached");
            holder.btnUploadQuestion.setText("Replace Question");
        } else {
            holder.tvQuestionStatus.setText("Question: none");
            holder.btnUploadQuestion.setText("Upload Question");
        }

        holder.btnUploadQuestion.setOnClickListener(v -> {
            if (listener != null) listener.onUploadQuestionClicked(item);
        });

        holder.btnViewSubmissions.setOnClickListener(v -> {
            if (listener != null) listener.onViewSubmissionsClicked(item);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditAssignmentClicked(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAssignmentClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvClassSubject;
        TextView tvDueDate;
        TextView tvDescription;
        TextView tvQuestionStatus;
        MaterialButton btnUploadQuestion;
        MaterialButton btnViewSubmissions;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvClassSubject = itemView.findViewById(R.id.tv_class_subject);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvQuestionStatus = itemView.findViewById(R.id.tv_question_status);
            btnUploadQuestion = itemView.findViewById(R.id.btn_upload_question);
            btnViewSubmissions = itemView.findViewById(R.id.btn_view_submissions);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
