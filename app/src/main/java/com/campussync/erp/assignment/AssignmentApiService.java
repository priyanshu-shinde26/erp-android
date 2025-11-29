package com.campussync.erp.assignment;

import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentStatusItem;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit API for talking to the Spring Boot Assignment backend.
 *
 * All methods expect a Firebase ID token as:
 *   Authorization: "Bearer " + idToken
 */
public interface AssignmentApiService {

    // ========= ASSIGNMENTS (LIST / DETAIL) =========

    /**
     * List assignments, optionally filtered by classId.
     * GET /api/assignments?classId=CSE-2
     * If classId is null, backend returns all active assignments.
     */
    @GET("/api/assignments")
    Call<List<AssignmentItem>> getAssignments(
            @Header("Authorization") String authHeader,
            @Query("classId") String classId
    );

    /**
     * Get single assignment by id.
     * GET /api/assignments/{assignmentId}
     */
    @GET("/api/assignments/{assignmentId}")
    Call<AssignmentItem> getAssignmentById(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    // ========= CREATE / UPDATE / DELETE =========

    /**
     * Teacher/Admin: create assignment.
     * POST /api/assignments
     */
    @POST("/api/assignments")
    Call<AssignmentItem> createAssignment(
            @Header("Authorization") String authHeader,
            @Body CreateAssignmentRequest body
    );

    /**
     * Teacher/Admin: update assignment.
     * PUT /api/assignments/{assignmentId}
     */
    @PUT("/api/assignments/{assignmentId}")
    Call<AssignmentItem> updateAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Body UpdateAssignmentRequest body
    );

    /**
     * Teacher/Admin: delete single assignment.
     * DELETE /api/assignments/{assignmentId}
     */
    @DELETE("/api/assignments/{assignmentId}")
    Call<Void> deleteAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    /**
     * Teacher/Admin: bulk delete assignments.
     * DELETE /api/assignments?ids=a&ids=b&ids=c
     */
    @DELETE("/api/assignments")
    Call<Void> deleteAssignmentsBulk(
            @Header("Authorization") String authHeader,
            @Query("ids") List<String> assignmentIds
    );

    // ========= QUESTION PDF (TEACHER) =========

    /**
     * Teacher/Admin: upload or replace question PDF.
     * POST /api/assignments/question/upload?assignmentId=...
     *
     * We send:
     * - assignmentId as a text part
     * - file as multipart PDF
     */
    @Multipart
    @POST("/api/assignments/question/upload")
    Call<AssignmentItem> uploadQuestionPdf(
            @Header("Authorization") String authHeader,
            @Part("assignmentId") RequestBody assignmentIdPart,
            @Part MultipartBody.Part filePart
    );

    // ========= SUBMISSIONS (STUDENT) =========

    /**
     * Student: submit assignment PDF.
     * POST /api/assignments/submissions/upload?assignmentId=...
     */
    @Multipart
    @POST("/api/assignments/submissions/upload")
    Call<AssignmentSubmissionItem> submitAssignmentPdf(
            @Header("Authorization") String authHeader,
            @Part("assignmentId") RequestBody assignmentIdPart,
            @Part MultipartBody.Part filePart
    );

    /**
     * Student: delete own submission.
     * DELETE /api/assignments/{assignmentId}/submissions/{submissionId}
     */
    @DELETE("/api/assignments/{assignmentId}/submissions/{submissionId}")
    Call<Void> deleteOwnSubmission(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Path("submissionId") String submissionId
    );

    // ========= SUBMISSIONS (TEACHER/ADMIN) =========

    /**
     * Teacher/Admin: list all submissions for an assignment.
     * GET /api/assignments/{assignmentId}/submissions
     */
    @GET("/api/assignments/{assignmentId}/submissions")
    Call<List<AssignmentSubmissionItem>> getSubmissionsForAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    /**
     * Teacher/Admin: grade a specific submission.
     * POST /api/assignments/{assignmentId}/submissions/{studentUid}/{submissionId}/grade
     */
    @POST("/api/assignments/{assignmentId}/submissions/{studentUid}/{submissionId}/grade")
    Call<AssignmentSubmissionItem> gradeSubmission(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Path("studentUid") String studentUid,
            @Path("submissionId") String submissionId,
            @Body GradeSubmissionRequest body
    );

    // ========= STATUS (TEACHER/ADMIN) =========

    /**
     * Teacher/Admin: overview of assignment status.
     * GET /api/assignments/{assignmentId}/status
     */
    @GET("/api/assignments/{assignmentId}/status")
    Call<AssignmentStatusItem> getAssignmentStatus(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    // ========= REQUEST DTOs =========

    /**
     * Matches backend CreateAssignmentRequest.
     */
    class CreateAssignmentRequest {
        public String title;
        public String description;
        public String classId;
        public String subject;
        public long dueDate; // epoch millis

        public CreateAssignmentRequest(String title,
                                       String description,
                                       String classId,
                                       String subject,
                                       long dueDate) {
            this.title = title;
            this.description = description;
            this.classId = classId;
            this.subject = subject;
            this.dueDate = dueDate;
        }
    }

    /**
     * Matches backend UpdateAssignmentRequest.
     */
    class UpdateAssignmentRequest {
        public String title;
        public String description;
        public String classId;
        public String subject;
        public long dueDate;

        public UpdateAssignmentRequest(String title,
                                       String description,
                                       String classId,
                                       String subject,
                                       long dueDate) {
            this.title = title;
            this.description = description;
            this.classId = classId;
            this.subject = subject;
            this.dueDate = dueDate;
        }
    }

    /**
     * Matches backend GradeSubmissionRequest.
     */
    class GradeSubmissionRequest {
        public Integer marks;
        public String feedback;

        public GradeSubmissionRequest(Integer marks, String feedback) {
            this.marks = marks;
            this.feedback = feedback;
        }
    }
}
