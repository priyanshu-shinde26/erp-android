package com.campussync.erp.assignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.google.android.material.button.MaterialButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for student assignment list.
 */
public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    // ===== Grade view support =====
    private final Map<String, GradeInfo> gradeMap = new HashMap<>();

    public static class GradeInfo {
        public Integer marks;
        public String feedback;

        public GradeInfo(Integer marks, String feedback) {
            this.marks = marks;
            this.feedback = feedback;
        }
    }

    public void setGradeForAssignment(String assignmentId, Integer marks, String feedback) {
        if (assignmentId == null) return;
        gradeMap.put(assignmentId, new GradeInfo(marks, feedback));
        notifyDataSetChanged();
    }

    // ===== Listener =====
    public interface Listener {
        void onSubmitPdfClicked(AssignmentItem item);
    }

    private final Context context;
    private List<AssignmentItem> items;
    private final Listener listener;

    // Local submitted ids
    private final Set<String> submittedAssignmentIds = new HashSet<>();

    public StudentAssignmentAdapter(Context context,
                                    List<AssignmentItem> items,
                                    Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setSubmittedAssignments(Set<String> ids) {
        submittedAssignmentIds.clear();
        if (ids != null) {
            submittedAssignmentIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setItems(List<AssignmentItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void markSubmitted(String assignmentId) {
        if (assignmentId != null) {
            submittedAssignmentIds.add(assignmentId);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public StudentAssignmentAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(
                R.layout.item_student_assignment,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull StudentAssignmentAdapter.ViewHolder holder,
            int position) {

        AssignmentItem item = items.get(position);

        // ===== Existing bindings (UNCHANGED) =====
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubject.setText("Subject: " + item.getSubject());
        holder.tvDescription.setText(item.getDescription());

        String dateStr = DateFormat.format(
                "dd MMM yyyy, hh:mm a",
                new Date(item.getDueDate())
        ).toString();
        holder.tvDueDate.setText("Due: " + dateStr);

        long now = System.currentTimeMillis();

        if (now > item.getDueDate()) {
            holder.tvStatusChip.setText("Closed");
            holder.tvStatusChip.setBackgroundColor(Color.parseColor("#B00020"));
        } else if (submittedAssignmentIds.contains(item.getId())) {
            holder.tvStatusChip.setText("Submitted");
            holder.tvStatusChip.setBackgroundColor(Color.parseColor("#388E3C"));
        } else {
            holder.tvStatusChip.setText("Not Submitted");
            holder.tvStatusChip.setBackgroundColor(Color.parseColor("#616161"));
        }

        // ===== View Question =====
        String qUrl = item.getQuestionFileUrl();
        if (!TextUtils.isEmpty(qUrl)) {
            holder.btnViewQuestion.setEnabled(true);
            holder.btnViewQuestion.setText("View Question");
            holder.btnViewQuestion.setOnClickListener(v -> openPdfInBrowser(qUrl));
        } else {
            holder.btnViewQuestion.setEnabled(false);
            holder.btnViewQuestion.setText("No Question");
            holder.btnViewQuestion.setOnClickListener(null);
        }

        // ===== Grade =====
        GradeInfo gi = gradeMap.get(item.getId());
        if (gi != null && gi.marks != null) {
            holder.tvGrade.setVisibility(View.VISIBLE);
            holder.tvGrade.setText("Marks: " + gi.marks);

            if (gi.feedback != null && !gi.feedback.isEmpty()) {
                holder.tvFeedback.setVisibility(View.VISIBLE);
                holder.tvFeedback.setText("Feedback: " + gi.feedback);
            } else {
                holder.tvFeedback.setVisibility(View.GONE);
            }
        } else {
            holder.tvGrade.setVisibility(View.GONE);
            holder.tvFeedback.setVisibility(View.GONE);
        }

        // ===== Submit / Resubmit (ONLY NEW LOGIC ADDED) =====
        boolean alreadySubmitted = submittedAssignmentIds.contains(item.getId());
        boolean isClosed = now > item.getDueDate();

        if (isClosed && alreadySubmitted) {
            // 🔒 ONLY resubmission blocked after due date
            holder.btnSubmitPdf.setEnabled(false);
            holder.btnSubmitPdf.setText("Resubmission Closed");
            holder.btnSubmitPdf.setAlpha(0.6f);

            holder.btnSubmitPdf.setOnClickListener(v ->
                    Toast.makeText(
                            context,
                            "Resubmission is closed after due date.",
                            Toast.LENGTH_SHORT
                    ).show()
            );

        } else {
            // ✅ OLD LOGIC (UNCHANGED)
            holder.btnSubmitPdf.setEnabled(true);
            holder.btnSubmitPdf.setAlpha(1f);

            if (alreadySubmitted) {
                holder.btnSubmitPdf.setText("Resubmit PDF");
            } else {
                holder.btnSubmitPdf.setText("Submit PDF");
            }

            holder.btnSubmitPdf.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubmitPdfClicked(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    private void openPdfInBrowser(String pdfUrl) {
        try {
            String encoded = URLEncoder.encode(pdfUrl, "UTF-8");
            String viewerUrl =
                    "https://docs.google.com/gview?embedded=1&url=" + encoded;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewerUrl));
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(context, "Invalid URL encoding", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open PDF", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvStatusChip;
        TextView tvSubject;
        TextView tvDueDate;
        TextView tvDescription;
        MaterialButton btnViewQuestion;
        MaterialButton btnSubmitPdf;
        TextView tvGrade;
        TextView tvFeedback;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatusChip = itemView.findViewById(R.id.tv_status_chip);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
            btnViewQuestion = itemView.findViewById(R.id.btn_view_question);
            btnSubmitPdf = itemView.findViewById(R.id.btn_submit_pdf);
            tvGrade = itemView.findViewById(R.id.tv_grade);
            tvFeedback = itemView.findViewById(R.id.tv_feedback);
        }
    }
}
