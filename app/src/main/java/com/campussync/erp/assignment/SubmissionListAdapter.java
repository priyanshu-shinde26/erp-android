package com.campussync.erp.assignment;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmissionListAdapter extends RecyclerView.Adapter<SubmissionListAdapter.ViewHolder> {

    public interface OnSubmissionActionListener {
        void onViewFile(String fileUrl);
        void onGradeSubmission(AssignmentSubmissionItem submission);
    }

    public interface StudentInfoProvider {
        String getStudentDisplay(String studentUid);
    }

    private final List<AssignmentSubmissionItem> submissions = new ArrayList<>();
    private OnSubmissionActionListener listener;
    private StudentInfoProvider studentInfoProvider;

    public void setOnSubmissionActionListener(OnSubmissionActionListener listener) {
        this.listener = listener;
    }

    public void setStudentInfoProvider(StudentInfoProvider provider) {
        this.studentInfoProvider = provider;
        notifyDataSetChanged();
    }

    public void setData(List<AssignmentSubmissionItem> list) {
        submissions.clear();
        if (list != null) submissions.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_submission_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssignmentSubmissionItem item = submissions.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvStudentUid;
        TextView tvSubmittedAt;
        TextView tvMarksFeedback;
        TextView tvFeedback;
        Chip chipNotGraded;
        Chip chipGraded;
        MaterialButton btnOpenPdf;
        MaterialButton btnGrade;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentUid = itemView.findViewById(R.id.tv_student_uid);
            tvSubmittedAt = itemView.findViewById(R.id.tv_submitted_at);
            tvMarksFeedback = itemView.findViewById(R.id.tv_marks_feedback);
            tvFeedback = itemView.findViewById(R.id.tv_feedback);
            chipNotGraded = itemView.findViewById(R.id.chip_not_graded);
            chipGraded = itemView.findViewById(R.id.chip_graded);
            btnOpenPdf = itemView.findViewById(R.id.btn_open_pdf);
            btnGrade = itemView.findViewById(R.id.btn_grade);
        }

        void bind(AssignmentSubmissionItem s) {
            if (s == null) return;

            // 🔥 STUDENT DISPLAY - Get name + roll from provider
            String display = "Unknown student";
            if (s.getStudentUid() != null) {
                if (studentInfoProvider != null) {
                    String fromProvider = studentInfoProvider.getStudentDisplay(s.getStudentUid());
                    if (fromProvider != null && !fromProvider.trim().isEmpty()) {
                        display = fromProvider;
                    }
                }
            }
            if (tvStudentUid != null) {
                tvStudentUid.setText("👤 " + display);
            }

            // 🔥 SUBMITTED TIME - Format timestamp
            if (tvSubmittedAt != null) {
                String dateStr = "N/A";
                if (s.getSubmittedAt() != null) {
                    try {
                        long timestamp = Long.parseLong(s.getSubmittedAt());
                        Date date = new Date(timestamp);
                        dateStr = DateFormat.format("dd MMM yyyy, hh:mm a", date).toString();
                    } catch (Exception e) {
                        dateStr = s.getSubmittedAt();
                    }
                }
                tvSubmittedAt.setText("📅 " + dateStr);
            }

            // 🔥 FIXED GRADING STATUS - CHECK BOTH marksObtained + status
            boolean isGraded = (s.getMarksObtained() != null && s.getMarksObtained() >= 0)
                    || "GRADED".equalsIgnoreCase(s.getStatus());

            if (isGraded) {
                // ✅ GRADED
                if (chipNotGraded != null) chipNotGraded.setVisibility(View.GONE);

                if (chipGraded != null) {
                    Integer marks = s.getMarksObtained();
                    chipGraded.setVisibility(View.VISIBLE);
                    chipGraded.setText("⭐ " + (marks != null ? marks : "Graded") + "/20");
                }

                if (tvMarksFeedback != null) {
                    tvMarksFeedback.setText("✅ Graded");
                }

                if (tvFeedback != null) {
                    String feedback = s.getFeedback();
                    if (feedback != null && !feedback.trim().isEmpty()) {
                        tvFeedback.setText("💬 " + feedback);
                        tvFeedback.setVisibility(View.VISIBLE);
                    } else {
                        tvFeedback.setText("💬 No feedback");
                        tvFeedback.setVisibility(View.VISIBLE);
                    }
                }

                if (btnGrade != null) {
                    btnGrade.setText("✏️ Edit Grade");
                }
            } else {
                // ⏳ NOT GRADED YET
                if (chipNotGraded != null) {
                    chipNotGraded.setVisibility(View.VISIBLE);
                    chipNotGraded.setText("⏳ Pending");
                }

                if (chipGraded != null) chipGraded.setVisibility(View.GONE);

                if (tvMarksFeedback != null) {
                    tvMarksFeedback.setText("⏳ Not graded yet");
                }

                if (tvFeedback != null) {
                    tvFeedback.setVisibility(View.GONE);
                }

                if (btnGrade != null) {
                    btnGrade.setText("✏️ Grade Now");
                }
            }

            // 🔥 VIEW PDF BUTTON - Shows submitted file
            if (btnOpenPdf != null) {
                btnOpenPdf.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewFile(s.getFileUrl());
                    }
                });
            }

            // 🔥 GRADE BUTTON - Opens dialog for THIS submission
            if (btnGrade != null) {
                btnGrade.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onGradeSubmission(s); // 🔥 PASS THIS SPECIFIC SUBMISSION
                    }
                });
            }
        }
    }
}
