package com.campussync.erp.assignment;

import android.content.Context;

import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentStatusItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AssignmentRepository {
    private Context context;
    private AssignmentApiService apiService;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public AssignmentRepository(Context context) {
        this.context = context;
        this.apiService = AssignmentRetrofitClient.getApiService();
    }

    // 🔥 FIXED: ADMIN uses /api/assignments/admin endpoint
    public void getAllAssignments(Callback<List<AssignmentItem>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not logged in");
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    // 🔥 ADMIN ENDPOINT - NO classId required!
                    apiService.getAllAssignmentsAdmin("Bearer " + token)
                            .enqueue(new retrofit2.Callback<List<AssignmentItem>>() {
                                @Override
                                public void onResponse(retrofit2.Call<List<AssignmentItem>> call,
                                                       retrofit2.Response<List<AssignmentItem>> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        callback.onSuccess(response.body());
                                    } else {
                                        callback.onError("Failed: " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<List<AssignmentItem>> call, Throwable t) {
                                    callback.onError("Network error: " + t.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void getAssignmentById(String assignmentId, Callback<AssignmentItem> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not logged in");
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    apiService.getAssignmentById("Bearer " + token, assignmentId)
                            .enqueue(new retrofit2.Callback<AssignmentItem>() {
                                @Override
                                public void onResponse(retrofit2.Call<AssignmentItem> call,
                                                       retrofit2.Response<AssignmentItem> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        callback.onSuccess(response.body());
                                    } else {
                                        callback.onError("Failed: " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<AssignmentItem> call, Throwable t) {
                                    callback.onError("Network error: " + t.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void getSubmissionsForAssignment(String assignmentId, Callback<List<AssignmentSubmissionItem>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not logged in");
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    apiService.getSubmissionsForAssignment("Bearer " + token, assignmentId)
                            .enqueue(new retrofit2.Callback<List<AssignmentSubmissionItem>>() {
                                @Override
                                public void onResponse(retrofit2.Call<List<AssignmentSubmissionItem>> call,
                                                       retrofit2.Response<List<AssignmentSubmissionItem>> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        callback.onSuccess(response.body());
                                    } else {
                                        callback.onError("Failed: " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<List<AssignmentSubmissionItem>> call, Throwable t) {
                                    callback.onError("Network error: " + t.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    public void deleteAssignments(List<AssignmentItem> assignments, Callback<Boolean> callback) {
        if (assignments == null || assignments.isEmpty()) {
            callback.onSuccess(false);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Not logged in");
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    List<String> ids = new ArrayList<>();
                    for (AssignmentItem item : assignments) {
                        if (item != null && item.getId() != null) ids.add(item.getId());
                    }

                    apiService.deleteAssignmentsBulk("Bearer " + token, ids)
                            .enqueue(new retrofit2.Callback<Void>() {
                                @Override
                                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                    if (response.isSuccessful()) callback.onSuccess(true);
                                    else callback.onError("Delete failed: " + response.code());
                                }

                                @Override
                                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                    callback.onError("Network error: " + t.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> callback.onError("Auth error: " + e.getMessage()));
    }

    // (unchanged placeholder — keep as you had)
    public void gradeSubmission(String submissionId, Integer marks, String feedback, Callback<Boolean> callback) {
        // TODO: Implement grading (needs assignmentId + studentUid + submissionId)
        callback.onSuccess(true);
    }
}
