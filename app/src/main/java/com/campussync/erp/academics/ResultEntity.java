package com.campussync.erp.academics;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "student_results")
public class ResultEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String subject;
    public String test;
    public int marks;
    public int maxMarks;
}
