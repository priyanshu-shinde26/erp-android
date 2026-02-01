package com.campussync.erp.academics;

public class MarksModel {

    // Firebase may store these as String
    public String marks;     // ⚠️ CHANGED from int → String
    public String status;    // PRESENT / ABSENT

    public MarksModel() {}

    // Safe helper
    public int getMarksAsInt() {
        try {
            return Integer.parseInt(marks);
        } catch (Exception e) {
            return -1; // ABSENT or invalid
        }
    }
}
