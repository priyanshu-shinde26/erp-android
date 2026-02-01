package com.campussync.erp.academics;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassSubjectSelectorActivity extends AppCompatActivity {

    private Spinner spinnerClass, spinnerSubject;
    private ArrayAdapter<String> classAdapter, subjectAdapter;

    private final List<String> classList = new ArrayList<>();
    private final List<String> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_class_subject_selector);


        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        Button btnContinue = findViewById(R.id.btnContinue);

        classAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                classList
        );
        spinnerClass.setAdapter(classAdapter);

        subjectAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                subjectList
        );
        spinnerSubject.setAdapter(subjectAdapter);

        fetchClassesFromFirebase();

        // 🔹 Load subjects when class changes
        spinnerClass.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedClassId = classList.get(position);
                fetchSubjectsForClass(selectedClassId);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnContinue.setOnClickListener(v -> {
            if (spinnerClass.getSelectedItem() == null || spinnerSubject.getSelectedItem() == null) return;

            String classId = spinnerClass.getSelectedItem().toString();
            String subjectId = spinnerSubject.getSelectedItem().toString();

            Intent i = new Intent(this, TestListActivity.class);
            i.putExtra("classId", classId);
            i.putExtra("subjectId", subjectId);
            startActivity(i);
        });
    }

    // ===============================
    // FETCH CLASSES
    // ===============================
    private void fetchClassesFromFirebase() {

        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("students");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Set<String> classSet = new HashSet<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String classId = ds.child("classId").getValue(String.class);
                    if (classId != null) {
                        classSet.add(classId);
                    }
                }

                classList.clear();
                classList.addAll(classSet);
                classAdapter.notifyDataSetChanged();

                Log.d("CLASS_FETCH", "Classes=" + classList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CLASS_FETCH", error.getMessage());
            }
        });
    }

    // ===============================
    // FETCH SUBJECTS BY CLASS
    // ===============================
    private void fetchSubjectsForClass(String classId) {

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("subjects")
                        .child(classId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                subjectList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    // key = maths / science
                    subjectList.add(ds.getKey());
                }

                subjectAdapter.notifyDataSetChanged();

                Log.d("SUBJECT_FETCH", "Subjects=" + subjectList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SUBJECT_FETCH", error.getMessage());
            }
        });
    }
}
