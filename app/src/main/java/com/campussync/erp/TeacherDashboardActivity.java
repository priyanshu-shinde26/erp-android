package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.attendance.ManageAttendanceActivity;  // ✅
import com.campussync.erp.timetable.ManageTimetableActivity;
import com.google.firebase.auth.FirebaseAuth;

public class TeacherDashboardActivity extends AppCompatActivity {

    private Button attendanceBtn;
    private Button btnAssignments;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);
        Button btnManageTimetable = findViewById(R.id.btnManageTimetable);
        btnManageTimetable.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, ManageTimetableActivity.class);
            startActivity(intent);
        });

        attendanceBtn = findViewById(R.id.btn_manage_attendance);
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());

        attendanceBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                TeacherDashboardActivity.this,
                                ManageAttendanceActivity.class   // ✅
                        )
                )
        );
        Button btnAssignments = findViewById(R.id.btn_assignments);
        if (btnAssignments != null) {
            btnAssignments.setOnClickListener(v -> {
                Intent i = new Intent(this, com.campussync.erp.assignment.TeacherAssignmentsActivity.class);
                startActivity(i);
            });
        } else {
            // Helpful log if something is wrong with the layout again
            android.util.Log.e("TeacherDashboard", "btn_assignments not found in layout");
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
