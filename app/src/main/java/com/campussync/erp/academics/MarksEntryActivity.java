package com.campussync.erp.academics;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarksEntryActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;

    private final List<AcademicStudentModel> fullList = new ArrayList<>();

    private RecyclerView recyclerView;
    private MarksEntryAdapter adapter;
    private Button btnSubmit;

    private String classId;
    private String subjectId;
    private String testId;
    private boolean noTest;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_marks_entry);

        classId = getIntent().getStringExtra("classId");
        subjectId = getIntent().getStringExtra("subjectId");
        testId = getIntent().getStringExtra("testId");

        noTest = "NO_TEST".equals(testId);

        String role = AcademicUtils.getRoleFromPrefs(this);

        if ("STUDENT".equals(role)) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (classId == null || subjectId == null || testId == null) {
            Toast.makeText(this, "Invalid context", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rvMarks);
        btnSubmit = findViewById(R.id.btnSubmitMarks);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarksEntryAdapter(noTest);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView rv, int dx, int dy) {
                        if (!rv.canScrollVertically(1)) {
                            currentPage++;
                            loadNextPage();
                        }
                    }
                });

        fetchStudentsFromFirebase();
        btnSubmit.setOnClickListener(v -> submitMarks());
    }

    // ====================================================
    // FETCH STUDENTS + ✅ FIX 2B: LOAD PREVIOUS MARKS
    // ====================================================
    private void fetchStudentsFromFirebase() {
        DatabaseReference studentsRef =
                FirebaseDatabase.getInstance().getReference("students");

        Query query = studentsRef
                .orderByChild("classId")
                .equalTo(classId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                fullList.clear();
                adapter.clear();
                currentPage = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    AcademicStudentModel s = ds.getValue(AcademicStudentModel.class);
                    if (s == null) continue;

                    s.studentId = ds.getKey();
                    if (noTest) s.enteredMarks = -1;

                    fullList.add(s);
                }

                // 🔥 AFTER loading students, fetch existing marks
                loadExistingMarks();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MarksEntryActivity.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExistingMarks() {
        DatabaseReference marksRef = FirebaseDatabase.getInstance()
                .getReference("marks")
                .child(classId)
                .child(subjectId)
                .child(testId);

        marksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (AcademicStudentModel s : fullList) {
                    if (snapshot.hasChild(s.studentId)) {
                        // Firebase returns Long by default for numbers
                        Long m = snapshot.child(s.studentId).child("marks").getValue(Long.class);
                        if (m != null) {
                            s.enteredMarks = m.intValue();
                        }
                    }
                }
                loadNextPage(); // Trigger initial UI load after marks are synced
            }

            @Override
            public void onCancelled(DatabaseError error) {
                loadNextPage(); // Still load names even if marks fetch fails
            }
        });
    }

    // ====================================================
    // PAGINATION
    // ====================================================
    private void loadNextPage() {
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, fullList.size());

        if (from >= to) return;

        List<AcademicStudentModel> page = fullList.subList(from, to);
        adapter.addItems(page);
        adapter.notifyDataSetChanged();
    }

    // ====================================================
    // ✅ CORRECT: SUBMIT MARKS USING updateChildren
    // ====================================================
    private void submitMarks() {
        List<AcademicStudentModel> entered = adapter.getCurrentList();

        if (entered.isEmpty()) {
            Toast.makeText(this, "No students loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("marks")
                .child(classId)
                .child(subjectId)
                .child(testId);

        Map<String, Object> updates = new HashMap<>();
        long timestamp = System.currentTimeMillis();

        for (AcademicStudentModel s : entered) {
            Map<String, Object> m = new HashMap<>();

            if (noTest) {
                m.put("marks", -1);
                m.put("status", "ABSENT");
            } else {
                m.put("marks", s.enteredMarks);
                m.put("status", s.enteredMarks < 0 ? "ABSENT" : "PRESENT");
            }
            m.put("updatedAt", timestamp);

            updates.put(s.studentId, m);
        }

        // Only update children to prevent wiping existing data for other students
        ref.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Marks updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}