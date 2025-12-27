package com.campussync.erp.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.StudentAssignmentsActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ManageAttendanceActivity extends AppCompatActivity
        implements StudentAttendanceAdapter.OnAttendanceClickListener {

    private static final String TAG = "ManageAttendanceActivity";

    private RecyclerView recyclerView;
    private StudentAttendanceAdapter adapter;
    private Spinner spinnerClass;  // NEW: Class spinner
    private final Gson gson = new Gson();
    private String token;
    private String selectedClassId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_attendance);

        TextView tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentDate.setText(getFormattedCurrentDate());

        // NEW: Initialize class spinner
        spinnerClass = findViewById(R.id.spinnerClass);

        recyclerView = findViewById(R.id.recyclerStudents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Adapter will receive student list later
        adapter = new StudentAttendanceAdapter(this);
        recyclerView.setAdapter(adapter);

        // ✅ FIXED: Proper TokenCallback implementation
        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String idToken) {
                token = idToken;
                loadClasses();
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ManageAttendanceActivity.this, "Authentication error", Toast.LENGTH_LONG).show());
            }
        });
    }


    // NEW: Token callback - loads classes first
    private void onTokenReceived(String idToken) {
        token = idToken;
        loadClasses();
    }

    // NEW: Load classes for spinner
    private void loadClasses() {
        BackendClient.get("/api/classes", token, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Type type = new TypeToken<List<ClassModel>>(){}.getType();
                List<ClassModel> classes = gson.fromJson(response.body().string(), type);

                runOnUiThread(() -> {
                    ArrayAdapter<ClassModel> adapter = new ArrayAdapter<>(
                            ManageAttendanceActivity.this,
                            android.R.layout.simple_spinner_item,
                            classes
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerClass.setAdapter(adapter);

                    spinnerClass.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            ClassModel selectedClass = (ClassModel) parent.getItemAtPosition(position);
                            loadStudentsForClass(selectedClass.classId);
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ManageAttendanceActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // UPDATED: Load students for specific class
    private void loadStudentsForClass(String classId) {
        BackendClient.get("/api/students/class/" + classId, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ManageAttendanceActivity.this, "Failed to load students", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    List<StudentModel> list = gson.fromJson(
                            response.body().string(),
                            new TypeToken<List<StudentModel>>() {}.getType()
                    );
                    runOnUiThread(() -> adapter.setStudents(list));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ManageAttendanceActivity.this, "Error loading students: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    /**
     * Called when PRESENT / ABSENT button is clicked
     */
    @Override
    public void onAttendanceClick(String rollNumber, String status) {
        markAttendance(rollNumber, status);
    }
    @Override
    public void onViewSummaryClick(String rollNumber) {

        Spinner spinnerClass = findViewById(R.id.spinnerClass);

        if (spinnerClass.getSelectedItem() == null) {
            Toast.makeText(
                    this,
                    "Please select class first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String classId = spinnerClass.getSelectedItem().toString();

        Intent intent = new Intent(
                ManageAttendanceActivity.this,
                StudentSummaryActivity.class
        );

        intent.putExtra("classId", classId);
        intent.putExtra("rollNumber", rollNumber);

        startActivity(intent);
    }

    // UPDATED: Use class-based attendance API
    private void markAttendance(String rollNumber, String status) {
        ClassModel selectedClass = (ClassModel) spinnerClass.getSelectedItem();
        if (selectedClass == null) {
            Toast.makeText(this, "Please select a class first", Toast.LENGTH_SHORT).show();
            return;
        }

        String json = gson.toJson(Map.of(
                "classId", selectedClass.classId,
                "rollNumber", rollNumber,
                "status", status
        ));

        BackendClient.postJson(
                "/api/attendance/class/mark",  // UPDATED: New class-based endpoint
                token,
                json,
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "markAttendance failed", e);
                        runOnUiThread(() ->
                                Toast.makeText(ManageAttendanceActivity.this, "Failed to mark attendance", Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(
                                        ManageAttendanceActivity.this,
                                        rollNumber + " marked " + status,
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else if (response.code() == 403) {
                                Toast.makeText(
                                        ManageAttendanceActivity.this,
                                        "Only TEACHER / ADMIN allowed",
                                        Toast.LENGTH_LONG
                                ).show();
                            } else {
                                Toast.makeText(
                                        ManageAttendanceActivity.this,
                                        "Error: " + response.code(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                }
        );
    }

    // REMOVED: Old loadStudents() - replaced by class-based loading

    private String getFormattedCurrentDate() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
