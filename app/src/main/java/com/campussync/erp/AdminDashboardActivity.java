package com.campussync.erp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.attendance.ManageAttendanceActivity;   // ✅ important import

public class AdminDashboardActivity extends AppCompatActivity {

    private Button attendanceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        attendanceBtn = findViewById(R.id.btn_manage_attendance_admin);

        attendanceBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                AdminDashboardActivity.this,
                                ManageAttendanceActivity.class   // ✅ use existing class
                        )
                )
        );
    }
}
