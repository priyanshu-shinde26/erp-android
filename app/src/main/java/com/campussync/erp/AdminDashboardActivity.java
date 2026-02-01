package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.assignment.AdminAssignmentActivity;
import com.campussync.erp.attendance.ManageAttendanceActivity;
import com.campussync.erp.lms.TeacherLmsActivity;
import com.campussync.erp.timetable.ManageTimetableActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button attendanceBtn;
    private Button btnAssignments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());

        Button btnManageTimetable = findViewById(R.id.btnManageTimetable);
        btnManageTimetable.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageTimetableActivity.class);

            // ✅ PASS ROLE HERE (so Add button enables)
            intent.putExtra("EXTRA_ROLE", "TEACHER");  // or "ADMIN" if this dashboard is admin

            startActivity(intent);
        });

        attendanceBtn = findViewById(R.id.btn_manage_attendance_admin);
        attendanceBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AdminDashboardActivity.this,
                                ManageAttendanceActivity.class
                        )
                )
        );

        btnAssignments = findViewById(R.id.btn_assignments);
        if (btnAssignments != null) {
            btnAssignments.setOnClickListener(v -> {
                Intent i = new Intent(this, AdminAssignmentActivity.class);
                startActivity(i);
            });
        } else {
            android.util.Log.e("AdminDashboard", "btn_assignments not found in layout");
        }
        // In onCreate():
        Button btnLms = findViewById(R.id.btnLms);
        btnLms.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, TeacherLmsActivity.class);
            startActivity(intent);
        });

        // ACADMIC BUTTON

        Button btnAcademics = findViewById(R.id.btnAcademics);

        btnAcademics.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this,
                    com.campussync.erp.academics.AcademicDashboardActivity.class);
            intent.putExtra("role", "ADMIN");
            startActivity(intent);
        });


    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
