package com.campussync.erp.lms;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface LmsApi {

    @Multipart
    @POST("api/lms/notes/{classId}")
    Call<Void> uploadNote(  // ✅ Void = ignore body
                            @Header("Authorization") String authorization,
                            @Header("uid") String uid,
                            @Path("classId") String classId,
                            @Part("title") RequestBody title,
                            @Part("subject") RequestBody subject,
                            @Part MultipartBody.Part file
    );

}
