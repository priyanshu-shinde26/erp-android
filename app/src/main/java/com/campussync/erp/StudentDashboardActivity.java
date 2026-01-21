package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.assignment.StudentAssignmentsActivity;
import com.campussync.erp.attendance.StudentAttendanceActivity;
import com.campussync.erp.lms.StudentLmsActivity;
import com.campussync.erp.timetable.StudentTimetableActivity;
import com.google.firebase.auth.FirebaseAuth;

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
    private static final String BASE_URL = "http://10.0.2.2:9090";

    private TextView infoText;
    private FirebaseAuth mAuth;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        infoText = findViewById(R.id.infoText);
        Button btnViewAttendance = findViewById(R.id.btnViewAttendance);
        Button btnViewTimetable = findViewById(R.id.btnViewTimetable);
        Button btnAssignments = findViewById(R.id.btn_assignments);
        Button btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();

        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        infoText.setText("Loading profile...");

        // ---------- Button Actions ----------

        btnViewAttendance.setOnClickListener(v ->
                startActivity(
                        new Intent(this, StudentAttendanceActivity.class)
                )
        );

        btnViewTimetable.setOnClickListener(v ->
                startActivity(
                        new Intent(this, StudentTimetableActivity.class)
                )
        );

        btnAssignments.setOnClickListener(v ->
                startActivity(
                        new Intent(this, StudentAssignmentsActivity.class)
                )
        );

        btnLogout.setOnClickListener(v -> performLogout());

        // ---------- Load Student Profile ONLY ----------

        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().getIdToken(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String idToken = task.getResult().getToken();
                            if (idToken != null) {
                                fetchStudentDetails(idToken);
                            }
                        } else {
                            Toast.makeText(this, "Failed to get token", Toast.LENGTH_SHORT).show();
                            infoText.setText("Failed to authenticate");
                        }
                    });
        } else {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            finish();
        }
        // In onCreate():
        Button btnLms = findViewById(R.id.btnLms);
        btnLms.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, StudentLmsActivity.class);
            startActivity(intent);
        });

    }

    // ================= STUDENT PROFILE =================

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
                        infoText.setText("Failed to load profile")
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                final String body = response.body() != null
                        ? response.body().string()
                        : "";

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(body);

                            // ✅ NEW: contact-safe extraction
                            String contact =
                                    json.has("contact") ? json.optString("contact") :
                                            json.has("phone")   ? json.optString("phone")   :
                                                    json.optString("email", "—");

                            String info =
                                    "Name: " + json.optString("name", "—") +
                                            "\nRoll No: " + json.optString("rollNo", "—") +
                                            "\nCourse: " + json.optString("course", "—") +
                                            "\nYear: " + json.optString("year", "—") +
                                            "\nSemester: " + json.optString("semester", "—") +
                                            "\nContact: " + contact;

                            infoText.setText(info);

                        } catch (Exception e) {
                            infoText.setText("Error parsing profile");
                        }
                    } else {
                        infoText.setText("Profile not found");
                    }
                });
            }
        });
    }

    // ================= LOGOUT =================

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}
