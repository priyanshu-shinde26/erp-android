package com.campussync.erp.timetable;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TimetableApi {

    // GET /api/timetable/class/{className}
    @GET("api/timetable/class/{className}")
    Call<List<TimetableEntry>> getTimetableForClass(@Path("className") String className);
}

