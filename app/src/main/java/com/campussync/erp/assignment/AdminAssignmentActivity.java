package com.campussync.erp.assignment;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AdminAssignmentActivity extends AppCompatActivity {
    private RecyclerView rvAssignments;
    private AdminAssignmentAdapter adapter;
    private ProgressBar progressBar;
    private Button btnDeleteSelected, btnRefresh;
    private TextView tvEmptyState;
    private AssignmentRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_assignment);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadAssignments();
    }

    private void initViews() {
        rvAssignments = findViewById(R.id.rv_assignments);
        progressBar = findViewById(R.id.progress_bar);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnRefresh = findViewById(R.id.btn_refresh);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        repository = new AssignmentRepository(this);

        adapter = new AdminAssignmentAdapter();

        // 🔥 FIXED: Listen for selection changes from adapter
        adapter.setOnSelectionChangedListener(count -> updateDeleteButton());

        adapter.setOnItemActionListener(new AdminAssignmentAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(AssignmentItem item) {
                updateDeleteButton();
            }

            @Override
            public void onBulkDelete(List<AssignmentItem> selected) {
                deleteSelectedAssignments(selected);
            }
        });

        btnDeleteSelected.setOnClickListener(v -> deleteSelectedAssignments(adapter.getSelectedItems()));
        btnRefresh.setOnClickListener(v -> loadAssignments());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("👨‍🏫 Manage Assignments");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        rvAssignments.setLayoutManager(new LinearLayoutManager(this));
        rvAssignments.setAdapter(adapter);
    }

    // 🔥 FIXED: Admin loads ALL assignments (no class filter)
    private void loadAssignments() {
        showLoading(true);
        tvEmptyState.setText("🔄 Loading assignments...");

        repository.getAllAssignments(new AssignmentRepository.Callback<List<AssignmentItem>>() {
            @Override
            public void onSuccess(List<AssignmentItem> assignments) {
                showLoading(false);
                if (assignments == null) assignments = new ArrayList<>();

                adapter.setAssignments(assignments);
                updateDeleteButton(); // 🔥 Refresh button state

                if (assignments.isEmpty()) {
                    tvEmptyState.setText("📭 No assignments found\nTap refresh to load latest");
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                tvEmptyState.setText("❌ Error: " + error + "\nTap refresh to retry");
                tvEmptyState.setVisibility(View.VISIBLE);
                Snackbar.make(rvAssignments, "Failed to load: " + error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void deleteSelectedAssignments(List<AssignmentItem> selected) {
        if (selected.isEmpty()) {
            Snackbar.make(rvAssignments, "No assignments selected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("🗑️ Delete Assignments")
                .setMessage("Delete " + selected.size() + " assignment(s)? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    showLoading(true);
                    repository.deleteAssignments(selected, new AssignmentRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            showLoading(false);
                            Snackbar.make(rvAssignments, "✅ Deleted " + selected.size() + " assignments", Snackbar.LENGTH_LONG).show();
                            adapter.clearSelection();
                            loadAssignments();
                        }

                        @Override
                        public void onError(String error) {
                            showLoading(false);
                            Snackbar.make(rvAssignments, "❌ Delete failed: " + error, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 🔥 FIXED: Update button text + enable/disable based on selection
    private void updateDeleteButton() {
        List<AssignmentItem> selected = adapter.getSelectedItems();
        int count = selected.size();
        btnDeleteSelected.setText("🗑️ Delete " + count + " Selected");
        btnDeleteSelected.setEnabled(count > 0);

        // 🔥 Show selection count in button
        if (count > 0) {
            btnDeleteSelected.setAlpha(1.0f);
        } else {
            btnDeleteSelected.setAlpha(0.5f);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvAssignments.setVisibility(show ? View.GONE : View.VISIBLE);
        btnDeleteSelected.setEnabled(!show);
        btnRefresh.setEnabled(!show);
        tvEmptyState.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
