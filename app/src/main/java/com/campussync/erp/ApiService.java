package com.campussync.erp;

import com.campussync.erp.timetable.TimetableEntry;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // ===================== EXISTING ENDPOINTS =====================
    // keep your other methods here (students, attendance, etc.)
    // e.g. Call<List<Student>> getStudents(...);
    // =============================================================


    // ====================== TIMETABLE API =========================

    // Everyone logged in (Student / Teacher / Admin) – view all entries
    @GET("api/timetable")
    Call<List<TimetableEntry>> getTimetableAll();

    // Optional: filter by day e.g. /api/timetable/day/MONDAY
    @GET("api/timetable/day/{dayOfWeek}")
    Call<List<TimetableEntry>> getTimetableByDay(@Path("dayOfWeek") String dayOfWeek);

    // Admin / Teacher – create
    @POST("api/timetable")
    Call<TimetableEntry> createTimetableEntry(@Body TimetableEntry entry);

    // Admin / Teacher – update
    @PUT("api/timetable/{id}")
    Call<TimetableEntry> updateTimetableEntry(@Path("id") Long id,
                                              @Body TimetableEntry entry);

    // Admin / Teacher – delete
    @DELETE("api/timetable/{id}")
    Call<Void> deleteTimetableEntry(@Path("id") Long id);

    // =============================================================
}
