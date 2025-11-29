package com.campussync.erp.timetable;

public class TimetableEntry {

    private Long id;
    private String dayOfWeek;   // e.g. "MONDAY"
    private String startTime;   // e.g. "09:00"
    private String endTime;     // e.g. "10:00"
    private String subjectName; // matches backend field
    private String teacherName;
    private String classroom;

    public TimetableEntry() {
    }

    public TimetableEntry(Long id,
                          String dayOfWeek,
                          String startTime,
                          String endTime,
                          String subjectName,
                          String teacherName,
                          String classroom) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.classroom = classroom;
    }

    public Long getId() {
        return id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }
}
