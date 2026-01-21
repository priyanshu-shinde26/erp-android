package com.campussync.erp.lms;

import static com.campussync.erp.lms.NotesAdapterTeacher.getMimeFromFilename;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapterStudent extends RecyclerView.Adapter<NotesAdapterStudent.ViewHolder> {
    private Context context;
    private List<Note> notes = new ArrayList<>();

    public NotesAdapterStudent(Context context) {
        this.context = context;
    }

    public void updateNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.tvTitle.setText(note.title);
        holder.tvSubject.setText(note.subject);

        // ✅ Show teacher name instead of UID
        // ✅ Show teacher name instead of UID
        String uploaderName = (note.uploadedByName != null && !note.uploadedByName.trim().isEmpty())
                ? note.uploadedByName
                : note.uploadedBy;

        holder.tvUploadedBy.setText("👤 " + uploaderName);

        holder.tvUploadedBy.setText("👤 " + uploaderName);

        holder.tvTime.setText(note.getFormattedTime());

        // 🔥 Download button (same as teacher)
        if (note.url != null && !note.url.isEmpty()) {
            holder.btnDownload.setVisibility(View.VISIBLE);
            holder.btnDownload.setText("⬇️ " + note.filename);
            holder.btnDownload.setOnClickListener(v -> downloadNote(context, note.url, note.filename));
        } else {
            holder.btnDownload.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return notes.size();
    }
    public static void downloadNote(Context context, String url, String filename) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "No URL", Toast.LENGTH_SHORT).show();
            return;
        }
        if (filename == null || filename.trim().isEmpty()) filename = "note";

        String mime = getMimeFromFilename(filename);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setTitle(filename);
        req.setMimeType(mime); // ✅ makes it openable after download [web:682]
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename); // ✅ forces correct extension [web:675]
        dm.enqueue(req);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvUploadedBy, tvTime;
        MaterialButton btnDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
}
