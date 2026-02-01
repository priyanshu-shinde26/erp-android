package com.campussync.erp.attendance;

public class StudentModel {
    public String uid;
    public String name;
    public String rollNo;
    public String classId;  // ENSURED: Added for class-based attendance

    public StudentModel() {}

    public String getName() {
        return name;
    }

    public String getRollNumber() {
        return rollNo;
    }
}