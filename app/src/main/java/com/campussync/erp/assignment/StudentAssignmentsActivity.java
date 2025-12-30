package com.campussync.erp.assignment;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentAssignmentsActivity extends AppCompatActivity {

    public static final String EXTRA_CLASS_ID = "classId";

    private MaterialToolbar toolbar;
    private TextView tvHeader;
    private Chip chipClass;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private RecyclerView rv;

    private StudentAssignmentAdapter adapter;
    private String classId;
    private String currentIdToken;
    private AssignmentItem pendingUpload;
    private final Set<String> submittedAssignmentIds = new HashSet<>(); // 🔥 PERSISTENT STATE

    private ActivityResultLauncher<String> pickPdfLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_assignments);

        toolbar = findViewById(R.id.toolbar_student_assignments);
        tvHeader = findViewById(R.id.tv_student_header);
        chipClass = findViewById(R.id.tv_student_class);
        progress = findViewById(R.id.progress_student_assignments);
        emptyState = findViewById(R.id.tv_student_assignments_empty);
        rv = findViewById(R.id.rv_student_assignments);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 🔥 NO "Loading..." - Will be set by Firebase
        if (chipClass != null) {
            chipClass.setText("👥 Loading...");
        }

        adapter = new StudentAssignmentAdapter(this, new ArrayList<>(), new StudentAssignmentAdapter.Listener() {
            @Override
            public void onSubmitClicked(AssignmentItem item) {
                startPickPdf(item);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        pickPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    if (pendingUpload == null) {
                        Toast.makeText(this, "No assignment selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    uploadSubmissionPdf(pendingUpload, uri);
                }
        );

        fetchTokenAndLoad();
    }

    private void fetchTokenAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    currentIdToken = result.getToken();
                    // 🔥 STEP 1: Get classId from Firebase student profile
                    fetchClassIdFromFirebase(user.getUid());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // 🔥 STEP 1: Read students/{uid}/classId from Firebase
    private void fetchClassIdFromFirebase(String uid) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(uid)
                .child("classId");

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                classId = snapshot.getValue(String.class);

                if (classId == null || classId.trim().isEmpty()) {
                    // 🔥 Fallback to intent extra
                    classId = getIntent().getStringExtra(EXTRA_CLASS_ID);
                    if (classId == null || classId.trim().isEmpty()) {
                        showLoading(false);
                        if (chipClass != null) chipClass.setText("👥 No class");
                        Toast.makeText(StudentAssignmentsActivity.this, "Class not assigned", Toast.LENGTH_LONG).show();
                        showEmpty();
                        return;
                    }
                }

                // 🔥 DISPLAY CLASS IMMEDIATELY
                if (chipClass != null) {
                    chipClass.setText("👥 " + classId.toUpperCase());
                }

                // 🔥 STEP 2: Load persistent submissions
                loadSubmittedAssignments();

                // 🔥 STEP 3: Load assignments
                loadAssignments();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showLoading(false);
                if (chipClass != null) chipClass.setText("👥 Error");
                Toast.makeText(StudentAssignmentsActivity.this, "Failed to load class", Toast.LENGTH_LONG).show();
                showEmpty();
            }
        });
    }

    // 🔥 PERSISTENT SUBMISSION TRACKING - LOAD
    private void loadSubmittedAssignments() {
        SharedPreferences prefs = getSharedPreferences("student_submissions", MODE_PRIVATE);
        submittedAssignmentIds.clear();

        String submittedJson = prefs.getString("submitted_" + classId, "[]");
        try {
            Type type = new TypeToken<Set<String>>(){}.getType();
            Set<String> saved = new Gson().fromJson(submittedJson, type);
            if (saved != null) submittedAssignmentIds.addAll(saved);
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }

    // 🔥 PERSISTENT SUBMISSION TRACKING - SAVE
    private void markSubmissionPersistent(String assignmentId) {
        if (assignmentId == null || classId == null) return;

        submittedAssignmentIds.add(assignmentId);

        SharedPreferences prefs = getSharedPreferences("student_submissions", MODE_PRIVATE);
        prefs.edit()
                .putString("submitted_" + classId, new Gson().toJson(submittedAssignmentIds))
                .apply();

        // Update adapter UI
        adapter.setSubmittedAssignments(submittedAssignmentIds);
    }

    private void loadAssignments() {
        if (currentIdToken == null || classId == null) {
            showLoading(false);
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        api.getAssignments("Bearer " + currentIdToken, classId)
                .enqueue(new Callback<List<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<List<AssignmentItem>> call, Response<List<AssignmentItem>> response) {
                        showLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            showEmpty();
                            Toast.makeText(StudentAssignmentsActivity.this,
                                    "Failed: " + response.code(), Toast.LENGTH_LONG).show();
                            return;
                        }

                        List<AssignmentItem> list = response.body();
                        adapter.setSubmittedAssignments(submittedAssignmentIds); // 🔥 PERSISTENT STATE
                        adapter.setItems(list);

                        if (list.isEmpty()) {
                            showEmpty();
                        } else {
                            emptyState.setVisibility(View.GONE);
                            rv.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<AssignmentItem>> call, Throwable t) {
                        showLoading(false);
                        showEmpty();
                        Toast.makeText(StudentAssignmentsActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
    }

    private void startPickPdf(AssignmentItem item) {
        pendingUpload = item;
        pickPdfLauncher.launch("application/pdf");
    }

    private void uploadSubmissionPdf(AssignmentItem item, Uri fileUri) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No token", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        try {
            File pdfFile = copyUriToCacheFile(fileUri, "submit_" + item.getId() + ".pdf");
            if (pdfFile == null) {
                showLoading(false);
                Toast.makeText(this, "Failed to read PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            RequestBody assignmentIdPart = RequestBody.create(
                    item.getId(),
                    MediaType.parse("text/plain")
            );

            RequestBody fileBody = RequestBody.create(
                    pdfFile,
                    MediaType.parse("application/pdf")
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file",
                    pdfFile.getName(),
                    fileBody
            );

            AssignmentApiService api = AssignmentRetrofitClient.getApiService();
            api.submitAssignmentPdf("Bearer " + currentIdToken, assignmentIdPart, filePart)
                    .enqueue(new Callback<com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem>() {
                        @Override
                        public void onResponse(Call<com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem> call,
                                               Response<com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem> response) {
                            showLoading(false);
                            if (response.isSuccessful()) {
                                Toast.makeText(StudentAssignmentsActivity.this,
                                        "✅ Submitted successfully!", Toast.LENGTH_LONG).show();

                                // 🔥 MARK PERSISTENTLY + UPDATE UI
                                markSubmissionPersistent(item.getId());
                                loadAssignments(); // Refresh list
                            } else {
                                Toast.makeText(StudentAssignmentsActivity.this,
                                        "Submit failed: " + response.code(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem> call, Throwable t) {
                            showLoading(false);
                            Toast.makeText(StudentAssignmentsActivity.this,
                                    "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToCacheFile(Uri uri, String outName) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;

            File outFile = new File(getCacheDir(), outName);
            FileOutputStream out = new FileOutputStream(outFile);

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rv.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
