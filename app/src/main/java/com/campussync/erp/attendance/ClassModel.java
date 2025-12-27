package com.campussync.erp.attendance;

public class ClassModel {
    public String classId;
    public String name;
    public String course;
    public String year;

    public ClassModel() {}

    @Override
    public String toString() {
        return name; // For Spinner display
    }
}
