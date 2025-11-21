package com.campussync.erp.attendance;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

public class StudentAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "StudentAttendanceAct";

    private TextView tvSummary, tvList;
    private Button btnRefresh;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        tvSummary = findViewById(R.id.tvSummaryStudent);
        tvList = findViewById(R.id.tvAttendanceListStudent);
        btnRefresh = findViewById(R.id.btnRefreshAttendance);

        btnRefresh.setOnClickListener(v -> load());

        // auto-load on start
        load();
    }

    private void load() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String courseId = "GENERAL"; // your backend still expects this

        BackendClient.getIdToken(new BackendClient.TokenCallback() {
            @Override
            public void onToken(String token) {
                // list
                String listPath = "/api/attendance/student/" + uid + "?courseId=" + courseId;
                BackendClient.get(listPath, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "list fail", e);
                        runOnUiThread(() ->
                                Toast.makeText(StudentAttendanceActivity.this,
                                        "List fail: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "list: " + response.code() + " " + body);
                        if (response.isSuccessful()) {
                            Type listType = new TypeToken<List<AttendanceRecord>>(){}.getType();
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
                            runOnUiThread(() -> tvList.setText(sb.toString()));
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(StudentAttendanceActivity.this,
                                            "List error: " + response.code(),
                                            Toast.LENGTH_LONG).show());
                        }
                    }
                });

                // summary
                String summaryPath = "/api/attendance/student/" + uid + "/summary?courseId=" + courseId;
                BackendClient.get(summaryPath, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "summary fail", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "summary: " + response.code() + " " + body);
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
                        Toast.makeText(StudentAttendanceActivity.this,
                                "Token error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}
