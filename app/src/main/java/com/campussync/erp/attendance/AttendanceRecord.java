package com.campussync.erp.attendance;

public class AttendanceRecord {
    public String rollNumber;
    public String status;
    public String date;
    public String markedBy;
    public String classId; // ✅ ADDED

    public AttendanceRecord() {}

    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getClassId() { return classId; }
}
