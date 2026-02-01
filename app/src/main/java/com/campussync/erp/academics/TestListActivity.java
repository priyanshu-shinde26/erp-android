package com.campussync.erp.academics;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Added for FAB
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestListActivity extends AppCompatActivity {

    private static final String TAG = "TEST_LIST";

    private String classId, subjectId;
    private TestListAdapter adapter;
    private FloatingActionButton btnCreate; // Changed to FloatingActionButton
    private LinearLayout llNoTest;           // Added for Empty State logic
    private RecyclerView rv;                 // Moved to class level for visibility toggling

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_test_list);

        // 1. Get Context from Intent
        classId = getIntent().getStringExtra("classId");
        subjectId = getIntent().getStringExtra("subjectId");

        if (classId == null || subjectId == null) {
            Toast.makeText(this, "Invalid class or subject", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "class=" + classId + ", subject=" + subjectId);

        // 2. Initialize UI Components
        rv = findViewById(R.id.rvTests);
        btnCreate = findViewById(R.id.btnCreateTest); // Now references the FAB
        llNoTest = findViewById(R.id.llNoTest);       // References the new Empty State container

        // 3. Setup RecyclerView & Adapter
        String role = AcademicUtils.getRoleFromPrefs(this);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TestListAdapter(
                role,
                this::openMarksEntryWithTest,
                this::confirmDelete
        );
        rv.setAdapter(adapter);

        // 4. Role-Based UI visibility
        if ("STUDENT".equals(role)) {
            btnCreate.setVisibility(View.GONE);
        } else {
            btnCreate.setVisibility(View.VISIBLE);
            btnCreate.setOnClickListener(v -> openCreateTest());
        }

        // 5. Load Data
        loadTestsFromFirebase();
    }

    // =========================================================
    // LOAD TESTS DIRECTLY FROM FIREBASE
    // =========================================================
    private void loadTestsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("tests")
                .child(classId)
                .child(subjectId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<TestModel> tests = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    TestModel t = ds.getValue(TestModel.class);
                    if (t != null) {
                        t.testId = ds.getKey();
                        tests.add(t);
                    }
                }

                if (tests.isEmpty()) {
                    // ✅ Updated: Show Empty State UI
                    llNoTest.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);

                    if (!"STUDENT".equals(AcademicUtils.getRoleFromPrefs(TestListActivity.this))) {
                        Toast.makeText(TestListActivity.this,
                                "No tests found. Redirecting to attendance-only mode.",
                                Toast.LENGTH_SHORT).show();
                        openMarksEntryWithoutTest();
                    }
                    return;
                }

                // ✅ Updated: Show RecyclerView UI
                llNoTest.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);

                Log.d(TAG, "Loaded " + tests.size() + " tests");
                adapter.update(tests);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error", error.toException());
                Toast.makeText(TestListActivity.this, "Failed to load tests", Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================================================
    // DELETE LOGIC
    // =========================================================
    private void confirmDelete(TestModel test) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Test")
                .setMessage("Delete \"" + test.testName + "\"? This will also delete all marks for this test. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteTest(test))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTest(TestModel test) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();

        updates.put("tests/" + classId + "/" + subjectId + "/" + test.testId, null);
        updates.put("marks/" + classId + "/" + subjectId + "/" + test.testId, null);

        root.updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Test and associated marks deleted", Toast.LENGTH_SHORT).show();
                    loadTestsFromFirebase(); // Refresh the list automatically
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    private void openCreateTest() {
        Intent i = new Intent(this, CreateTestActivity.class);
        i.putExtra("classId", classId);
        i.putExtra("subjectId", subjectId);
        startActivity(i);
    }

    private void openMarksEntryWithTest(TestModel test) {
        Intent i = new Intent(this, MarksEntryActivity.class);
        i.putExtra("classId", classId);
        i.putExtra("subjectId", subjectId);
        i.putExtra("testId", test.testId);
        startActivity(i);
    }

    private void openMarksEntryWithoutTest() {
        Intent i = new Intent(this, MarksEntryActivity.class);
        i.putExtra("classId", classId);
        i.putExtra("subjectId", subjectId);
        i.putExtra("testId", "NO_TEST");
        startActivity(i);
        finish();
    }
}