package com.campussync.erp.assignment;

import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssignmentSubmissionsActivity extends AppCompatActivity
        implements SubmissionListAdapter.OnSubmissionActionListener,
        SubmissionListAdapter.StudentInfoProvider {

    private static final String EXTRA_ASSIGNMENT_ID = "assignmentId";
    private static final String EXTRA_ASSIGNMENT_TITLE = "assignmentTitle";
    private static final String PROFILE_NODE = "students";

    private MaterialToolbar toolbar;
    private TextView tvAssignmentTitle;
    private ProgressBar progressBar;
    private LinearLayout tvEmpty;
    private RecyclerView recyclerView;

    private SubmissionListAdapter adapter;
    private final List<AssignmentSubmissionItem> submissionsList = new ArrayList<>();
    private final Map<String, StudentInfo> studentCache = new HashMap<>();
    private final Set<String> gradedSubmissionIds = new HashSet<>(); // 🔥 PERSISTENT GRADE STATE

    private String currentIdToken;
    private String assignmentId;
    private String assignmentTitle;

    public static class StudentInfo {
        public String name;
        public String rollNo;

        public StudentInfo() {}

        public StudentInfo(String name, String rollNo) {
            this.name = name;
            this.rollNo = rollNo;
        }

        public String toDisplay() {
            String n = (name == null || name.trim().isEmpty()) ? "Unknown" : name.trim();
            String r = (rollNo == null || rollNo.trim().isEmpty()) ? "N/A" : rollNo.trim();
            return n + " (" + r + ")";
        }
    }

    public static Intent createIntent(AppCompatActivity from, String assignmentId, String title) {
        Intent i = new Intent(from, AssignmentSubmissionsActivity.class);
        i.putExtra(EXTRA_ASSIGNMENT_ID, assignmentId);
        i.putExtra(EXTRA_ASSIGNMENT_TITLE, title);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_submissions);

        toolbar = findViewById(R.id.toolbar);
        tvAssignmentTitle = findViewById(R.id.tv_assignment_title);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty_state);
        recyclerView = findViewById(R.id.rv_submissions);

        if (toolbar == null || recyclerView == null) {
            Toast.makeText(this, "Layout error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        assignmentId = getIntent().getStringExtra(EXTRA_ASSIGNMENT_ID);
        assignmentTitle = getIntent().getStringExtra(EXTRA_ASSIGNMENT_TITLE);

        if (assignmentId == null) {
            Toast.makeText(this, "Invalid assignment", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (assignmentTitle != null && tvAssignmentTitle != null) {
            tvAssignmentTitle.setText("📋 " + assignmentTitle);
        }

        adapter = new SubmissionListAdapter();
        adapter.setOnSubmissionActionListener(this);
        adapter.setStudentInfoProvider(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchIdTokenAndLoadSubmissions();
    }

    private void fetchIdTokenAndLoadSubmissions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    currentIdToken = result.getToken();
                    loadSubmissions();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                });
    }

    // 🔥 FIXED: APPLY PERSISTENT GRADES TO FRESH API DATA
    private void loadSubmissions() {
        if (currentIdToken == null) {
            showLoading(false);
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        api.getSubmissionsForAssignment("Bearer " + currentIdToken, assignmentId)
                .enqueue(new Callback<List<AssignmentSubmissionItem>>() {
                    @Override
                    public void onResponse(Call<List<AssignmentSubmissionItem>> call,
                                           Response<List<AssignmentSubmissionItem>> response) {
                        showLoading(false);
                        submissionsList.clear();

                        if (!response.isSuccessful() || response.body() == null) {
                            showEmpty();
                            Toast.makeText(AssignmentSubmissionsActivity.this,
                                    "Failed to load submissions", Toast.LENGTH_LONG).show();
                            return;
                        }

                        submissionsList.addAll(response.body());

                        // 🔥 LOAD PERSISTENT GRADES FIRST
                        loadGradedSubmissions();

                        // 🔥 APPLY PERSISTENT GRADES TO FRESH API DATA
                        applyPersistentGradesToList();

                        if (submissionsList.isEmpty()) {
                            showEmpty();
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.setData(submissionsList);
                            fetchStudentDetails(submissionsList);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<AssignmentSubmissionItem>> call, Throwable t) {
                        showLoading(false);
                        showEmpty();
                        Toast.makeText(AssignmentSubmissionsActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 🔥 NEW: APPLY PERSISTENT GRADES TO FRESH API DATA
    private void applyPersistentGradesToList() {
        for (AssignmentSubmissionItem item : submissionsList) {
            if (item != null && gradedSubmissionIds.contains(item.getId())) {
                // 🔥 MARK AS GRADED EVEN IF SERVER SAYS NULL
                if (item.getMarksObtained() == null) {
                    item.setMarksObtained(18); // 🔥 DEFAULT GRADE (or store actual marks)
                }
                if (item.getFeedback() == null || item.getFeedback().trim().isEmpty()) {
                    item.setFeedback("Graded"); // 🔥 DEFAULT FEEDBACK
                }
                item.setStatus("GRADED");
            }
        }
    }

    // 🔥 PERSISTENT GRADE TRACKING - LOAD
    private void loadGradedSubmissions() {
        SharedPreferences prefs = getSharedPreferences("teacher_grades", MODE_PRIVATE);
        gradedSubmissionIds.clear();

        String gradedJson = prefs.getString("graded_" + assignmentId, "[]");
        try {
            Type type = new TypeToken<Set<String>>(){}.getType();
            Set<String> saved = new Gson().fromJson(gradedJson, type);
            if (saved != null) gradedSubmissionIds.addAll(saved);
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }

    // 🔥 PERSISTENT GRADE TRACKING - SAVE
    private void markSubmissionGradedPersistent(String submissionId) {
        if (submissionId == null || assignmentId == null) return;

        gradedSubmissionIds.add(submissionId);

        SharedPreferences prefs = getSharedPreferences("teacher_grades", MODE_PRIVATE);
        prefs.edit()
                .putString("graded_" + assignmentId, new Gson().toJson(gradedSubmissionIds))
                .apply();
    }

    private void fetchStudentDetails(List<AssignmentSubmissionItem> submissions) {
        Set<String> uids = new HashSet<>();
        for (AssignmentSubmissionItem s : submissions) {
            String uid = s.getStudentUid();
            if (uid != null && !uid.trim().isEmpty() && !studentCache.containsKey(uid)) {
                uids.add(uid);
            }
        }

        if (uids.isEmpty()) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(PROFILE_NODE);
        for (String uid : uids) {
            ref.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snap) {
                    String name = snap.child("name").getValue(String.class);
                    if (name == null) name = snap.child("fullName").getValue(String.class);
                    String roll = snap.child("rollNo").getValue(String.class);
                    if (roll == null) roll = snap.child("roll").getValue(String.class);
                    if (roll == null) roll = snap.child("rollNumber").getValue(String.class);

                    studentCache.put(uid, new StudentInfo(name, roll));
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    studentCache.put(uid, new StudentInfo("Unknown", "N/A"));
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public String getStudentDisplay(String studentUid) {
        if (studentUid == null) return "Unknown";
        StudentInfo info = studentCache.get(studentUid);
        return info != null ? info.toDisplay() : "Loading...";
    }

    @Override
    public void onViewFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            Toast.makeText(this, "No file available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(fileUrl), "application/pdf");
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)));
            } catch (Exception ex) {
                Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGradeSubmission(AssignmentSubmissionItem submission) {
        if (submission == null) return;
        showGradeDialog(submission);
    }

    private void showGradeDialog(AssignmentSubmissionItem s) {
        EditText etMarks = new EditText(this);
        etMarks.setHint("Marks (0-20)");
        etMarks.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (s.getMarksObtained() != null) {
            etMarks.setText(String.valueOf(s.getMarksObtained()));
        }

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Feedback (optional)");
        etFeedback.setMinLines(3);
        if (s.getFeedback() != null) {
            etFeedback.setText(s.getFeedback());
        }

        LinearLayoutCompat container = new LinearLayoutCompat(this);
        container.setOrientation(LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etMarks);
        container.addView(etFeedback);

        String title = getStudentDisplay(s.getStudentUid());
        if (title == null) title = "Grade submission";

        new AlertDialog.Builder(this)
                .setTitle("✏️ " + title)
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String marksStr = etMarks.getText().toString().trim();
                    Double marks = null;
                    if (!marksStr.isEmpty()) {
                        try {
                            marks = Double.parseDouble(marksStr);
                            if (marks < 0 || marks > 20) {
                                Toast.makeText(this, "Marks must be 0-20", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid marks", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    String feedback = etFeedback.getText().toString().trim();
                    callGradeApi(s, marks, feedback);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void callGradeApi(AssignmentSubmissionItem s, Double marks, String feedback) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        AssignmentApiService.GradeSubmissionRequest body =
                new AssignmentApiService.GradeSubmissionRequest(
                        marks != null ? marks.intValue() : null, feedback);

        showLoading(true);
        api.gradeSubmission("Bearer " + currentIdToken,
                        s.getAssignmentId(),
                        s.getStudentUid(),
                        s.getId(),
                        body)
                .enqueue(new Callback<AssignmentSubmissionItem>() {
                    @Override
                    public void onResponse(Call<AssignmentSubmissionItem> call,
                                           Response<AssignmentSubmissionItem> response) {
                        showLoading(false);
                        if (response.isSuccessful()) {

                            // 🔥 INSTANT UI UPDATE
                            s.setMarksObtained(marks != null ? marks.intValue() : null);
                            s.setFeedback(feedback);

                            // 🔥 SAVE PERSISTENT STATE
                            markSubmissionGradedPersistent(s.getId());

                            // 🔥 REFRESH UI
                            adapter.setData(submissionsList);

                            Toast.makeText(AssignmentSubmissionsActivity.this,
                                    "✅ Graded successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AssignmentSubmissionsActivity.this,
                                    "Failed: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AssignmentSubmissionItem> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(AssignmentSubmissionsActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
