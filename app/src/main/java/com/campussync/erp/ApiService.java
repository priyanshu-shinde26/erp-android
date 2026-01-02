package com.campussync.erp;

import com.campussync.erp.timetable.SchedulePeriod;
import com.campussync.erp.timetable.ClassTimetableResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // ===================== YOUR EXISTING ENDPOINTS (KEEP ALL) =====================
    // getStudents(), attendance, assignments, etc. – UNCHANGED!
    // ============================================================================

    // ====================== TIMETABLE FIREBASE ENDPOINTS =========================

    // ✅ NEW: Get classes list for dropdown (unique from students)
    @GET("api/timetable/classes")
    Call<List<String>> getTimetableClasses();

    // ✅ NEW: Teacher/Admin - Get day schedule by class/day
    @GET("api/timetable/{classId}/{day}")
    Call<List<SchedulePeriod>> getDaySchedule(@Path("classId") String classId,
                                              @Path("day") String day);

    // ✅ NEW: Student - Get full week schedule (or today)
    @GET("api/timetable/{classId}")
    Call<ClassTimetableResponse> getClassTimetable(@Path("classId") String classId);

    // ✅ NEW: Teacher/Admin - Create period for class/day
    @POST("api/timetable/{classId}/{day}")
    Call<Void> createTimetablePeriod(@Path("classId") String classId,
                                     @Path("day") String day,
                                     @Body SchedulePeriod period);

    // ✅ NEW: Teacher/Admin - Update specific period
    @PUT("api/timetable/{classId}/{day}/{periodId}")
    Call<Void> updateTimetablePeriod(@Path("classId") String classId,
                                     @Path("day") String day,
                                     @Path("periodId") String periodId,
                                     @Body SchedulePeriod period);

    // ✅ NEW: Teacher/Admin - Delete specific period
    @DELETE("api/timetable/{classId}/{day}/{periodId}")
    Call<Void> deleteTimetablePeriod(@Path("classId") String classId,
                                     @Path("day") String day,
                                     @Path("periodId") String periodId);

    // ===================== BACKWARD COMPATIBILITY (Optional) ======================
   //==================================================================
}
