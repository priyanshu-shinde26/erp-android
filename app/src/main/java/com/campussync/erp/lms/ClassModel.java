package com.campussync.erp.lms;

public class ClassModel {
    public String classId;
    public String name;

    public ClassModel() {}

    @Override
    public String toString() {
        return name != null ? name : classId;
    }
}
