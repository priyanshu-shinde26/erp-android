package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.attendance.ManageAttendanceActivity;  // ✅

public class TeacherDashboardActivity extends AppCompatActivity {

    private Button attendanceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        attendanceBtn = findViewById(R.id.btn_manage_attendance);

        attendanceBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                TeacherDashboardActivity.this,
                                ManageAttendanceActivity.class   // ✅
                        )
                )
        );
    }
}
