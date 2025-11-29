package com.campussync.erp.assignment;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Teacher/Admin UI for managing assignments:
 *  - View list
 *  - Create / Edit / Delete
 *  - Upload Question PDF
 *  - Open submissions screen
 */
public class TeacherAssignmentsActivity extends AppCompatActivity
        implements TeacherAssignmentAdapter.Listener {

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    private TeacherAssignmentAdapter adapter;

    private String currentIdToken;

    private AssignmentItem pendingAssignmentForUpload;

    private final ActivityResultLauncher<Intent> pickPdfLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null && pendingAssignmentForUpload != null) {
                                uploadQuestionPdf(uri, pendingAssignmentForUpload);
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
        setContentView(R.layout.activity_teacher_assignment);

        toolbar = findViewById(R.id.toolbar_teacher_assignments);
        progressBar = findViewById(R.id.progress_teacher_assignments);
        tvEmpty = findViewById(R.id.tv_teacher_assignments_empty);
        recyclerView = findViewById(R.id.rv_teacher_assignments);
        fabAdd = findViewById(R.id.fab_add_assignment);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        adapter = new TeacherAssignmentAdapter(
                this,
                new ArrayList<>(),
                this
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showCreateOrEditDialog(null));

        fetchIdTokenAndLoadAssignments();
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadAssignments();
            return true;
        }
        return false;
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
        // For teacher view, we can see all assignments
        Call<List<AssignmentItem>> call = api.getAssignments(
                "Bearer " + currentIdToken,
                null   // no classId -> all
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
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load assignments (" + response.code() + ")");
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

    // ====== Adapter Listener methods ======

    @Override
    public void onUploadQuestionClicked(AssignmentItem item) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingAssignmentForUpload = item;
        openPdfPicker();
    }

    @Override
    public void onViewSubmissionsClicked(AssignmentItem item) {
        Intent i = AssignmentSubmissionsActivity.createIntent(this, item.getId(), item.getTitle());
        startActivity(i);
    }

    @Override
    public void onEditAssignmentClicked(AssignmentItem item) {
        showCreateOrEditDialog(item);
    }

    @Override
    public void onDeleteAssignmentClicked(AssignmentItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete assignment?")
                .setMessage("Are you sure you want to delete \"" + item.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAssignment(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ====== Question PDF upload ======

    private void openPdfPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            pickPdfLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadQuestionPdf(Uri uri, AssignmentItem assignment) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] fileBytes;
        String fileName = "question.pdf";
        try {
            fileBytes = readBytesFromUri(uri);
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment != null) {
                int idx = lastSegment.lastIndexOf('/');
                fileName = (idx >= 0 && idx < lastSegment.length() - 1)
                        ? lastSegment.substring(idx + 1)
                        : lastSegment;
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
        Call<AssignmentItem> call = api.uploadQuestionPdf(
                "Bearer " + currentIdToken,
                assignmentIdPart,
                filePart
        );

        progressBar.setVisibility(View.VISIBLE);

        call.enqueue(new Callback<AssignmentItem>() {
            @Override
            public void onResponse(Call<AssignmentItem> call, Response<AssignmentItem> response) {
                progressBar.setVisibility(View.GONE);
                pendingAssignmentForUpload = null;

                if (response.isSuccessful()) {
                    Toast.makeText(TeacherAssignmentsActivity.this,
                            "Question PDF uploaded", Toast.LENGTH_SHORT).show();
                    loadAssignments();
                } else {
                    Toast.makeText(TeacherAssignmentsActivity.this,
                            "Upload failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AssignmentItem> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                pendingAssignmentForUpload = null;
                Toast.makeText(TeacherAssignmentsActivity.this,
                        "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    // ====== Create / Edit assignment dialog ======

    private void showCreateOrEditDialog(@Nullable AssignmentItem existing) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_assignment, null, false);

        EditText etTitle = dialogView.findViewById(R.id.et_title);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        EditText etClassId = dialogView.findViewById(R.id.et_class_id);
        EditText etSubject = dialogView.findViewById(R.id.et_subject);
        EditText etDueDate = dialogView.findViewById(R.id.et_due_date);

        final long[] dueDateMillis = {System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L};

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            etDescription.setText(existing.getDescription());
            etClassId.setText(existing.getClassId());
            etSubject.setText(existing.getSubject());
            dueDateMillis[0] = existing.getDueDate();
        } else {
            etClassId.setText("CSE-2");
            etSubject.setText("JAVA");
        }

        etDueDate.setText(DateFormat.format("dd/MM/yyyy", dueDateMillis[0]));
        etDueDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(dueDateMillis[0]);
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (DatePicker view, int y, int m, int d) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(y, m, d, 23, 59, 59);
                        picked.set(Calendar.MILLISECOND, 0);
                        dueDateMillis[0] = picked.getTimeInMillis();
                        etDueDate.setText(DateFormat.format("dd/MM/yyyy", picked));
                    },
                    year,
                    month,
                    day
            );
            dpd.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Create Assignment" : "Edit Assignment")
                .setView(dialogView)
                .setPositiveButton(existing == null ? "Create" : "Update", (d, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();
                    String classId = etClassId.getText().toString().trim();
                    String subject = etSubject.getText().toString().trim();

                    if (title.isEmpty() || classId.isEmpty() || subject.isEmpty()) {
                        Toast.makeText(this, "Title, Class ID and Subject are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existing == null) {
                        createAssignment(title, desc, classId, subject, dueDateMillis[0]);
                    } else {
                        updateAssignment(existing.getId(), title, desc, classId, subject, dueDateMillis[0]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void createAssignment(String title, String desc, String classId,
                                  String subject, long dueDateMillis) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();

        AssignmentApiService.CreateAssignmentRequest body =
                new AssignmentApiService.CreateAssignmentRequest(
                        title, desc, classId, subject, dueDateMillis
                );

        progressBar.setVisibility(View.VISIBLE);

        api.createAssignment("Bearer " + currentIdToken, body)
                .enqueue(new Callback<AssignmentItem>() {
                    @Override
                    public void onResponse(Call<AssignmentItem> call, Response<AssignmentItem> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Assignment created", Toast.LENGTH_SHORT).show();
                            loadAssignments();
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Create failed (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AssignmentItem> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeacherAssignmentsActivity.this,
                                "Create error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateAssignment(String id, String title, String desc, String classId,
                                  String subject, long dueDateMillis) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();

        AssignmentApiService.UpdateAssignmentRequest body =
                new AssignmentApiService.UpdateAssignmentRequest(
                        title, desc, classId, subject, dueDateMillis
                );

        progressBar.setVisibility(View.VISIBLE);

        api.updateAssignment("Bearer " + currentIdToken, id, body)
                .enqueue(new Callback<AssignmentItem>() {
                    @Override
                    public void onResponse(Call<AssignmentItem> call, Response<AssignmentItem> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Assignment updated", Toast.LENGTH_SHORT).show();
                            loadAssignments();
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Update failed (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AssignmentItem> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeacherAssignmentsActivity.this,
                                "Update error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteAssignment(AssignmentItem item) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        progressBar.setVisibility(View.VISIBLE);

        api.deleteAssignment("Bearer " + currentIdToken, item.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Assignment deleted", Toast.LENGTH_SHORT).show();
                            loadAssignments();
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Delete failed (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TeacherAssignmentsActivity.this,
                                "Delete error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
