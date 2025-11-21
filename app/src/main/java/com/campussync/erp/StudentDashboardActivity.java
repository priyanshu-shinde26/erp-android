package com.campussync.erp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StudentDashboardActivity extends AppCompatActivity {
    private static final String TAG = "StudentDashboard";

    // ✅ Change base URL if your backend is different
    private static final String BASE_URL = "http://10.0.2.2:9090";
    // ✅ We agreed to use a general course bucket
    private static final String COURSE_ID = "GENERAL";

    private TextView infoText;
    private TextView attendanceSummaryText;
    private TextView attendanceListText;

    private FirebaseAuth mAuth;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        infoText = findViewById(R.id.infoText);
        attendanceSummaryText = findViewById(R.id.tvAttendanceSummary);
        attendanceListText = findViewById(R.id.tvAttendanceList);

        mAuth = FirebaseAuth.getInstance();

        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        infoText.setText("Loading profile...");
        attendanceSummaryText.setText("Loading attendance summary...");
        attendanceListText.setText("Loading attendance records...");

        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().getIdToken(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String idToken = task.getResult().getToken();
                            if (idToken == null) {
                                Toast.makeText(this, "Token is null", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Log.d(TAG, "Firebase ID Token obtained (student dashboard)");

                            // ✅ Call all three endpoints with same token
                            fetchStudentDetails(idToken);
                            fetchAttendanceSummary(idToken);
                            fetchAttendanceList(idToken);
                        } else {
                            Log.e(TAG, "Failed to get ID token", task.getException());
                            Toast.makeText(this, "Failed to get token", Toast.LENGTH_SHORT).show();
                            infoText.setText("Failed to get token");
                            attendanceSummaryText.setText("Failed to get token");
                            attendanceListText.setText("Failed to get token");
                        }
                    });
        } else {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ---------- 1) Student profile ----------

    private void fetchStudentDetails(String idToken) {
        String uid = mAuth.getCurrentUser().getUid();
        String url = BASE_URL + "/api/students/" + uid;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchStudentDetails failed", e);
                runOnUiThread(() ->
                        infoText.setText("Backend call failed: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String body = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "[PROFILE] code: " + response.code() + ", body: " + body);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(body);
                            String info = "Name: " + json.optString("name", "—") +
                                    "\nRoll No: " + json.optString("rollNo", "—") +
                                    "\nCourse: " + json.optString("course", "—") +
                                    "\nYear: " + json.optString("year", "—") +
                                    "\nSemester: " + json.optString("semester", "—") +
                                    "\nEmail: " + json.optString("email", "—") +
                                    "\nContact: " + json.optString("contact", "—");
                            infoText.setText(info);
                        } catch (Exception e) {
                            Log.e(TAG, "JSON parse error (profile)", e);
                            infoText.setText("Error parsing student data");
                        }
                    } else if (response.code() == 404) {
                        infoText.setText("Student not found (404).\nAsk admin to create your student record.");
                    } else if (response.code() == 401) {
                        infoText.setText("Unauthorized (401). Try signing in again.");
                    } else {
                        infoText.setText("Backend error: " + response.code());
                    }
                });
            }
        });
    }

    // ---------- 2) Attendance summary ----------

    private void fetchAttendanceSummary(String idToken) {
        String uid = mAuth.getCurrentUser().getUid();
        String url = BASE_URL + "/api/attendance/student/" + uid + "/summary?courseId=" + COURSE_ID;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchAttendanceSummary failed", e);
                runOnUiThread(() ->
                        attendanceSummaryText.setText("Failed to load summary: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String body = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "[SUMMARY] code: " + response.code() + ", body: " + body);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(body);
                            int total = json.optInt("totalClasses", 0);
                            int present = json.optInt("presentCount", 0);
                            int absent = json.optInt("absentCount", 0);
                            double percent = json.optDouble("attendancePercentage", 0.0);

                            String summary = "Total Classes: " + total +
                                    "\nPresent: " + present +
                                    "\nAbsent: " + absent +
                                    "\nAttendance: " + String.format("%.2f", percent) + "%";

                            attendanceSummaryText.setText(summary);
                        } catch (Exception e) {
                            Log.e(TAG, "JSON parse error (summary)", e);
                            attendanceSummaryText.setText("Error parsing summary data");
                        }
                    } else if (response.code() == 404) {
                        attendanceSummaryText.setText("No attendance summary yet.");
                    } else if (response.code() == 401) {
                        attendanceSummaryText.setText("Unauthorized to view summary.");
                    } else {
                        attendanceSummaryText.setText("Backend error (summary): " + response.code());
                    }
                });
            }
        });
    }

    // ---------- 3) Attendance list (records) ----------

    private void fetchAttendanceList(String idToken) {
        String uid = mAuth.getCurrentUser().getUid();
        String url = BASE_URL + "/api/attendance/student/" + uid + "?courseId=" + COURSE_ID;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchAttendanceList failed", e);
                runOnUiThread(() ->
                        attendanceListText.setText("Failed to load attendance list: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String body = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "[LIST] code: " + response.code() + ", body: " + body);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONArray arr = new JSONArray(body);
                            if (arr.length() == 0) {
                                attendanceListText.setText("No attendance records yet.");
                                return;
                            }

                            StringBuilder sb = new StringBuilder();
                            sb.append("Date    | Status\n");
                            sb.append("---------------------\n");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String date = obj.optString("date", "—");
                                String status = obj.optString("status", "—");
                                sb.append(date).append("  :  ").append(status).append("\n");
                            }
                            attendanceListText.setText(sb.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "JSON parse error (list)", e);
                            attendanceListText.setText("Error parsing attendance list");
                        }
                    } else if (response.code() == 404) {
                        attendanceListText.setText("No attendance records found.");
                    } else if (response.code() == 401) {
                        attendanceListText.setText("Unauthorized to view attendance list.");
                    } else if (response.code() == 403) {
                        attendanceListText.setText("Forbidden: not allowed to view this student's attendance.");
                    } else {
                        attendanceListText.setText("Backend error (list): " + response.code());
                    }
                });
            }
        });
    }
}
