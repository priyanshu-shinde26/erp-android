package com.campussync.erp.academics;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ResultDao {

    @Query("SELECT * FROM student_results")
    List<ResultEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ResultEntity> list);

    @Query("DELETE FROM student_results")
    void clear();
}
