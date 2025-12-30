package com.campussync.erp.assignment;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Teacher/Admin UI for managing assignments:
 *  - Dynamically fetches classes from Firebase 'students' or 'users' node
 *  - Strictly filters assignments by selected class
 *  - Preserves PDF upload logic
 */
public class TeacherAssignmentsActivity extends AppCompatActivity
        implements TeacherAssignmentAdapter.Listener {

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvTeacherClassDisplay; // shows selected class
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private Spinner spinnerClassFilter;

    private TeacherAssignmentAdapter adapter;

    private String currentIdToken;
    private String selectedClassId = null;

    private final List<String> classList = new ArrayList<>();
    private ArrayAdapter<String> classSpinnerAdapter;
    private boolean ignoreFirstSelection = true;

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

        // Bind Views
        toolbar = findViewById(R.id.toolbar_teacher_assignments);
        progressBar = findViewById(R.id.progress_teacher_assignments);
        tvEmpty = findViewById(R.id.tv_teacher_assignments_empty);
        recyclerView = findViewById(R.id.rv_teacher_assignments);
        fabAdd = findViewById(R.id.fab_add_assignment);

        spinnerClassFilter = findViewById(R.id.spinner_class_filter);
        tvTeacherClassDisplay = findViewById(R.id.tv_teacher_class_display);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        // Setup RecyclerView
        adapter = new TeacherAssignmentAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showCreateOrEditDialog(null));

        setupClassFilterSpinner();
        fetchIdToken();

        // 🔥 Start loading classes
        fetchClassIdsFromStudents();
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            if (selectedClassId == null) {
                Toast.makeText(this, "Select class first", Toast.LENGTH_SHORT).show();
                return true;
            }
            loadAssignmentsForSelectedClass();
            return true;
        }
        return false;
    }

    private void setupClassFilterSpinner() {
        classSpinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                classList
        );
        spinnerClassFilter.setAdapter(classSpinnerAdapter);

        spinnerClassFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreFirstSelection) {
                    ignoreFirstSelection = false;
                    return;
                }

                if (position < 0 || position >= classList.size()) return;

                selectedClassId = classList.get(position);
                tvTeacherClassDisplay.setText("👥 Selected: " + selectedClassId);

                // Load assignments immediately when dropdown changes
                if (currentIdToken != null) {
                    loadAssignmentsForSelectedClass();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        tvTeacherClassDisplay.setText("👥 Select a class to begin");
    }

    private void fetchIdToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        user.getIdToken(true)
                .addOnSuccessListener(result -> currentIdToken = result.getToken())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show());
    }

    // =======================================================
    // 🔥 FIXED: Fetch classes from Firebase users/students node
    // =======================================================

    private void fetchClassIdsFromStudents() {
        progressBar.setVisibility(View.VISIBLE);

        // 🔥 TRY "students" node first
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> distinctClasses = new LinkedHashSet<>();

                if (snapshot.exists() && snapshot.hasChildren()) {
                    // 🔥 Extract classId from each student
                    for (DataSnapshot studentSnap : snapshot.getChildren()) {
                        String classId = studentSnap.child("classId").getValue(String.class);
                        if (classId != null && !classId.trim().isEmpty()) {
                            distinctClasses.add(classId.trim());
                        }
                    }
                    updateClassList(distinctClasses);
                } else {
                    // 🔥 Fallback to "users" node if "students" is empty/missing
                    fetchFromUsersNode();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // On error, also try fallback
                fetchFromUsersNode();
            }
        });
    }

    /**
     * 🔥 FALLBACK: Try users node if students node empty
     */
    private void fetchFromUsersNode() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> distinctClasses = new LinkedHashSet<>();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String classId = userSnap.child("classId").getValue(String.class);
                    String role = userSnap.child("role").getValue(String.class);

                    // 🔥 Only students (teachers won't have classId usually)
                    if (classId != null && !classId.trim().isEmpty() &&
                            (role == null || role.equalsIgnoreCase("student"))) {
                        distinctClasses.add(classId.trim());
                    }
                }

                updateClassList(distinctClasses);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("❌ No classes found\nCheck Firebase data");
            }
        });
    }

    /**
     * 🔥 Update spinner with sorted classes
     */
    private void updateClassList(Set<String> distinctClasses) {
        classList.clear();
        classList.addAll(distinctClasses);
        Collections.sort(classList);

        classSpinnerAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);

        if (classList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("📭 No classes found\nAdd students with classId first");
            return;
        }

        Toast.makeText(this, "✅ " + classList.size() + " classes loaded", Toast.LENGTH_SHORT).show();

        // 🔥 Auto-select first class
        ignoreFirstSelection = true;
        spinnerClassFilter.setSelection(0);
        selectedClassId = classList.get(0);
        tvTeacherClassDisplay.setText("👥 Selected: " + selectedClassId);

        if (currentIdToken != null) {
            loadAssignmentsForSelectedClass();
        }
    }

    // =======================================================
    // Loading Assignments with Strict Filtering
    // =======================================================

    /**
     * 🔥 FIXED: Proper UI state management
     */
    private void loadAssignmentsForSelectedClass() {
        if (currentIdToken == null || selectedClassId == null || selectedClassId.isEmpty()) {
            showEmptyState("⚠️ Select class first");
            return;
        }

        showLoadingState();
        Log.d("TeacherAssignments", "Loading assignments for: " + selectedClassId);

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        Call<List<AssignmentItem>> call = api.getAssignments("Bearer " + currentIdToken, selectedClassId);

        call.enqueue(new Callback<List<AssignmentItem>>() {
            @Override
            public void onResponse(Call<List<AssignmentItem>> call, Response<List<AssignmentItem>> response) {
                Log.d("TeacherAssignments", "API Response: " + response.code());

                List<AssignmentItem> allAssignments = response.body();
                if (allAssignments == null) allAssignments = new ArrayList<>();

                // 🔥 Filter by classId
                List<AssignmentItem> filtered = new ArrayList<>();
                for (AssignmentItem item : allAssignments) {
                    if (item != null && selectedClassId.equalsIgnoreCase(item.getClassId())) {
                        filtered.add(item);
                        Log.d("TeacherAssignments", "Added: " + item.getTitle() + " for " + item.getClassId());
                    }
                }

                Log.d("TeacherAssignments", "Filtered: " + filtered.size() + "/" + allAssignments.size());

                if (filtered.isEmpty()) {
                    showEmptyState("📭 No assignments for " + selectedClassId + "\n\n➕ Tap + to create first");
                } else {
                    showAssignments(filtered);
                }
            }

            @Override
            public void onFailure(Call<List<AssignmentItem>> call, Throwable t) {
                Log.e("TeacherAssignments", "API Failure: " + t.getMessage());
                showEmptyState("🌐 Network error\n" + t.getMessage());
            }
        });
    }

    /**
     * 🔥 Show loading spinner + hide everything else
     */
    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        tvTeacherClassDisplay.setText("🔄 Loading " + selectedClassId + "...");
    }

    /**
     * 🔥 Show assignments list
     */
    private void showAssignments(List<AssignmentItem> assignments) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvTeacherClassDisplay.setText("👥 " + selectedClassId + " (" + assignments.size() + ")");

        adapter.setItems(assignments);
        Log.d("TeacherAssignments", "Showing " + assignments.size() + " assignments");
    }

    /**
     * 🔥 Show empty message
     */
    private void showEmptyState(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        tvTeacherClassDisplay.setText("👥 " + selectedClassId);
    }

    // =======================================================
    // Adapter Listeners
    // =======================================================

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

    // =======================================================
    // PDF Upload Logic (UNCHANGED)
    // =======================================================

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
                    // Reload for current class
                    loadAssignmentsForSelectedClass();
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

    // =======================================================
    // Dialog Logic (Using loaded classList)
    // =======================================================

    private void showCreateOrEditDialog(@Nullable AssignmentItem existing) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_assignment, null, false);

        EditText etTitle = dialogView.findViewById(R.id.et_title);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Spinner spinnerClass = dialogView.findViewById(R.id.spinner_class);
        EditText etSubject = dialogView.findViewById(R.id.et_subject);
        EditText etDueDate = dialogView.findViewById(R.id.et_due_date);

        final long[] dueDateMillis = {System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L};

        // Populate dialog spinner with loaded classes
        ArrayAdapter<String> dialogClassAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                classList
        );
        spinnerClass.setAdapter(dialogClassAdapter);

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            etDescription.setText(existing.getDescription());
            etSubject.setText(existing.getSubject());
            dueDateMillis[0] = existing.getDueDate();

            // Set spinner to existing class
            int idx = classList.indexOf(existing.getClassId());
            if (idx >= 0) spinnerClass.setSelection(idx);
        } else {
            // Default to currently selected class in filter
            etSubject.setText("JAVA");
            if (selectedClassId != null) {
                int idx = classList.indexOf(selectedClassId);
                if (idx >= 0) spinnerClass.setSelection(idx);
            }
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
                    year, month, day
            );
            dpd.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Create Assignment" : "Edit Assignment")
                .setView(dialogView)
                .setPositiveButton(existing == null ? "Create" : "Update", (d, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();
                    String classId = "";
                    if (spinnerClass.getSelectedItem() != null) {
                        classId = spinnerClass.getSelectedItem().toString().trim();
                    }
                    String subject = etSubject.getText().toString().trim();

                    if (title.isEmpty() || classId.isEmpty() || subject.isEmpty()) {
                        Toast.makeText(this, "Title, Class and Subject are required", Toast.LENGTH_SHORT).show();
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
                new AssignmentApiService.CreateAssignmentRequest(title, desc, classId, subject, dueDateMillis);

        progressBar.setVisibility(View.VISIBLE);

        api.createAssignment("Bearer " + currentIdToken, body)
                .enqueue(new Callback<AssignmentItem>() {
                    @Override
                    public void onResponse(Call<AssignmentItem> call, Response<AssignmentItem> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TeacherAssignmentsActivity.this, "Assignment created", Toast.LENGTH_SHORT).show();

                            // Reload ONLY if created for currently viewed class
                            if (classId.equals(selectedClassId)) {
                                loadAssignmentsForSelectedClass();
                            }
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Create failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
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
                new AssignmentApiService.UpdateAssignmentRequest(title, desc, classId, subject, dueDateMillis);

        progressBar.setVisibility(View.VISIBLE);

        api.updateAssignment("Bearer " + currentIdToken, id, body)
                .enqueue(new Callback<AssignmentItem>() {
                    @Override
                    public void onResponse(Call<AssignmentItem> call, Response<AssignmentItem> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TeacherAssignmentsActivity.this, "Assignment updated", Toast.LENGTH_SHORT).show();
                            loadAssignmentsForSelectedClass();
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Update failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(TeacherAssignmentsActivity.this, "Assignment deleted", Toast.LENGTH_SHORT).show();
                            loadAssignmentsForSelectedClass();
                        } else {
                            Toast.makeText(TeacherAssignmentsActivity.this,
                                    "Delete failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
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
