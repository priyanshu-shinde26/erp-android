package com.campussync.erp.timetable;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    private RecyclerView rvTimetable;
    private TimetableViewAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvTitle, tvClassToday, tvEmpty;

    private String idToken;
    private String myClassId;
    private String todayDay;
    private TimetableApi timetableApi; // ✅ Single API instance

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        initViews();
        loadMyClassAndToday();
    }

    private void initViews() {
        rvTimetable = findViewById(R.id.rvTimetable);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        tvClassToday = findViewById(R.id.tvClassToday);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvTimetable.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableViewAdapter();
        rvTimetable.setAdapter(adapter);
    }

    private void loadMyClassAndToday() {
        showLoading(true);

        // Get today day name
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        todayDay = sdf.format(new Date()).toLowerCase();

        // Get my classId from Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("students").child(FirebaseAuth.getInstance().getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                myClassId = snapshot.child("classId").getValue(String.class);
                if (myClassId == null) {
                    showLoading(false);
                    tvEmpty.setText("⚠️ No class assigned. Contact admin.");
                    return;
                }

                tvClassToday.setText("👥 " + myClassId + " • 📅 Today (" + todayDay + ")");
                loadIdTokenAndSchedule();
            }

            @Override public void onCancelled(DatabaseError error) {
                showLoading(false);
                toast("Error loading profile");
            }
        });
    }

    private void loadIdTokenAndSchedule() {
        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(true).addOnSuccessListener(result -> {
                    idToken = result.getToken();
                    if (idToken == null || idToken.isEmpty()) {
                        showLoading(false);
                        toast("❌ Invalid token");
                        return;
                    }

                    Log.d("STUDENT_TT", "✅ Token length=" + idToken.length());

                    // ✅ Single RetrofitClient (no auth param) [web:318]
                    timetableApi = RetrofitClient.getClient().create(TimetableApi.class);
                    loadTodaySchedule();
                }).addOnFailureListener(e -> {
                    showLoading(false);
                    toast("Token error: " + e.getMessage());
                });
    }

    /** ✅ Helper: Bearer + idToken */
    private String bearerToken() {
        return "Bearer " + (idToken != null ? idToken : "");
    }

    private void loadTodaySchedule() {
        timetableApi.getDaySchedule(bearerToken(), myClassId, todayDay)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        showLoading(false);

                        if (response.isSuccessful()) {
                            List<Map<String, Object>> jsonList = response.body();
                            if (jsonList == null) {
                                jsonList = new ArrayList<>();
                            }

                            // ✅ Convert JSON → SchedulePeriod list
                            List<SchedulePeriod> schedule = new ArrayList<>();
                            for (Map<String, Object> json : jsonList) {
                                SchedulePeriod p = new SchedulePeriod();
                                p.id = safeString(json.get("id"));
                                p.subject = safeString(json.get("subject"));
                                p.startTime = safeString(json.get("startTime"));
                                p.endTime = safeString(json.get("endTime"));
                                p.teacher = safeString(json.get("teacher"));
                                p.room = safeString(json.get("room"));
                                schedule.add(p);
                            }

                            // ✅ Pass day to adapter for display
                            adapter.setDay(todayDay);
                            adapter.setItems(schedule);

                            // ✅ Update UI
                            boolean isEmpty = schedule.isEmpty();
                            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                            tvEmpty.setText(isEmpty
                                    ? "📭 No classes today (" + capitalize(todayDay) + ")"
                                    : "");

                            tvClassToday.setText("👥 " + myClassId + " • 📅 " + capitalize(todayDay));

                        } else {
                            tvEmpty.setText("📡 Load failed (" + response.code() + ")");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        showLoading(false);
                        tvEmpty.setText("🌐 Network error: " + t.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ✅ Helper methods
    private String safeString(Object obj) {
        return obj instanceof String ? (String) obj : "";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvTimetable.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
