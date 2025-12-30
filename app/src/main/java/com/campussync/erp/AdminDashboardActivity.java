package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.assignment.*;
// In AdminDashboardActivity.java
import com.campussync.erp.assignment.AdminAssignmentActivity;  // ✅ FIXED

// Start activity
import com.campussync.erp.attendance.ManageAttendanceActivity;   // ✅ important import
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
            startActivity(intent);
        });

        attendanceBtn = findViewById(R.id.btn_manage_attendance_admin);

        attendanceBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AdminDashboardActivity.this,
                                ManageAttendanceActivity.class   // ✅ use existing class
                        )
                )
        );
        Button btnAssignments = findViewById(R.id.btn_assignments);
        if (btnAssignments != null) {
            btnAssignments.setOnClickListener(v -> {
                Intent i = new Intent(this, AdminAssignmentActivity.class);  // singular

                startActivity(i);
            });
        } else {
            // Helpful log if something is wrong with the layout again
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
