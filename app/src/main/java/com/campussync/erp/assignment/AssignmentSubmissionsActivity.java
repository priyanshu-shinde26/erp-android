package com.campussync.erp.assignment;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Teacher/Admin screen for viewing submissions of a single assignment
 * and grading them.
 */
public class AssignmentSubmissionsActivity extends AppCompatActivity
        implements SubmissionAdapter.Listener {

    private static final String EXTRA_ASSIGNMENT_ID = "assignmentId";
    private static final String EXTRA_ASSIGNMENT_TITLE = "assignmentTitle";

    private MaterialToolbar toolbar;
    private TextView tvAssignmentTitle;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;

    private SubmissionAdapter adapter;
    private String currentIdToken;
    private String assignmentId;
    private String assignmentTitle;

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

        toolbar = findViewById(R.id.toolbar_assignment_submissions);
        tvAssignmentTitle = findViewById(R.id.tv_assignment_title);
        progressBar = findViewById(R.id.progress_assignment_submissions);
        tvEmpty = findViewById(R.id.tv_assignment_submissions_empty);
        recyclerView = findViewById(R.id.rv_assignment_submissions);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        assignmentId = getIntent().getStringExtra(EXTRA_ASSIGNMENT_ID);
        assignmentTitle = getIntent().getStringExtra(EXTRA_ASSIGNMENT_TITLE);
        if (assignmentTitle == null) assignmentTitle = "";
        tvAssignmentTitle.setText("Assignment: " + assignmentTitle);

        adapter = new SubmissionAdapter(this, new ArrayList<>(), this);
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

        progressBar.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    currentIdToken = result.getToken();
                    loadSubmissions();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSubmissions() {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        Call<List<AssignmentSubmissionItem>> call = api.getSubmissionsForAssignment(
                "Bearer " + currentIdToken,
                assignmentId
        );

        call.enqueue(new Callback<List<AssignmentSubmissionItem>>() {
            @Override
            public void onResponse(Call<List<AssignmentSubmissionItem>> call,
                                   Response<List<AssignmentSubmissionItem>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful()) {
                    List<AssignmentSubmissionItem> list = response.body();
                    if (list == null || list.isEmpty()) {
                        tvEmpty.setVisibility(android.view.View.VISIBLE);
                        adapter.setItems(new ArrayList<>());
                    } else {
                        tvEmpty.setVisibility(android.view.View.GONE);
                        adapter.setItems(list);
                    }
                } else {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    tvEmpty.setText("Failed (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<AssignmentSubmissionItem>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                tvEmpty.setVisibility(android.view.View.VISIBLE);
                tvEmpty.setText("Error: " + t.getMessage());
            }
        });
    }

    // ===== SubmissionAdapter.Listener =====

    @Override
    public void onGradeClicked(AssignmentSubmissionItem submission) {
        showGradeDialog(submission);
    }

    private void showGradeDialog(AssignmentSubmissionItem submission) {
        EditText etMarks = new EditText(this);
        etMarks.setHint("Marks (integer)");
        etMarks.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (submission.getMarks() != null) {
            etMarks.setText(String.valueOf(submission.getMarks()));
        }

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Feedback (optional)");
        etFeedback.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (submission.getFeedback() != null) {
            etFeedback.setText(submission.getFeedback());
        }

        androidx.appcompat.widget.LinearLayoutCompat container =
                new androidx.appcompat.widget.LinearLayoutCompat(this);
        container.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etMarks);
        container.addView(etFeedback);

        new AlertDialog.Builder(this)
                .setTitle("Grade submission")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String marksStr = etMarks.getText().toString().trim();
                    Integer marks = null;
                    if (!marksStr.isEmpty()) {
                        try {
                            marks = Integer.parseInt(marksStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid marks", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    String feedback = etFeedback.getText().toString().trim();
                    gradeSubmission(submission, marks, feedback);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void gradeSubmission(AssignmentSubmissionItem submission,
                                 Integer marks,
                                 String feedback) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        AssignmentApiService.GradeSubmissionRequest body =
                new AssignmentApiService.GradeSubmissionRequest(marks, feedback);

        progressBar.setVisibility(android.view.View.VISIBLE);

        api.gradeSubmission(
                "Bearer " + currentIdToken,
                submission.getAssignmentId(),
                submission.getStudentUid(),
                submission.getId(),
                body
        ).enqueue(new Callback<AssignmentSubmissionItem>() {
            @Override
            public void onResponse(Call<AssignmentSubmissionItem> call,
                                   Response<AssignmentSubmissionItem> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AssignmentSubmissionsActivity.this,
                            "Graded successfully", Toast.LENGTH_SHORT).show();

                    // Update UI immediately using local data
                    adapter.updateSubmissionLocal(submission.getId(), marks, feedback);

                    // Optionally also reload from backend to be 100% in sync:
                    // loadSubmissions();
                } else {
                    Toast.makeText(AssignmentSubmissionsActivity.this,
                            "Grade failed (" + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AssignmentSubmissionItem> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(AssignmentSubmissionsActivity.this,
                        "Grade error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
