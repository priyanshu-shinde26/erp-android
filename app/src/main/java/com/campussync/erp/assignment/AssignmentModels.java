package com.campussync.erp.assignment;
import android.text.format.DateFormat;
import com.google.gson.annotations.SerializedName;

public class AssignmentModels {

    // ===== ASSIGNMENT ITEM =====

 // ✅ Single import

    public static class AssignmentItem {
        @SerializedName("id")
        private String id;

        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("classId")
        private String classId;

        @SerializedName("subject")
        private String subject;

        @SerializedName("createdAt")
        private long createdAt;

        @SerializedName("dueDate")
        private long dueDate;

        @SerializedName("createdByUid")
        private String createdByUid;

        @SerializedName("active")
        private boolean active;

        @SerializedName("questionFileUrl")
        private String questionFileUrl;

        @SerializedName("questionFilePublicId")
        private String questionFilePublicId;

        @SerializedName("marks")
        private Integer marks;

        @SerializedName("feedback")
        private String feedback;

        @SerializedName("totalMarks")
        public double totalMarks;

        // 🔥 STATUS FIELDS (populated by adapters)
        public int totalSubmissions;
        public int gradedCount;
        public int pendingCount;
        public boolean hasQuestionPdf;

        public AssignmentItem() {}

        // 🔥 COMPLETE GETTERS - 100% WORKING
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getClassId() { return classId; }
        public String getClassName() { return classId; }
        public String getSubject() { return subject; }
        public long getDueDate() { return dueDate; }
        public long getCreatedAt() { return createdAt; }
        public String getCreatedByUid() { return createdByUid; }
        public boolean isActive() { return active; }
        public String getQuestionFileUrl() { return questionFileUrl; }
        public String getQuestionFilePublicId() { return questionFilePublicId; }
        public Integer getMarks() { return marks; }
        public String getFeedback() { return feedback; }
        public double getTotalMarks() { return totalMarks; }
        public boolean isHasQuestionPdf() { return hasQuestionPdf; }

        // 🔥 FIXED FORMATTING - NO ERRORS
        public String getDueDateFormatted() {
            return DateFormat.format("dd MMM yyyy", new java.util.Date(dueDate)).toString();
        }

        public String getDueDateTimeFormatted() {
            return DateFormat.format("dd MMM yyyy, hh:mm a", new java.util.Date(dueDate)).toString();
        }

        public String getStatusSummary() {
            return String.format("📊 %d total, %d graded, %d pending",
                    totalSubmissions, gradedCount, pendingCount);
        }

        // 🔥 HELPER METHODS
        public boolean isDueSoon() {
            long now = System.currentTimeMillis();
            long threeDays = 3 * 24 * 60 * 60 * 1000L;
            return dueDate > now && dueDate < (now + threeDays);
        }

        public boolean isOverdue() {
            return System.currentTimeMillis() > dueDate;
        }

        public String getDueStatus() {
            if (isOverdue()) return "🔴 Overdue";
            if (isDueSoon()) return "⚡ Due Soon";
            return "🟢 Active";
        }

        // 🔥 SETTERS FOR JSON
        public void setId(String id) { this.id = id; }
        public void setTitle(String title) { this.title = title; }
        public void setClassId(String classId) { this.classId = classId; }
        public void setSubject(String subject) { this.subject = subject; }
        public void setDueDate(long dueDate) { this.dueDate = dueDate; }
        public void setQuestionFileUrl(String questionFileUrl) { this.questionFileUrl = questionFileUrl; }
        public void setMarks(Integer marks) { this.marks = marks; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    // ===== ASSIGNMENT STATUS =====
    public static class AssignmentStatusItem {
        @SerializedName("assignmentId")
        private String assignmentId;

        @SerializedName("title")
        private String title;

        @SerializedName("classId")
        private String classId;

        @SerializedName("dueDate")
        private long dueDate;

        @SerializedName("totalSubmissions")
        private int totalSubmissions;

        @SerializedName("distinctStudentsSubmitted")
        private int distinctStudentsSubmitted;

        @SerializedName("gradedCount")
        private int gradedCount;

        @SerializedName("ungradedCount")
        private int ungradedCount;

        public AssignmentStatusItem() {}

        public String getAssignmentId() { return assignmentId; }
        public String getTitle() { return title; }
        public String getClassId() { return classId; }
        public long getDueDate() { return dueDate; }
        public int getTotalSubmissions() { return totalSubmissions; }
        public int getDistinctStudentsSubmitted() { return distinctStudentsSubmitted; }
        public int getGradedCount() { return gradedCount; }
        public int getPendingCount() { return ungradedCount; }

        public String getDueDateFormatted() {
            return java.text.DateFormat.getDateInstance().format(new java.util.Date(dueDate));
        }
    }

    // ===== ASSIGNMENT SUBMISSION =====
    // ===== ASSIGNMENT SUBMISSION ===== (REPLACE ENTIRE CLASS)
    public static class AssignmentSubmissionItem {  // 🔥 CHANGED TO STATIC

        @SerializedName("id")
        public String id;

        @SerializedName("assignmentId")
        public String assignmentId;

        @SerializedName("studentUid")
        public String studentUid;

        @SerializedName("fileUrl")
        public String fileUrl;

        @SerializedName("submittedAt")
        public String submittedAt;

        @SerializedName("marksObtained")
        public Integer marksObtained;

        @SerializedName("feedback")
        public String feedback;

        @SerializedName("status")
        public String status;

        // 🔥 ALL GETTERS
        public String getId() { return id; }
        public String getAssignmentId() { return assignmentId; }
        public String getStudentUid() { return studentUid; }
        public String getFileUrl() { return fileUrl; }
        public String getSubmittedAt() { return submittedAt; }
        public Integer getMarksObtained() { return marksObtained; }
        public String getFeedback() { return feedback; }
        public String getStatus() { return status; }

        // 🔥 ALL SETTERS - FOR INSTANT UI UPDATE
        public void setId(String id) { this.id = id; }
        public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
        public void setStudentUid(String studentUid) { this.studentUid = studentUid; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
        public void setMarksObtained(Integer marksObtained) { this.marksObtained = marksObtained; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        public void setStatus(String status) { this.status = status; }
    }

}
