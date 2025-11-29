package com.campussync.erp.timetable;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

public class ManageTimetableActivity extends AppCompatActivity
        implements TimetableManageAdapter.TimetableActionListener {

    private RecyclerView rvManage;
    private TimetableManageAdapter adapter;
    private Button btnAdd;

    private String idToken; // cached Firebase token for this session
    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_timetable);

        rvManage = findViewById(R.id.rvTimetableManage);
        btnAdd = findViewById(R.id.btnAddEntry);

        rvManage.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableManageAdapter(this);
        rvManage.setAdapter(adapter);

        initApiAndLoad();
        btnAdd.setOnClickListener(v -> openAddDialog());
    }

    private void initApiAndLoad() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    idToken = result.getToken();
                    if (idToken == null) {
                        Toast.makeText(this, "Token error", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiService = RetrofitClient.getClient(idToken).create(ApiService.class);
                    loadTimetable();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Token error", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTimetable() {
        apiService.getTimetableAll().enqueue(new Callback<List<TimetableEntry>>() {
            @Override
            public void onResponse(Call<List<TimetableEntry>> call,
                                   Response<List<TimetableEntry>> response) {
                if (response.isSuccessful()) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(ManageTimetableActivity.this,
                            "Load error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TimetableEntry>> call, Throwable t) {
                Log.e("Timetable", "Failed to load", t);
                Toast.makeText(ManageTimetableActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAddDialog() {
        TimetableEntryDialog.show(this, null, entry -> {
            apiService.createTimetableEntry(entry).enqueue(new Callback<TimetableEntry>() {
                @Override
                public void onResponse(Call<TimetableEntry> call,
                                       Response<TimetableEntry> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ManageTimetableActivity.this,
                                "Created", Toast.LENGTH_SHORT).show();
                        loadTimetable();
                    } else {
                        Toast.makeText(ManageTimetableActivity.this,
                                "Create error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TimetableEntry> call, Throwable t) {
                    Toast.makeText(ManageTimetableActivity.this,
                            "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onEditClicked(TimetableEntry entry) {
        TimetableEntryDialog.show(this, entry, updated -> {
            apiService.updateTimetableEntry(entry.getId(), updated)
                    .enqueue(new Callback<TimetableEntry>() {
                        @Override
                        public void onResponse(Call<TimetableEntry> call,
                                               Response<TimetableEntry> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ManageTimetableActivity.this,
                                        "Updated", Toast.LENGTH_SHORT).show();
                                loadTimetable();
                            } else {
                                Toast.makeText(ManageTimetableActivity.this,
                                        "Update error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<TimetableEntry> call, Throwable t) {
                            Toast.makeText(ManageTimetableActivity.this,
                                    "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    public void onDeleteClicked(TimetableEntry entry) {
        apiService.deleteTimetableEntry(entry.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call,
                                           Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ManageTimetableActivity.this,
                                    "Deleted", Toast.LENGTH_SHORT).show();
                            loadTimetable();
                        } else {
                            Toast.makeText(ManageTimetableActivity.this,
                                    "Delete error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(ManageTimetableActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
