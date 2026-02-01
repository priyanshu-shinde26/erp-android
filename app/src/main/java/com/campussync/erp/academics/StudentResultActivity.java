package com.campussync.erp.academics;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.github.mikephil.charting.components.XAxis;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class StudentResultActivity extends AppCompatActivity {

    private RecyclerView rvResults;
    private StudentMarksAdapter adapter;
    private BarChart barChart;
    private AcademicDBHelper dbHelper;

    private String studentId;
    private String classId;

    // ✅ FINAL STATE MANAGEMENT
    private final List<ResultEntity> freshResults = new ArrayList<>();
    private int pendingFetch = 0;

    // Track listener to prevent memory leaks/DeadObject
    private ValueEventListener firebaseListener;
    private DatabaseReference marksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_result);

        rvResults = findViewById(R.id.rvResults);
        barChart = findViewById(R.id.barChart);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentMarksAdapter();
        rvResults.setAdapter(adapter);

        dbHelper = new AcademicDBHelper(this);

        studentId = FirebaseAuth.getInstance().getUid();
        classId = AcademicUtils.getStudentClass(this);
        FloatingActionButton fab = findViewById(R.id.fabShowGraph);
        View chartCard = findViewById(R.id.chartContainer);

        fab.setOnClickListener(v -> {
            if (chartCard.getVisibility() == View.VISIBLE) {
                chartCard.setVisibility(View.GONE);
                fab.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            } else {
                chartCard.setVisibility(View.VISIBLE);
                fab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                // Refresh chart data if needed
                barChart.animateY(1000);
            }
        });
        // ✅ STEP 2: SAFE CONTEXT CHECK AND LOAD ORDER
        if (studentId == null || classId == null) {
            Log.e("STUDENT_RESULT", "Missing context, closing");
            finish();
            return;
        }

        // ✅ Load quick local data first, then trigger async network call
        loadCachedResults();
        loadResultsFromFirebase();
    }

    // ✅ STEP 3: UI-SAFE LOCAL READ
    private void loadCachedResults() {
        List<ResultEntity> cached = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT subject, test, marks, maxMarks FROM student_results",
                null
        );

        while (c.moveToNext()) {
            ResultEntity e = new ResultEntity();
            e.subject = c.getString(0);
            e.test = c.getString(1);
            e.marks = c.getInt(2);
            e.maxMarks = c.getInt(3);
            cached.add(e);
        }
        c.close();

        if (!cached.isEmpty()) {
            adapter.update(cached);
            renderChart(cached);
        }
    }

    // ✅ STEP 4: CLEAN FIREBASE LOADER
    private void loadResultsFromFirebase() {
        marksRef = FirebaseDatabase.getInstance()
                .getReference("marks")
                .child(classId);

        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot classSnap) {
                freshResults.clear();
                pendingFetch = 0;

                for (DataSnapshot subjectSnap : classSnap.getChildren()) {
                    String subjectId = subjectSnap.getKey();

                    for (DataSnapshot testSnap : subjectSnap.getChildren()) {
                        String testId = testSnap.getKey();

                        if (!testSnap.hasChild(studentId)) continue;

                        Integer marks = testSnap
                                .child(studentId)
                                .child("marks")
                                .getValue(Integer.class);

                        if (marks == null) continue;

                        pendingFetch++;
                        fetchTest(subjectId, testId, marks);
                    }
                }

                if (pendingFetch == 0) {
                    Log.d("STUDENT_RESULT", "No marks found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("STUDENT_RESULT", "Marks load failed", error.toException());
            }
        };
        marksRef.addListenerForSingleValueEvent(firebaseListener);
    }

    private void fetchTest(String subjectId, String testId, int marks) {
        DatabaseReference testRef = FirebaseDatabase.getInstance()
                .getReference("tests")
                .child(classId)
                .child(subjectId)
                .child(testId);

        testRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                if (snap.exists()) {
                    ResultEntity e = new ResultEntity();
                    e.subject = subjectId;
                    e.test = snap.child("testName").getValue(String.class);
                    e.marks = marks;
                    Integer max = snap.child("maxMarks").getValue(Integer.class);
                    e.maxMarks = max != null ? max : 100;

                    freshResults.add(e);
                }

                pendingFetch--;
                if (pendingFetch == 0) {
                    saveAndShow();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                pendingFetch--;
                if (pendingFetch == 0) saveAndShow();
            }
        });
    }

    // ✅ STEP 5: ATOMIC UI UPDATE AND CACHE
    private void saveAndShow() {
        if (isFinishing() || isDestroyed()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM student_results");
            for (ResultEntity e : freshResults) {
                db.execSQL(
                        "INSERT INTO student_results(subject, test, marks, maxMarks) VALUES(?,?,?,?)",
                        new Object[]{e.subject, e.test, e.marks, e.maxMarks}
                );
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("STUDENT_RESULT", "Save failed", e);
        } finally {
            db.endTransaction();
        }

        // Final UI update
        adapter.update(new ArrayList<>(freshResults));
        renderChart(freshResults);
    }

    private void renderChart(List<ResultEntity> list) {
        if (isFinishing() || isDestroyed() || list.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            entries.add(new BarEntry(i, list.get(i).marks));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Your Scores");

        // ✅ STYLE: Professional Purple with rounded corners
        dataSet.setColor(Color.parseColor("#6C5CE7"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#2D3436"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f); // Make bars slightly thinner for a cleaner look

        barChart.setData(data);

        // ✅ CLEANER AXIS: Remove grid lines and borders
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);

        barChart.getAxisRight().setEnabled(false); // Hide right Y-axis (redundant)
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.getLegend().setEnabled(false); // Hide legend if only one data type
        barChart.getDescription().setEnabled(false); // Remove "Description Label"

        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);

        // ✅ ANIMATION: Smooth entry
        barChart.animateY(1000);
        barChart.invalidate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Clean up listener to prevent potential crashes on return
        if (marksRef != null && firebaseListener != null) {
            marksRef.removeEventListener(firebaseListener);
        }
    }
}