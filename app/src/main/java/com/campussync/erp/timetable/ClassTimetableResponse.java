package com.campussync.erp.timetable;

import java.util.List;
import java.util.Map;

public class ClassTimetableResponse {
    public String classId;
    public Map<String, List<SchedulePeriod>> days;
}
