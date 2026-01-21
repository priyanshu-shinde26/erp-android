package com.campussync.erp.lms;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapterTeacher extends RecyclerView.Adapter<NotesAdapterTeacher.NoteViewHolder> {
    private List<Note> notes = new ArrayList<>();
    private Context context;
    private OnNoteActionListener listener;

    public interface OnNoteActionListener {
        void onDeleteNote(String noteId);
    }

    public NotesAdapterTeacher(Context context, OnNoteActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note_teacher, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.title);
        holder.tvSubject.setText(note.subject);
        holder.tvDate.setText(note.filename);
        holder.btnViewNote.setOnClickListener(v -> downloadNote(context, note.url,note.filename));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteNote(note.id));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    private void openNoteUrl(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "No file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), getMimeType(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Open file"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app to open this file", Toast.LENGTH_LONG).show();
        }
    }
    private String getMimeType(String url) {
        if (url.toLowerCase().endsWith(".pdf")) return "application/pdf";
        if (url.toLowerCase().endsWith(".pptx") || url.toLowerCase().endsWith(".ppt")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (url.toLowerCase().endsWith(".docx") || url.toLowerCase().endsWith(".doc")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "text/plain"; // fallback
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvDate;
        Button btnViewNote, btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnViewNote = itemView.findViewById(R.id.btnViewNote);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
   /*private void openInChrome(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "No file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Simple: append download parameters
        String downloadUrl = url + "?fl_attachment&dl=1";

        Intent chromeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
        chromeIntent.setPackage("com.android.chrome");
        chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (chromeIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chromeIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            context.startActivity(Intent.createChooser(browserIntent, "Open file"));
        }
    }

    */
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

    static String getMimeFromFilename(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".pdf")) return "application/pdf";
        if (f.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (f.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (f.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (f.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
    }

}
