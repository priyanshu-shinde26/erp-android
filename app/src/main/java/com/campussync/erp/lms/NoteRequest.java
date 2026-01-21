package com.campussync.erp.lms;

public class NoteRequest {
    public String id;
    public String studentId;
    public String classId;
    public String subject;
    public String message;
    public String status = "PENDING"; // PENDING, RESOLVED
    public String timestamp;

    public NoteRequest() {}
}
