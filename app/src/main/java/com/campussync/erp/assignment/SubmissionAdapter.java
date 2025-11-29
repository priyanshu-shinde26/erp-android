package com.campussync.erp.assignment;

import android.content.Context;
import android.content.Intent;
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
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.button.MaterialButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

    public interface Listener {
        void onGradeClicked(AssignmentSubmissionItem submission);
    }

    private final Context context;
    private List<AssignmentSubmissionItem> items = new ArrayList<>();
    private final Listener listener;

    public SubmissionAdapter(Context context,
                             List<AssignmentSubmissionItem> items,
                             Listener listener) {
        this.context = context;
        if (items != null) {
            this.items = items;
        }
        this.listener = listener;
    }

    public void setItems(List<AssignmentSubmissionItem> newItems) {
        if (newItems == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = newItems;
        }
        notifyDataSetChanged();
    }

    public void updateSubmissionLocal(String submissionId, Integer marks, String feedback) {
        if (submissionId == null) return;
        for (AssignmentSubmissionItem item : items) {
            if (submissionId.equals(item.getId())) {
                item.setMarks(marks);
                item.setFeedback(feedback);
                notifyDataSetChanged();
                break;
            }
        }
    }

    @NonNull
    @Override
    public SubmissionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_submission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubmissionAdapter.ViewHolder holder, int position) {
        AssignmentSubmissionItem item = items.get(position);

        holder.tvStudentUid.setText("Student UID: " + item.getStudentUid());

        String dateStr = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(item.getSubmittedAt())).toString();
        holder.tvSubmittedAt.setText("Submitted: " + dateStr);

        if (item.getMarks() != null) {
            String msg = "Marks: " + item.getMarks();
            if (!TextUtils.isEmpty(item.getFeedback())) {
                msg += ", Feedback: " + item.getFeedback();
            }
            holder.tvMarksFeedback.setText(msg);
        } else {
            holder.tvMarksFeedback.setText("Not graded yet");
        }

        holder.btnGrade.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGradeClicked(item);
            }
        });

        // Open PDF via Google Docs viewer
        holder.btnOpenPdf.setOnClickListener(v -> {
            String url = item.getFileUrl();
            if (!TextUtils.isEmpty(url)) {
                openPdfInBrowser(url);
            } else {
                Toast.makeText(context, "No file URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    private void openPdfInBrowser(String pdfUrl) {
        try {
            String encoded = URLEncoder.encode(pdfUrl, "UTF-8");
            String viewerUrl = "https://docs.google.com/gview?embedded=1&url=" + encoded;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(viewerUrl));
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(context, "Invalid URL encoding", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open PDF", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentUid;
        TextView tvSubmittedAt;
        TextView tvMarksFeedback;
        MaterialButton btnOpenPdf;
        MaterialButton btnGrade;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentUid = itemView.findViewById(R.id.tv_student_uid);
            tvSubmittedAt = itemView.findViewById(R.id.tv_submitted_at);
            tvMarksFeedback = itemView.findViewById(R.id.tv_marks_feedback);
            btnOpenPdf = itemView.findViewById(R.id.btn_open_pdf);
            btnGrade = itemView.findViewById(R.id.btn_grade);
        }
    }
}
