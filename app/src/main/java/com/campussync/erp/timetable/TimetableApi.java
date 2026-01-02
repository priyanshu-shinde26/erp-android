package com.campussync.erp.timetable;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TimetableApi {

    @GET("api/timetable/classes")
    Call<List<String>> getClasses(@Header("Authorization") String bearerToken);

    @GET("api/timetable/{classId}/{day}")
    Call<List<Map<String, Object>>> getDaySchedule(  // ✅ Backend returns Map<String,Object>
                                                     @Header("Authorization") String bearerToken,
                                                     @Path("classId") String classId,
                                                     @Path("day") String day);

    @POST("api/timetable/{classId}/{day}")
    Call<Void> createPeriod(
            @Header("Authorization") String bearerToken,
            @Path("classId") String classId,
            @Path("day") String day,
            @Body Map<String, Object> period  // ✅ Use Map for create
    );

    @PUT("api/timetable/{classId}/{day}/{periodId}")
    Call<Void> updatePeriod(
            @Header("Authorization") String bearerToken,
            @Path("classId") String classId,
            @Path("day") String day,
            @Path("periodId") String periodId,
            @Body Map<String, Object> period
    );

    @DELETE("api/timetable/{classId}/{day}/{periodId}")
    Call<Void> deletePeriod(
            @Header("Authorization") String bearerToken,
            @Path("classId") String classId,
            @Path("day") String day,
            @Path("periodId") String periodId
    );
}
