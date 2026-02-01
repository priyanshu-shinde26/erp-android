package com.campussync.erp.academics;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AcademicApi {

    @POST("api/academic/test/create")
    Call<Void> createTest(
            @Query("classId") String classId,
            @Query("subjectId") String subjectId,
            @Body TestModel model
    );

    @POST("api/academic/marks/save")
    Call<Void> saveMarks(
            @Query("classId") String classId,
            @Query("subjectId") String subjectId,
            @Query("testId") String testId,
            @Body Map<String, MarksModel> marksMap
    );

    @GET("api/academic/result/student")
    Call<Map<String, Object>> getStudentResult(
            @Query("classId") String classId
    );

    @GET("api/academic/tests")
    Call<List<TestModel>> getTests(
            @Query("classId") String classId,
            @Query("subjectId") String subjectId
    );

}
