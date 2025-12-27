package com.campussync.erp.attendance;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StudentSummaryActivity extends AppCompatActivity {

    private static final String TAG = "StudentSummary";

    // Intent extras (teacher/admin mode)
    public static final String EXTRA_CLASS_ID = "classId";
    public static final String EXTRA_ROLL_NUMBER = "rollNumber";

    private TextView tvSummary;
    private TextView tvList;
    private Button btnRefreshAttendance;

    private String token;

    // Teacher/Admin view
    private String classIdFromIntent;
    private String rollFromIntent;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_summary);

        tvSummary = findViewById(R.id.tvAttSummary);
        tvList = findViewById(R.id.tvAttendanceList);
        btnRefreshAttendance = findViewById(R.id.btnRefreshAttendance);

        classIdFromIntent = getIntent().getStringExtra(EXTRA_CLASS_ID);
        rollFromIntent = getIntent().getStringExtra(EXTRA_ROLL_NUMBER);

        btnRefreshAttendance.setOnClickListener(v -> {
            if (token != null) {
                loadSummary();
                loadAttendance();
            }
        });

        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String t) {
                token = t;
                loadSummary();
                loadAttendance();
            }

            @Override
            public void onError(Exception e) {
                toast("Authentication failed");
            }
        });
    }

    // ===================== MODE CHECK =====================

    private boolean isTeacherView() {
        return classIdFromIntent != null && !classIdFromIntent.isEmpty()
                && rollFromIntent != null && !rollFromIntent.isEmpty();
    }

    // ===================== LOAD SUMMARY =====================

    private void loadSummary() {

        String url;

        if (isTeacherView()) {
            // ✅ CORRECT teacher endpoint
            url = "/api/attendance/teacher/student/summary"
                    + "?classId=" + classIdFromIntent
                    + "&rollNumber=" + rollFromIntent;
        } else {
            // ✅ student self view
            url = "/api/attendance/student/me/summary";
        }

        BackendClient.get(url, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Summary API failed", e);
                runOnUiThread(() -> tvSummary.setText("Attendance: -"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Summary API error code=" + response.code());
                    runOnUiThread(() -> tvSummary.setText("Attendance: -"));
                    return;
                }

                AttendanceSummaryDto dto =
                        gson.fromJson(response.body().string(), AttendanceSummaryDto.class);

                runOnUiThread(() -> {
                    if (dto == null) {
                        tvSummary.setText("Attendance: -");
                        return;
                    }

                    String header = isTeacherView()
                            ? "Roll: " + rollFromIntent +
                            " | Class: " + classIdFromIntent + "\n"
                            : "";

                    tvSummary.setText(
                            header +
                                    "Attendance: " + String.format("%.1f", dto.attendancePercentage) + "%\n" +
                                    "Total: " + dto.totalClasses +
                                    " | Present: " + dto.presentCount +
                                    " | Absent: " + dto.absentCount
                    );
                });
            }
        });
    }

    // ===================== LOAD DAY-WISE ATTENDANCE =====================

    private void loadAttendance() {

        String url;

        if (isTeacherView()) {
            // ❌ There is NO day-wise teacher API yet
            // So we show only summary in teacher mode
            tvList.setText("Day-wise attendance not available for teacher view");
            return;
        } else {
            url = "/api/attendance/student/me";
        }

        BackendClient.get(url, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Attendance API failed", e);
                runOnUiThread(() -> tvList.setText("Failed to load attendance"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Attendance API error code=" + response.code());
                    runOnUiThread(() -> tvList.setText("No attendance data"));
                    return;
                }

                Type type = new TypeToken<List<AttendanceRecord>>() {}.getType();
                List<AttendanceRecord> list =
                        gson.fromJson(response.body().string(), type);

                runOnUiThread(() -> {
                    if (list == null || list.isEmpty()) {
                        tvList.setText("No attendance records found");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    for (AttendanceRecord r : list) {
                        sb.append(r.getDate())
                                .append(" : ")
                                .append(r.getStatus())
                                .append("\n");
                    }
                    tvList.setText(sb.toString());
                });
            }
        });
    }

    private void toast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(StudentSummaryActivity.this, msg, Toast.LENGTH_LONG).show()
        );
    }
}
