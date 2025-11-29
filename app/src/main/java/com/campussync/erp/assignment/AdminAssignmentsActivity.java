package com.campussync.erp.assignment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentStatusItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin UI:
 *  - See all assignments (any class)
 *  - See status (submissions / graded / ungraded)
 *  - Multi-select + bulk delete
 */
public class AdminAssignmentsActivity extends AppCompatActivity
        implements AdminAssignmentAdapter.Listener {

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private MaterialButton btnDeleteSelected;

    private AdminAssignmentAdapter adapter;
    private String currentIdToken;

    public static Intent createIntent(AppCompatActivity from) {
        return new Intent(from, AdminAssignmentsActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_assignments);

        toolbar = findViewById(R.id.toolbar_admin_assignments);
        progressBar = findViewById(R.id.progress_admin_assignments);
        tvEmpty = findViewById(R.id.tv_admin_assignments_empty);
        recyclerView = findViewById(R.id.rv_admin_assignments);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new AdminAssignmentAdapter(
                this,
                new ArrayList<>(),
                this
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnDeleteSelected.setOnClickListener(v -> onDeleteSelectedClicked());

        fetchIdTokenAndLoadAssignments();
    }

    private void fetchIdTokenAndLoadAssignments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    currentIdToken = result.getToken();
                    loadAssignments();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAssignments() {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        // classId = null -> all assignments
        api.getAssignments("Bearer " + currentIdToken, null)
                .enqueue(new Callback<List<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<List<AssignmentItem>> call,
                                           Response<List<AssignmentItem>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            List<AssignmentItem> list = response.body();
                            if (list == null || list.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                adapter.setItems(new ArrayList<>());
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                                adapter.setItems(list);
                            }
                        } else {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Failed to load (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<AssignmentItem>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Error: " + t.getMessage());
                    }
                });
    }

    // ===== AdminAssignmentAdapter.Listener =====

    @Override
    public void onStatusRequested(AssignmentItem item) {
        if (currentIdToken == null) return;
        AssignmentApiService api = AssignmentRetrofitClient.getApiService();

        api.getAssignmentStatus("Bearer " + currentIdToken, item.getId())
                .enqueue(new Callback<AssignmentStatusItem>() {
                    @Override
                    public void onResponse(Call<AssignmentStatusItem> call,
                                           Response<AssignmentStatusItem> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setStatusForAssignment(item.getId(), response.body());
                        } else {
                            // Optionally show some fallback text
                            AssignmentStatusItem s = new AssignmentStatusItem();
                            s.setAssignmentId(item.getId());
                            s.setTitle(item.getTitle());
                            s.setClassId(item.getClassId());
                            s.setDueDate(item.getDueDate());
                            s.setTotalSubmissions(0);
                            s.setGradedCount(0);
                            s.setUngradedCount(0);
                            adapter.setStatusForAssignment(item.getId(), s);
                        }
                    }

                    @Override
                    public void onFailure(Call<AssignmentStatusItem> call, Throwable t) {
                        // On failure, set a "0/0" status so UI doesn't keep "loading..."
                        AssignmentStatusItem s = new AssignmentStatusItem();
                        s.setAssignmentId(item.getId());
                        s.setTitle(item.getTitle());
                        s.setClassId(item.getClassId());
                        s.setDueDate(item.getDueDate());
                        s.setTotalSubmissions(0);
                        s.setGradedCount(0);
                        s.setUngradedCount(0);
                        adapter.setStatusForAssignment(item.getId(), s);
                    }
                });
    }

    @Override
    public void onSelectionChanged(List<String> selectedIds) {
        int count = selectedIds.size();
        btnDeleteSelected.setText("Delete Selected (" + count + ")");
        btnDeleteSelected.setEnabled(count > 0);
    }

    // ===== Bulk delete =====

    private void onDeleteSelectedClicked() {
        List<String> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete assignments?")
                .setMessage("Delete " + selected.size() + " selected assignment(s)?")
                .setPositiveButton("Delete", (dialog, which) -> performBulkDelete(selected))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performBulkDelete(List<String> ids) {
        if (currentIdToken == null) {
            Toast.makeText(this, "No auth token", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        AssignmentApiService api = AssignmentRetrofitClient.getApiService();
        api.deleteAssignmentsBulk("Bearer " + currentIdToken, ids)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAssignmentsActivity.this,
                                    "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadAssignments();
                        } else {
                            Toast.makeText(AdminAssignmentsActivity.this,
                                    "Delete failed (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminAssignmentsActivity.this,
                                "Delete error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
