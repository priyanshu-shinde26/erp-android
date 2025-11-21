package com.campussync.erp.attendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ManageAttendanceActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://10.0.2.2:9090";
    private static final String DEFAULT_COURSE_ID = "GENERAL";
    private static final String TAG = "ManageAttendanceActivity";

    private EditText etStudentUid, etDate, etCourseId;
    private Button btnMarkPresent, btnMarkAbsent, btnLoad;
    private TextView tvSummary, tvAttendanceList;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_attendance);

        etStudentUid = findViewById(R.id.etStudentUid);
        etDate = findViewById(R.id.etDate);
        etCourseId = findViewById(R.id.etCourseId);
        btnMarkPresent = findViewById(R.id.btnMarkPresent);
        btnMarkAbsent = findViewById(R.id.btnMarkAbsent);
        btnLoad = findViewById(R.id.btnLoadAttendance);
        tvSummary = findViewById(R.id.tvSummary);
        tvAttendanceList = findViewById(R.id.tvAttendanceList);

        btnMarkPresent.setOnClickListener(v -> mark("PRESENT"));
        btnMarkAbsent.setOnClickListener(v -> mark("ABSENT"));
        btnLoad.setOnClickListener(v -> loadAttendance());
    }

    private void mark(String status) {
        String studentUid = etStudentUid.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String courseId = etCourseId.getText().toString().trim();

        if (TextUtils.isEmpty(studentUid) || TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Student UID and date required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(courseId)) {
            courseId = "GENERAL"; // default
        }

        // Build JSON body same as backend expects
        String json = "{"
                + "\"studentUid\":\"" + studentUid + "\","
                + "\"courseId\":\"" + courseId + "\","
                + "\"date\":\"" + date + "\","
                + "\"status\":\"" + status + "\""
                + "}";

        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String token) {
                BackendClient.postJson("/api/attendance/mark", token, json, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "mark onFailure", e);
                        runOnUiThread(() ->
                                Toast.makeText(ManageAttendanceActivity.this,
                                        "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "mark onResponse: " + response.code() + " " + body);

                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageAttendanceActivity.this,
                                        "Attendance marked: " + status,
                                        Toast.LENGTH_SHORT).show();
                            } else if (response.code() == 403) {
                                Toast.makeText(ManageAttendanceActivity.this,
                                        "Forbidden: only ADMIN/TEACHER can mark",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ManageAttendanceActivity.this,
                                        "Error: " + response.code(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "getIdToken error", e);
                runOnUiThread(() ->
                        Toast.makeText(ManageAttendanceActivity.this,
                                "Not logged in / token error",
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void loadAttendance() {
        String studentUid = etStudentUid.getText().toString().trim();
        String courseId = etCourseId.getText().toString().trim();
        if (TextUtils.isEmpty(studentUid)) {
            Toast.makeText(this, "Student UID required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(courseId)) {
            courseId = "GENERAL";
        }

        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String token) {
                // 1) List records
                final String listPath = "/api/attendance/student/" + studentUid;

                BackendClient.get(listPath, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "loadAttendance list onFailure", e);
                        runOnUiThread(() ->
                                Toast.makeText(ManageAttendanceActivity.this,
                                        "List fail: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "list onResponse: " + response.code() + " " + body);
                        if (response.isSuccessful()) {
                            Type listType = new TypeToken<List<AttendanceRecord>>() {}.getType();
                            List<AttendanceRecord> records = gson.fromJson(body, listType);

                            StringBuilder sb = new StringBuilder();
                            if (records != null) {
                                for (AttendanceRecord r : records) {
                                    sb.append(r.date)
                                            .append(" : ")
                                            .append(r.status)
                                            .append("\n");
                                }
                            }
                            runOnUiThread(() -> tvAttendanceList.setText(sb.toString()));
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(ManageAttendanceActivity.this,
                                            "List error: " + response.code(),
                                            Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                });

                // 2) Summary
                final String summaryPath = "/api/attendance/student/" + studentUid
                        + "/summary";
                BackendClient.get(summaryPath, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "summary onFailure", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "summary onResponse: " + response.code() + " " + body);
                        if (response.isSuccessful()) {
                            AttendanceSummaryDto summary =
                                    gson.fromJson(body, AttendanceSummaryDto.class);
                            runOnUiThread(() -> {
                                String text = "Summary: total=" + summary.totalClasses
                                        + ", present=" + summary.presentCount
                                        + ", absent=" + summary.absentCount
                                        + ", %=" + summary.attendancePercentage;
                                tvSummary.setText(text);
                            });
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ManageAttendanceActivity.this,
                                "Token error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}
