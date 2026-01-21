package com.campussync.erp.lms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeacherLmsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotesAdapterTeacher adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnUpload;
    private Spinner spinnerClass;

    private String currentClassId, uid, role;
    private DatabaseReference dbRef;

    // ✅ Single source for spinner
    private final List<String> classIds = new ArrayList<>();
    private ArrayAdapter<String> classAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_lms);

        try {
            initViews();
            getUserSession();
            setupClassSpinner();
            setupRecyclerView();
            setupListeners();
            loadClassesFromFirebase();
        } catch (Exception e) {
            Log.e("TeacherLms", "Setup failed", e);
            toast("Setup failed: " + e.getMessage());
            finish();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnUpload = findViewById(R.id.btnUpload);
        spinnerClass = findViewById(R.id.spinnerClass);
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    private void getUserSession() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        uid = prefs.getString("uid", "");
        role = prefs.getString("role", "");
    }

    private void setupClassSpinner() {
        classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        classAdapter.add("Loading classes...");
        spinnerClass.setAdapter(classAdapter);
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapterTeacher(this, noteId -> deleteNote(noteId));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadClassesFromFirebase() {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> unique = new HashSet<>();

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String classId = studentSnap.child("classId").getValue(String.class);
                    if (classId != null && !classId.trim().isEmpty()) {
                        unique.add(classId.trim());
                    }
                }

                classIds.clear();
                classIds.addAll(unique);
                Collections.sort(classIds);

                classAdapter.clear();
                classAdapter.add("Select Class");

                if (classIds.isEmpty()) {
                    classAdapter.add("(No classes found)");
                    currentClassId = null;
                    toast("⚠️ No classId found in students");
                } else {
                    classAdapter.addAll(classIds);
                }

                classAdapter.notifyDataSetChanged();
                spinnerClass.setSelection(0);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                toast("❌ Firebase error: " + error.getMessage());
            }
        });
    }

    private void setupListeners() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) {
                    currentClassId = null;
                    adapter.updateNotes(new ArrayList<>());
                    toggleEmptyState(true);
                    return;
                }

                int index = position - 1;
                if (index < 0 || index >= classIds.size()) {
                    currentClassId = null;
                    return;
                }

                currentClassId = classIds.get(index);
                loadNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentClassId = null;
            }
        });

        btnUpload.setOnClickListener(v -> {
            if (currentClassId == null || currentClassId.trim().isEmpty()) {
                toast("👆 Select class first");
                spinnerClass.performClick();
                return;
            }

            Intent intent = new Intent(TeacherLmsActivity.this, UploadNoteActivity.class);
            intent.putExtra("classId", currentClassId);
            intent.putExtra("uid", uid);
            startActivity(intent);
        });
    }

    private void loadNotes() {
        if (currentClassId == null || currentClassId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        DatabaseReference notesRef = dbRef.child("lms").child("notes").child(currentClassId);
        notesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Note> notesList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Note note = child.getValue(Note.class);
                        if (note != null) {
                            note.id = child.getKey();
                            notesList.add(note);
                        }
                    } catch (Exception e) {
                        // ✅ Skip bad notes (HashMap→String error)
                        Log.w("TeacherLms", "Skipping bad note: " + child.getKey(), e);
                    }
                }
                adapter.updateNotes(notesList);
                toggleEmptyState(notesList.isEmpty());
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                toast("Error loading notes: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (isEmpty) {
            tvEmpty.setText("No notes for selected class");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void deleteNote(String noteId) {
        if (currentClassId != null) {
            dbRef.child("lms").child("notes").child(currentClassId).child(noteId).removeValue()
                    .addOnSuccessListener(aVoid -> toast("Note deleted"))
                    .addOnFailureListener(e -> toast("Delete failed: " + e.getMessage()));
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
