// AttendanceRecord.java
package com.campussync.erp.attendance;

public class AttendanceRecord {
    public String courseId;
    public String date;       // "yyyy-MM-dd"
    public String studentUid;
    public String status;     // "PRESENT" / "ABSENT"
    public String markedBy;

    public AttendanceRecord() {} // needed for JSON

    @Override
    public String toString() {
        return date + " - " + status;
    }
}
