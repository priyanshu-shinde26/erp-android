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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.google.android.material.button.MaterialButton;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    // 🔥 FIXED: Matches Activity perfectly
    public interface Listener {
        void onSubmitClicked(AssignmentItem item);
    }

    private final Context context;
    private List<AssignmentItem> items = new ArrayList<>();
    private final Listener listener;
    private final Set<String> submittedAssignmentIds = new HashSet<>(); // 🔥 PERSISTENT STATE

    public StudentAssignmentAdapter(Context context, List<AssignmentItem> items, Listener listener) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    // 🔥 PERSISTENT SUBMISSION TRACKING - METHOD 1
    public void setSubmittedAssignments(Set<String> ids) {
        submittedAssignmentIds.clear();
        if (ids != null) {
            submittedAssignmentIds.addAll(ids);
        }
        notifyDataSetChanged(); // Refresh ALL items with new state
    }

    // 🔥 MARK SINGLE SUBMISSION - METHOD 2 (called after upload)
    public void markSubmitted(String assignmentId) {
        if (assignmentId != null && !assignmentId.trim().isEmpty()) {
            submittedAssignmentIds.add(assignmentId);
            notifyDataSetChanged(); // Refresh to show "Submitted"
        }
    }

    public void setItems(List<AssignmentItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssignmentItem item = items.get(position);

        // 🔥 BASIC INFO
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubject.setText("Subject: " + item.getSubject());
        String dateStr = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(item.getDueDate())).toString();
        holder.tvDueDate.setText("Due: " + dateStr);
        holder.tvDescription.setText(item.getDescription());

        // 🔥 STATUS LOGIC - PERSISTENT STATE ✅
        long now = System.currentTimeMillis();
        boolean isClosed = now > item.getDueDate();
        boolean alreadySubmitted = submittedAssignmentIds.contains(item.getId()); // 🔥 USES PERSISTENT STATE

        // Status Chip
        if (isClosed) {
            holder.tvStatusChip.setText("🔒 Closed");
            holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_chip_error);
        } else if (alreadySubmitted) {
            holder.tvStatusChip.setText("✅ Submitted");
            holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_chip_success);
        } else {
            holder.tvStatusChip.setText("⏳ Not Submitted");
            holder.tvStatusChip.setBackgroundResource(R.drawable.bg_status_chip_neutral);
        }

        // 🔥 GRADES
        if (item.getMarks() != null) {
            holder.layoutGradeBlock.setVisibility(View.VISIBLE);
            holder.tvGrade.setText("⭐ " + item.getMarks() + "/20");
            String feedback = item.getFeedback();
            holder.tvFeedback.setText(!TextUtils.isEmpty(feedback) ? "💬 " + feedback : "Good work!");
        } else {
            holder.layoutGradeBlock.setVisibility(View.GONE);
        }

        // 🔥 VIEW QUESTION
        String qUrl = item.getQuestionFileUrl();
        if (!TextUtils.isEmpty(qUrl)) {
            holder.btnViewQuestion.setEnabled(true);
            holder.btnViewQuestion.setText("📄 View Question");
            holder.btnViewQuestion.setOnClickListener(v -> openPdfInBrowser(qUrl));
        } else {
            holder.btnViewQuestion.setEnabled(false);
            holder.btnViewQuestion.setText("No Question");
        }

        // 🔥 SUBMIT BUTTON - PERFECT LOGIC ✅
        if (isClosed && alreadySubmitted) {
            holder.btnSubmitPdf.setEnabled(false);
            holder.btnSubmitPdf.setText("Resubmission Closed");
            holder.btnSubmitPdf.setAlpha(0.6f);
        } else {
            holder.btnSubmitPdf.setEnabled(true);
            holder.btnSubmitPdf.setAlpha(1f);
            holder.btnSubmitPdf.setText(alreadySubmitted ? "🔄 Resubmit PDF" : "📤 Submit PDF");
            holder.btnSubmitPdf.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubmitClicked(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void openPdfInBrowser(String pdfUrl) {
        try {
            String encoded = URLEncoder.encode(pdfUrl, "UTF-8");
            String viewerUrl = "https://docs.google.com/gview?embedded=true&url=" + encoded;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewerUrl));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open PDF", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatusChip, tvSubject, tvDueDate, tvDescription;
        TextView tvGrade, tvFeedback;
        LinearLayout layoutGradeBlock;
        MaterialButton btnViewQuestion, btnSubmitPdf;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatusChip = itemView.findViewById(R.id.tv_status_chip);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvGrade = itemView.findViewById(R.id.tv_grade);
            tvFeedback = itemView.findViewById(R.id.tv_feedback);
            layoutGradeBlock = itemView.findViewById(R.id.layout_grade_block);
            btnViewQuestion = itemView.findViewById(R.id.btn_view_question);
            btnSubmitPdf = itemView.findViewById(R.id.btn_submit_pdf);
        }
    }
}
