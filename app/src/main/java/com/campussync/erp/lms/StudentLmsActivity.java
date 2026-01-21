package com.campussync.erp.lms;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentLmsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotesAdapterStudent adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnRequestNote;
    private LinearLayout emptyStateLayout;
    private DatabaseReference dbRef;
    private String myClassId;
    private ValueEventListener notesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_lms);
        initViews();
        loadMyClassAndNotes();  // 🔥 YOUR EXACT METHOD
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        emptyStateLayout = findViewById(R.id.emptyState);
        btnRequestNote = findViewById(R.id.btnRequest);

        btnRequestNote.setOnClickListener(v ->
                Toast.makeText(this, "📝 Request coming soon!", Toast.LENGTH_SHORT).show()
        );

        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    private void loadMyClassAndNotes() {
        showLoading(true);

        // 🔥 YOUR EXACT CODE - students node
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("students").child(FirebaseAuth.getInstance().getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                myClassId = snapshot.child("classId").getValue(String.class);
                showLoading(false);

                if (myClassId == null || myClassId.trim().isEmpty()) {
                    tvEmpty.setText("⚠️ No class assigned. Contact admin.");
                    showEmptyState();
                    return;
                }

                // ✅ CLASS FOUND - load notes
                setTitle("📚 Notes - " + myClassId);
                setupRecyclerView();
                loadClassNotes(myClassId);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showLoading(false);
                Toast.makeText(StudentLmsActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapterStudent(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadClassNotes(String classId) {
        DatabaseReference notesRef = dbRef.child("lms/notes").child(classId);

        notesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Note> notes = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Note note = child.getValue(Note.class);
                        if (note != null && note.url != null) {
                            note.id = child.getKey();
                            notes.add(note);
                        }
                    } catch (Exception ignored) {}
                }

                adapter.updateNotes(notes);
                toggleEmptyState(notes.isEmpty());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentLmsActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        };

        notesRef.addValueEventListener(notesListener);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (isEmpty) {
            tvEmpty.setText("📝 No notes for " + myClassId);
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        btnRequestNote.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        btnRequestNote.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null && myClassId != null) {
            dbRef.child("lms/notes").child(myClassId).removeEventListener(notesListener);
        }
    }
}
