package com.campussync.erp.assignment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Student UI for viewing and submitting assignments.
 *
 * Assumptions:
 * - FirebaseAuth is already initialized in your app.
 * - Current user is a STUDENT with a valid Firebase ID token.
 * - Student's classId is provided via Intent extra "classId", fallback to "CSE-2".
 */
public class StudentAssignmentsActivity extends AppCompatActivity
        implements StudentAssignmentAdapter.Listener {

    private static final String EXTRA_CLASS_ID = "classId";
    private static final String PREFS_NAME = "assignment_prefs";
    private static final String KEY_SUBMITTED_IDS = "submitted_ids";

    private MaterialToolbar toolbar;
    private TextView tvClass;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;

    private StudentAssignmentAdapter adapter;

    private String currentIdToken;
    private String classId;

    private AssignmentItem pendingAssignmentForUpload;

    private final ActivityResultLauncher<Intent> pickPdfLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null && pendingAssignmentForUpload != null) {
                                uploadSubmissionPdf(uri, pendingAssignmentForUpload);
                            }
                        } else {
                            pendingAssignmentForUpload = null;
                            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_assignments);

        toolbar = findViewById(R.id.toolbar_student_assignments);
        tvClass = findViewById(R.id.tv_student_class);
        progressBar = findViewById(R.id.progress_student_assignments);
        tvEmpty = findViewById(R.id.tv_student_assignments_empty);
        recyclerView = findViewById(R.id.rv_student_assignments);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        classId = getIntent().getStringExtra(EXTRA_CLASS_ID);
        if (classId == null || classId.isEmpty()) {
            classId = "CSE-2"; // fallback
        }
        tvClass.setText("Class: " + classId);

        adapter = new StudentAssignmentAdapter(
                this,
                new ArrayList<>(),
                this
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchIdTokenAndLoadAssignments();
    }

    /**
     * Helper to start this Activity from StudentDashboardActivity
     */

    private Set<String> getSubmittedIdsFromPrefs() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getStringSet(KEY_SUBMITTED_IDS, new java.util.HashSet<>());
    }

    private void addSubmittedIdToPrefs(String assignmentId) {
        if (assignmentId == null) return;
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        java.util.Set<String> current =
                new java.util.HashSet<>(prefs.getStringSet(KEY_SUBMITTED_IDS, new java.util.HashSet<>()));

        current.add(assignmentId);
        prefs.edit().putStringSet(KEY_SUBMITTED_IDS, current).apply();
    }

    public static Intent createIntent(AppCompatActivity from, String classId) {
        Intent i = new Intent(from, StudentAssignmentsActivity.class);
        i.putExtra(EXTRA_CLASS_ID, classId);
        return i;
    }

    private void fetchIdTokenAndLoadAssignments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    currentIdToken = result.getToken();
                    loadAssignments();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAssignments() {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        Call<List<AssignmentItem>> call = api.getAssignments(
                "Bearer " + currentIdToken,
                classId
        );

        call.enqueue(new Callback<List<AssignmentItem>>() {
            @Override
            public void onResponse(Call<List<AssignmentItem>> call, Response<List<AssignmentItem>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    List<AssignmentItem> list = response.body();
                    if (list == null || list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        adapter.setItems(new ArrayList<>());
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        adapter.setItems(list);

                        // 🔹 Load submitted IDs from SharedPreferences and apply to adapter
                        Set<String> submittedIds = getSubmittedIdsFromPrefs();
                        adapter.setSubmittedAssignments(submittedIds);
                    }
                }

            }

            @Override
            public void onFailure(Call<List<AssignmentItem>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error: " + t.getMessage());
            }
        });
    }

    // ======== StudentAssignmentAdapter.Listener ========

    @Override
    public void onSubmitPdfClicked(AssignmentItem item) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingAssignmentForUpload = item;
        openPdfPicker();
    }

    private void openPdfPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            // Optional: limit to openable PDFs
            String[] mimeTypes = new String[]{"application/pdf"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            pickPdfLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadSubmissionPdf(Uri uri, AssignmentItem assignment) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read bytes from Uri
        byte[] fileBytes;
        String fileName = "assignment.pdf";
        try {
            fileBytes = readBytesFromUri(uri);
            // Try to get file name from Uri
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment != null && lastSegment.contains("/")) {
                fileName = lastSegment.substring(lastSegment.lastIndexOf('/') + 1);
            } else if (lastSegment != null) {
                fileName = lastSegment;
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody assignmentIdPart = RequestBody.create(
                assignment.getId(),
                MediaType.parse("text/plain")
        );

        RequestBody fileRequestBody = RequestBody.create(
                fileBytes,
                MediaType.parse("application/pdf")
        );

        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file",
                fileName,
                fileRequestBody
        );

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        Call<AssignmentSubmissionItem> call = api.submitAssignmentPdf(
                "Bearer " + currentIdToken,
                assignmentIdPart,
                filePart
        );

        progressBar.setVisibility(View.VISIBLE);

        call.enqueue(new Callback<AssignmentSubmissionItem>() {
            @Override
            public void onResponse(Call<AssignmentSubmissionItem> call, Response<AssignmentSubmissionItem> response) {
                progressBar.setVisibility(View.GONE);
                pendingAssignmentForUpload = null;

                if (response.isSuccessful()) {
                    Toast.makeText(StudentAssignmentsActivity.this,
                            "Submitted successfully",
                            Toast.LENGTH_SHORT).show();
                    // Mark locally as submitted for status display
                    adapter.markSubmitted(assignment.getId());

                    // 🔹 Persist this submission so it stays even after back
                    addSubmittedIdToPrefs(assignment.getId());
                }  else if (response.code() == 403) {

                Toast.makeText(
                        StudentAssignmentsActivity.this,
                        "Assignment is closed. Submission not allowed.",
                        Toast.LENGTH_LONG
                ).show();

            } else {

                Toast.makeText(
                        StudentAssignmentsActivity.this,
                        "Submission failed. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }
                }



            @Override
            public void onFailure(Call<AssignmentSubmissionItem> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                pendingAssignmentForUpload = null;
                Toast.makeText(StudentAssignmentsActivity.this,
                        "Submit error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Unable to open input stream from Uri");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        inputStream.close();
        return buffer.toByteArray();
    }
}
