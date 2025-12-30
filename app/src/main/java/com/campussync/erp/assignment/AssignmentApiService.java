package com.campussync.erp.assignment;

import com.campussync.erp.assignment.AssignmentModels.AssignmentItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentStatusItem;
import com.campussync.erp.assignment.AssignmentModels.AssignmentSubmissionItem;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface AssignmentApiService {

    // ========= ASSIGNMENTS (LIST / DETAIL) =========

    @GET("/api/assignments")
    Call<List<AssignmentItem>> getAssignments(
            @Header("Authorization") String authHeader,
            @Query("classId") String classId // pass null to fetch all (query omitted) [web:101]
    );

    // 🔥 NEW: ADMIN ONLY - ALL assignments
    @GET("/api/assignments/admin")
    Call<List<AssignmentItem>> getAllAssignmentsAdmin(
            @Header("Authorization") String authHeader
    );

    @GET("/api/assignments/{assignmentId}")
    Call<AssignmentItem> getAssignmentById(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    // ========= CREATE / UPDATE / DELETE =========

    @POST("/api/assignments")
    Call<AssignmentItem> createAssignment(
            @Header("Authorization") String authHeader,
            @Body CreateAssignmentRequest body
    );

    @PUT("/api/assignments/{assignmentId}")
    Call<AssignmentItem> updateAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Body UpdateAssignmentRequest body
    );

    @DELETE("/api/assignments/{assignmentId}")
    Call<Void> deleteAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    @DELETE("/api/assignments")
    Call<Void> deleteAssignmentsBulk(
            @Header("Authorization") String authHeader,
            @Query("ids") List<String> assignmentIds
    );

    // ========= QUESTION PDF (TEACHER) =========

    @Multipart
    @POST("/api/assignments/question/upload")
    Call<AssignmentItem> uploadQuestionPdf(
            @Header("Authorization") String authHeader,
            @Part("assignmentId") RequestBody assignmentIdPart,
            @Part MultipartBody.Part filePart
    );

    // ========= SUBMISSIONS (STUDENT) =========

    @Multipart
    @POST("/api/assignments/submissions/upload")
    Call<AssignmentSubmissionItem> submitAssignmentPdf(
            @Header("Authorization") String authHeader,
            @Part("assignmentId") RequestBody assignmentIdPart,
            @Part MultipartBody.Part filePart
    );

    @DELETE("/api/assignments/{assignmentId}/submissions/{submissionId}")
    Call<Void> deleteOwnSubmission(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Path("submissionId") String submissionId
    );

    // ========= SUBMISSIONS (TEACHER/ADMIN) =========

    @GET("/api/assignments/{assignmentId}/submissions")
    Call<List<AssignmentSubmissionItem>> getSubmissionsForAssignment(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    @POST("/api/assignments/{assignmentId}/submissions/{studentUid}/{submissionId}/grade")
    Call<AssignmentSubmissionItem> gradeSubmission(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId,
            @Path("studentUid") String studentUid,
            @Path("submissionId") String submissionId,
            @Body GradeSubmissionRequest body
    );

    // ========= STATUS (TEACHER/ADMIN) =========

    @GET("/api/assignments/{assignmentId}/status")
    Call<AssignmentStatusItem> getAssignmentStatus(
            @Header("Authorization") String authHeader,
            @Path("assignmentId") String assignmentId
    );

    // ========= REQUEST DTOs =========

    class CreateAssignmentRequest {
        public String title, description, classId, subject;
        public long dueDate;

        public CreateAssignmentRequest(String title, String description, String classId, String subject, long dueDate) {
            this.title = title; this.description = description; this.classId = classId; this.subject = subject; this.dueDate = dueDate;
        }
    }

    class UpdateAssignmentRequest {
        public String title, description, classId, subject;
        public long dueDate;

        public UpdateAssignmentRequest(String title, String description, String classId, String subject, long dueDate) {
            this.title = title; this.description = description; this.classId = classId; this.subject = subject; this.dueDate = dueDate;
        }
    }

    class GradeSubmissionRequest {
        public Integer marks;
        public String feedback;

        public GradeSubmissionRequest(Integer marks, String feedback) {
            this.marks = marks; this.feedback = feedback;
        }
    }
}
