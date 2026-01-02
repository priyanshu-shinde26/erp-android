package com.campussync.erp.timetable;

public class SchedulePeriod {
    // ✅ Backend sends these fields
    public String id;
    public String subject;
    public String teacher;
    public String startTime;
    public String endTime;
    public String room;

    // ✅ Gson requires empty constructor
    public SchedulePeriod() {}

    // ✅ Safe display time (null-proof)
    public String getDisplayTime() {
        String start = (startTime != null ? startTime : "");
        String end = (endTime != null ? endTime : "");
        return start + " - " + end;
    }
}
