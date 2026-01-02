package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.assignment.*;
import com.campussync.erp.assignment.AdminAssignmentActivity;
import com.campussync.erp.attendance.ManageAttendanceActivity;
import com.campussync.erp.timetable.ManageTimetableActivity;
import com.google.firebase.auth.FirebaseAuth;

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
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
