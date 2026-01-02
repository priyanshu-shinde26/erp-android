package com.campussync.erp.timetable;

import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageTimetableActivity extends AppCompatActivity {

    private TimetableApi timetableApi;

    private RecyclerView rvManage;
    private TimetableManageAdapter adapter;
    private Spinner spinnerClass, spinnerDay;
    private Button btnAdd, btnLoad;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvSelected;

    private static final String EXTRA_ROLE = "EXTRA_ROLE";
    private static final String PREFS_SESSION = "session";
    private static final String KEY_ROLE = "role";

    private String idToken;
    private String selectedClassId, selectedDay;
    private boolean isTeacherAdmin = false;

    private final String[] DAYS = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday","sunday"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_timetable);

        initViews();
        initApiAndLoad();
    }

    private void initViews() {
        rvManage = findViewById(R.id.rvTimetableManage);
        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerDay = findViewById(R.id.spinnerDay);
        btnAdd = findViewById(R.id.btnAddEntry);
        btnLoad = findViewById(R.id.btnLoadSchedule);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSelected = findViewById(R.id.tvSelected);

        rvManage.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TimetableManageAdapter(new TimetableManageAdapter.TimetableActionListener() {
            @Override
            public void onEditClicked(SchedulePeriod period, String periodId) {
                // ✅ FIX: call activity method (no recursion)
                ManageTimetableActivity.this.onEditClicked(period, periodId);
            }

            @Override
            public void onDeleteClicked(SchedulePeriod period, String periodId) {
                // ✅ FIX: call activity method (no recursion)
                ManageTimetableActivity.this.onDeleteClicked(period, periodId);
            }
        });
        rvManage.setAdapter(adapter);

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                DAYS
        );
        spinnerDay.setAdapter(dayAdapter);

        spinnerDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDay = DAYS[position];
                updateSelectedDisplay();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClassId = (spinnerClass.getSelectedItem() != null)
                        ? spinnerClass.getSelectedItem().toString()
                        : null;
                updateSelectedDisplay();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnLoad.setOnClickListener(v -> ensureApiReadyThen(this::loadSelectedDaySchedule));
        btnAdd.setOnClickListener(v -> ensureApiReadyThen(this::openAddDialog));
    }

    private void initApiAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            toast("❌ Not logged in");
            finish();
            return;
        }

        loadClassesFromFirebase();
        checkUserRole();

        user.getIdToken(true).addOnSuccessListener(result -> {
            idToken = result.getToken();
            if (idToken == null || idToken.isEmpty()) {
                toast("❌ Token empty");
                return;
            }

            timetableApi = RetrofitClient.getClient().create(TimetableApi.class);
            toast("✅ API ready");
        }).addOnFailureListener(e -> toast("❌ Token error: " + e.getMessage()));
    }

    private void ensureApiReadyThen(Runnable action) {
        if (timetableApi != null && idToken != null && !idToken.isEmpty()) {
            action.run();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            toast("❌ Not logged in");
            return;
        }

        toast("⏳ Preparing API...");
        user.getIdToken(true).addOnSuccessListener(result -> {
            idToken = result.getToken();
            if (idToken == null || idToken.isEmpty()) {
                toast("❌ Token empty");
                return;
            }
            timetableApi = RetrofitClient.getClient().create(TimetableApi.class);
            action.run();
        }).addOnFailureListener(e -> toast("❌ Token error: " + e.getMessage()));
    }

    private String bearerToken() {
        return "Bearer " + (idToken != null ? idToken : "");
    }

    private void loadClassesFromFirebase() {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<String> unique = new HashSet<>();

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String classId = studentSnap.child("classId").getValue(String.class);
                    if (classId != null && !classId.trim().isEmpty()) unique.add(classId.trim());
                }

                List<String> classes = new ArrayList<>(unique);
                Collections.sort(classes);

                ArrayAdapter<String> ad = new ArrayAdapter<>(
                        ManageTimetableActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        classes
                );
                spinnerClass.setAdapter(ad);

                if (!classes.isEmpty()) spinnerClass.setSelection(0);
                else toast("⚠️ No classId found in students");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                toast("❌ Firebase error: " + error.getMessage());
            }
        });
    }

    private void checkUserRole() {
        String role = getIntent() != null ? getIntent().getStringExtra(EXTRA_ROLE) : null;
        if (role == null || role.trim().isEmpty()) {
            role = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE).getString(KEY_ROLE, "");
        }
        role = role == null ? "" : role.trim();

        isTeacherAdmin = role.equalsIgnoreCase("TEACHER") || role.equalsIgnoreCase("ADMIN");
        btnAdd.setEnabled(isTeacherAdmin);
        adapter.setEditable(isTeacherAdmin);

        tvEmpty.setText(isTeacherAdmin
                ? "👆 Select class + day → Load schedule"
                : "📚 Read-only. Contact admin for changes");
    }

    private void loadSelectedDaySchedule() {
        if (selectedClassId == null || selectedClassId.trim().isEmpty() || selectedDay == null) {
            toast("Select class & day first");
            return;
        }

        showLoading(true);
        Log.d("LOAD_DEBUG", "Loading: " + selectedClassId + "/" + selectedDay);

        timetableApi.getDaySchedule(bearerToken(), selectedClassId, selectedDay)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        showLoading(false);

                        if (!response.isSuccessful()) {
                            toast("Load failed: " + response.code());
                            setEmptyState(true, "Load failed: " + response.code());
                            return;
                        }

                        List<Map<String, Object>> jsonList = response.body();
                        if (jsonList == null) jsonList = new ArrayList<>();

                        List<SchedulePeriod> periods = new ArrayList<>();
                        for (Map<String, Object> json : jsonList) {
                            SchedulePeriod p = new SchedulePeriod();
                            p.id = asString(json.get("id"));
                            p.subject = asString(json.get("subject"));
                            p.startTime = asString(json.get("startTime"));
                            p.endTime = asString(json.get("endTime"));
                            p.teacher = asString(json.get("teacher"));
                            p.room = asString(json.get("room"));
                            periods.add(p);
                        }

                        // ✅ IMPORTANT: give day to adapter so tvDay shows in each card
                        adapter.setDay(selectedDay);

                        // ✅ set data
                        adapter.setItems(periods);

                        // ✅ show/hide list
                        boolean empty = periods.isEmpty();
                        setEmptyState(empty, empty ? ("📭 No periods for " + capitalize(selectedDay)) : "");
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        showLoading(false);
                        toast("Network error: " + t.getMessage());
                        setEmptyState(true, "🌐 Network error");
                    }
                });
    }

    private String getTodayKey() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.MONDAY:    return "monday";
            case Calendar.TUESDAY:   return "tuesday";
            case Calendar.WEDNESDAY: return "wednesday";
            case Calendar.THURSDAY:  return "thursday";
            case Calendar.FRIDAY:    return "friday";
            case Calendar.SATURDAY:  return "saturday";
            case Calendar.SUNDAY:    return "sunday";
            default: return "monday";
        }
    }

    private void openAddDialog() {
        if (!isTeacherAdmin) {
            toast("Only Teacher/Admin can add");
            return;
        }

        if (selectedClassId == null || selectedClassId.trim().isEmpty()) {
            toast("Select class first");
            return;
        }

        // ✅ Use TODAY automatically instead of selectedDay
        String todayDay = getTodayKey();
        toast("📅 Adding to " + capitalize(todayDay));

        TimetableEntryDialog.showAdd(this, selectedClassId, todayDay, period -> {
            // ✅ Convert SchedulePeriod -> Map for backend
            Map<String, Object> body = new HashMap<>();
            body.put("subject", period.subject != null ? period.subject : "");
            body.put("startTime", period.startTime != null ? period.startTime : "");
            body.put("endTime", period.endTime != null ? period.endTime : "");
            body.put("teacher", period.teacher != null ? period.teacher : "");
            body.put("room", period.room != null ? period.room : "");

            timetableApi.createPeriod(bearerToken(), selectedClassId, todayDay, body)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                toast("✅ Added to " + capitalize(todayDay));
                                // ✅ Switch to today tab and refresh
                                spinnerDay.setSelection(getDayIndex(todayDay));
                                selectedDay = todayDay;
                                loadSelectedDaySchedule();
                            } else {
                                String err = "";
                                try {
                                    if (response.errorBody() != null) err = response.errorBody().string();
                                } catch (IOException ignored) {}
                                toast("❌ Add failed: " + response.code() + (err.isEmpty() ? "" : (" " + err)));
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            toast("Network: " + t.getMessage());
                        }
                    });
        });
    }

    private int getDayIndex(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) return i;
        }
        return 0;
    }

    private void onEditClicked(SchedulePeriod period, String periodId) {
        if (!isTeacherAdmin) {
            toast("Only Teacher/Admin can edit");
            return;
        }

        // ✅ Open dialog in "Edit" mode with existing data
        TimetableEntryDialog.showEdit(this, selectedClassId, selectedDay, period, updatedPeriod -> {

            // Build the request body Map
            Map<String, Object> body = new HashMap<>();
            body.put("subject", updatedPeriod.subject != null ? updatedPeriod.subject : "");
            body.put("startTime", updatedPeriod.startTime != null ? updatedPeriod.startTime : "");
            body.put("endTime", updatedPeriod.endTime != null ? updatedPeriod.endTime : "");
            body.put("teacher", updatedPeriod.teacher != null ? updatedPeriod.teacher : "");
            body.put("room", updatedPeriod.room != null ? updatedPeriod.room : "");

            // ✅ Call the update API
            timetableApi.updatePeriod(bearerToken(), selectedClassId, selectedDay, periodId, body)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                toast("✅ Updated successfully");
                                loadSelectedDaySchedule(); // Refresh the list
                            } else {
                                toast("❌ Update failed: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            toast("🌐 Network error: " + t.getMessage());
                        }
                    });
        });
    }

    private void onDeleteClicked(SchedulePeriod period, String periodId) {
        if (!isTeacherAdmin) {
            toast("Only Teacher/Admin can delete");
            return;
        }
        if (periodId == null || periodId.trim().isEmpty()) {
            toast("❌ periodId missing (cannot delete)");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Period?")
                .setMessage(period.subject + " (" + period.getDisplayTime() + ")")
                .setPositiveButton("Delete", (dialog, which) -> {
                    timetableApi.deletePeriod(bearerToken(), selectedClassId, selectedDay, periodId)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        toast("🗑️ Deleted");
                                        loadSelectedDaySchedule();
                                    } else {
                                        toast("❌ Delete failed: " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    toast("Network error: " + t.getMessage());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSelectedDisplay() {
        if (selectedClassId != null && selectedDay != null) {
            tvSelected.setText("👥 " + selectedClassId + " • 📅 " + capitalize(selectedDay));
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvManage.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void setEmptyState(boolean empty, String msg) {
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvManage.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String asString(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
