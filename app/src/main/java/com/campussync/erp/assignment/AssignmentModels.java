package com.campussync.erp.assignment;

/**
 * Shared models for the Assignment module.
 * These match the JSON shapes from your Spring Boot backend.
 *
 * You can use them with Retrofit + Gson/Moshi directly.
 */
public class AssignmentModels {

    // ===== Assignment item (matches backend Assignment) =====
    public static class AssignmentItem {
        private String id;
        private String title;
        private String description;
        private String classId;
        private String subject;
        private long createdAt;
        private long dueDate;
        private String createdByUid;
        private boolean active;
        private String questionFileUrl;
        private String questionFilePublicId;

        public AssignmentItem() {
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getClassId() {
            return classId;
        }

        public String getSubject() {
            return subject;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getDueDate() {
            return dueDate;
        }

        public String getCreatedByUid() {
            return createdByUid;
        }

        public boolean isActive() {
            return active;
        }

        public String getQuestionFileUrl() {
            return questionFileUrl;
        }

        public String getQuestionFilePublicId() {
            return questionFilePublicId;
        }

        // Optional setters if you ever need to modify locally
        public void setId(String id) {
            this.id = id;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setClassId(String classId) {
            this.classId = classId;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public void setDueDate(long dueDate) {
            this.dueDate = dueDate;
        }

        public void setCreatedByUid(String createdByUid) {
            this.createdByUid = createdByUid;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setQuestionFileUrl(String questionFileUrl) {
            this.questionFileUrl = questionFileUrl;
        }

        public void setQuestionFilePublicId(String questionFilePublicId) {
            this.questionFilePublicId = questionFilePublicId;
        }
    }

    // ===== Assignment submission item (matches backend AssignmentSubmission) =====
    public static class AssignmentSubmissionItem {
        private String id;
        private String assignmentId;
        private String studentUid;
        private long submittedAt;
        private String fileUrl;
        private String filePublicId;
        private boolean deleted;

        private Integer marks;
        private String feedback;
        private String gradedByUid;
        private Long gradedAt;

        public AssignmentSubmissionItem() {
        }

        public String getId() {
            return id;
        }

        public String getAssignmentId() {
            return assignmentId;
        }

        public String getStudentUid() {
            return studentUid;
        }

        public long getSubmittedAt() {
            return submittedAt;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getFilePublicId() {
            return filePublicId;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public Integer getMarks() {
            return marks;
        }

        public String getFeedback() {
            return feedback;
        }

        public String getGradedByUid() {
            return gradedByUid;
        }

        public Long getGradedAt() {
            return gradedAt;
        }

        // Optional setters
        public void setId(String id) {
            this.id = id;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }

        public void setStudentUid(String studentUid) {
            this.studentUid = studentUid;
        }

        public void setSubmittedAt(long submittedAt) {
            this.submittedAt = submittedAt;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public void setFilePublicId(String filePublicId) {
            this.filePublicId = filePublicId;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public void setMarks(Integer marks) {
            this.marks = marks;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public void setGradedByUid(String gradedByUid) {
            this.gradedByUid = gradedByUid;
        }

        public void setGradedAt(Long gradedAt) {
            this.gradedAt = gradedAt;
        }
    }

    // ===== Assignment status overview (matches AssignmentStatusDto) =====
    public static class AssignmentStatusItem {
        private String assignmentId;
        private String title;
        private String classId;
        private long dueDate;

        private int totalSubmissions;
        private int distinctStudentsSubmitted;
        private int gradedCount;
        private int ungradedCount;

        public AssignmentStatusItem() {
        }

        public String getAssignmentId() {
            return assignmentId;
        }

        public String getTitle() {
            return title;
        }

        public String getClassId() {
            return classId;
        }

        public long getDueDate() {
            return dueDate;
        }

        public int getTotalSubmissions() {
            return totalSubmissions;
        }

        public int getDistinctStudentsSubmitted() {
            return distinctStudentsSubmitted;
        }

        public int getGradedCount() {
            return gradedCount;
        }

        public int getUngradedCount() {
            return ungradedCount;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setClassId(String classId) {
            this.classId = classId;
        }

        public void setDueDate(long dueDate) {
            this.dueDate = dueDate;
        }

        public void setTotalSubmissions(int totalSubmissions) {
            this.totalSubmissions = totalSubmissions;
        }

        public void setDistinctStudentsSubmitted(int distinctStudentsSubmitted) {
            this.distinctStudentsSubmitted = distinctStudentsSubmitted;
        }

        public void setGradedCount(int gradedCount) {
            this.gradedCount = gradedCount;
        }

        public void setUngradedCount(int ungradedCount) {
            this.ungradedCount = ungradedCount;
        }
    }
}
