package com.campussync.erp.academics;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.firebase.auth.FirebaseAuth;

public class AcademicDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_academics_dashboard);

        String role = AcademicUtils.getRoleFromPrefs(this);

        if ("STUDENT".equals(role)) {
            startActivity(new Intent(this, StudentResultActivity.class));
            finish();
        } else {
            startActivity(new Intent(this, ClassSubjectSelectorActivity.class));
            finish();
        }

    }
}
