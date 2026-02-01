package com.campussync.erp.academics;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTestActivity extends AppCompatActivity {

    private EditText etTestName, etMaxMarks, etExamDate;
    private Spinner spinnerTestType;
    private Button btnCreate;

    private String classId;
    private String subjectId;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_test);


        // ================= GET CONTEXT =================
        classId = getIntent().getStringExtra("classId");
        subjectId = getIntent().getStringExtra("subjectId");

        if (classId == null || subjectId == null) {
            Toast.makeText(this, "Invalid class or subject", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ================= INIT UI =================
        etTestName = findViewById(R.id.etTestName);
        etMaxMarks = findViewById(R.id.etMaxMarks);
        etExamDate = findViewById(R.id.etExamDate);
        spinnerTestType = findViewById(R.id.spinnerTestType);
        btnCreate = findViewById(R.id.btnCreateTest);

        // ================= TEST TYPE SPINNER =================
        String[] testTypes = {
                "UNIT_TEST",
                "MID_TERM",
                "FINAL_EXAM",
                "CUSTOM_TEST"
        };

        spinnerTestType.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        testTypes
                )
        );

        // ================= DATE PICKER =================
        etExamDate.setOnClickListener(v -> showDatePicker());

        // ================= CREATE TEST =================
        btnCreate.setOnClickListener(v -> createTest());
    }

    // =====================================================
    // DATE PICKER
    // =====================================================
    private void showDatePicker() {

        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    String date =
                            year + "-" +
                                    String.format("%02d", month + 1) + "-" +
                                    String.format("%02d", day);

                    etExamDate.setText(date);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // =====================================================
    // CREATE TEST API CALL (FIXED)
    // =====================================================
    private void createTest() {

        String testName = etTestName.getText().toString().trim();
        String maxMarksStr = etMaxMarks.getText().toString().trim();
        String examDate = etExamDate.getText().toString().trim();
        String testType = spinnerTestType.getSelectedItem().toString();

        // ---------------- VALIDATION ----------------
        if (testName.isEmpty()) {
            etTestName.setError("Required");
            return;
        }

        if (maxMarksStr.isEmpty()) {
            etMaxMarks.setError("Required");
            return;
        }

        if (examDate.isEmpty()) {
            etExamDate.setError("Select date");
            return;
        }

        int maxMarks;
        try {
            maxMarks = Integer.parseInt(maxMarksStr);
        } catch (Exception e) {
            etMaxMarks.setError("Invalid number");
            return;
        }

        // ---------------- BUILD MODEL ----------------
        TestModel model = new TestModel();
        model.testName = testName;
        model.testType = testType;
        model.maxMarks = maxMarks;
        model.examDate = examDate;

        // ---------------- CORRECT API CALL ----------------
        AcademicRepository
                .getApi((this))
                .createTest(classId, subjectId, model)
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {

                        if (response.isSuccessful()) {
                            Toast.makeText(CreateTestActivity.this,
                                    "Test created successfully",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Log.e("CREATE_TEST",
                                    "Failed | code=" + response.code());
                            Toast.makeText(CreateTestActivity.this,
                                    "Failed (" + response.code() + ")",
                                    Toast.LENGTH_LONG).show();
                        }
                    }



                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(
                                CreateTestActivity.this,
                                "Network error",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
