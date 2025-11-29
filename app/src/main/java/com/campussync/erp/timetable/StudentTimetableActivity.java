package com.campussync.erp.timetable;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.ApiService;
import com.campussync.erp.R;
import com.campussync.erp.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentTimetableActivity extends AppCompatActivity {

    private RecyclerView rvTimetable;
    private TimetableViewAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        rvTimetable = findViewById(R.id.rvTimetable);
        rvTimetable.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableViewAdapter();
        rvTimetable.setAdapter(adapter);

        loadTimetable();
    }

    private void loadTimetable() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    if (token == null) {
                        Toast.makeText(this, "Failed to get token", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiService api = RetrofitClient.getClient(token)
                            .create(ApiService.class);

                    api.getTimetableAll().enqueue(new Callback<List<TimetableEntry>>() {
                        @Override
                        public void onResponse(Call<List<TimetableEntry>> call,
                                               Response<List<TimetableEntry>> response) {
                            if (response.isSuccessful()) {
                                adapter.setItems(response.body());
                            } else {
                                Toast.makeText(StudentTimetableActivity.this,
                                        "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<TimetableEntry>> call, Throwable t) {
                            Log.e("Timetable", "Failed to load", t);
                            Toast.makeText(StudentTimetableActivity.this,
                                    "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Token error", Toast.LENGTH_SHORT).show();
                });
    }
}
