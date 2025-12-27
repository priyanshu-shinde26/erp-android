package com.campussync.erp.attendance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StudentAttendanceActivity extends AppCompatActivity {

    TextView infoText, tvAttendancePercentage, tvAttendanceList;
    Button btnRefresh;

    private Gson gson = new Gson();
    private String token;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_student_attendance);

        infoText = findViewById(R.id.infoText);
        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage);
        tvAttendanceList = findViewById(R.id.tvAttendanceList);
        btnRefresh = findViewById(R.id.btnRefreshAttendance);

        btnRefresh.setOnClickListener(v -> loadAll());
        loadAll();
    }

    private void loadAll() {
        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String t) {
                token = t;
                loadProfile();
                loadAttendance();
                loadSummary();
            }

            @Override
            public void onError(Exception e) {
                toast("Auth error: " + e.getMessage());
            }
        });
    }

    // ✅ FIXED: Proper Authorization header
    private void loadProfile() {
        BackendClient.getWithAuth("/api/students/me", token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> toast("Profile load failed"));
            }

            @Override
            public void onResponse(Call call, Response r) throws IOException {
                if (!r.isSuccessful()) {
                    runOnUiThread(() -> toast("Profile API error: " + r.code()));
                    return;
                }

                try {
                    JSONObject json = new JSONObject(r.body().string());
                    String rollNo = json.optString("rollNo", "N/A");
                    String name = json.optString("name", "Student");

                    runOnUiThread(() ->
                            infoText.setText("Name: " + name + "\nRoll No: " + rollNo)
                    );
                } catch (JSONException e) {
                    runOnUiThread(() -> toast("Profile parse error: " + e.getMessage()));
                }
            }
        });
    }

    // ✅ FIXED: Proper header + error handling
    private void loadAttendance() {
        BackendClient.getWithAuth("/api/attendance/student/me", token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> toast("Attendance load failed"));
            }

            @Override
            public void onResponse(Call call, Response r) throws IOException {
                if (!r.isSuccessful()) {
                    runOnUiThread(() -> toast("Attendance API error: " + r.code()));
                    return;
                }

                String json = r.body().string();
                Type type = new TypeToken<List<AttendanceRecord>>(){}.getType();

                try {
                    List<AttendanceRecord> list = gson.fromJson(json, type);

                    runOnUiThread(() -> {
                        StringBuilder sb = new StringBuilder();
                        if (list != null && !list.isEmpty()) {
                            for (AttendanceRecord a : list) {
                                sb.append(a.date != null ? a.date : "Unknown")
                                        .append(": ")
                                        .append(a.status != null ? a.status : "Unknown");

                                if (a.classId != null && !a.classId.isEmpty()) {
                                    sb.append(" (").append(a.classId).append(")");
                                }
                                sb.append("\n");
                            }
                        } else {
                            sb.append("No attendance records found");
                        }
                        tvAttendanceList.setText(sb.toString());
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> toast("Attendance parse error: " + e.getMessage()));
                }
            }
        });
    }

    // ✅ FIXED: Proper AttendanceSummaryDto
    private void loadSummary() {
        BackendClient.getWithAuth("/api/attendance/student/me/summary", token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> toast("Summary load failed"));
            }

            @Override
            public void onResponse(Call call, Response r) throws IOException {
                if (!r.isSuccessful()) {
                    runOnUiThread(() -> toast("Summary API error: " + r.code()));
                    return;
                }

                String json = r.body().string();

                try {
                    AttendanceSummaryDto s = gson.fromJson(json, AttendanceSummaryDto.class);

                    runOnUiThread(() -> {
                        String percent = (s != null && s.totalClasses > 0)
                                ? String.format("%.1f%%", s.attendancePercentage)
                                : "0%";
                        tvAttendancePercentage.setText("Attendance: " + percent +
                                "\nTotal: " + (s != null ? s.totalClasses : 0) +
                                " | Present: " + (s != null ? s.presentCount : 0));
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> toast("Summary parse error: " + e.getMessage()));
                }
            }
        });
    }

    private void toast(String m) {
        runOnUiThread(() ->
                Toast.makeText(this, m, Toast.LENGTH_LONG).show());
    }
}
