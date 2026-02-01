package com.campussync.erp.academics;

import android.content.Context;
import android.util.Log;

import com.campussync.erp.TokenManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class AcademicRepository {

    private static AcademicApi api;

    // ================= CORE API INSTANCE =================
    public static AcademicApi getApi(Context context) {

        if (api == null) {
            String token = TokenManager.getToken(context);
            Log.d("AUTH_REPO", "Creating API | tokenPresent=" + (token != null));

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(token))
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:9090/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            api = retrofit.create(AcademicApi.class);
        }

        return api;
    }

    // ================= GET TESTS =================
    public static Call<List<TestModel>> getTests(
            Context context,
            String classId,
            String subjectId
    ) {
        return getApi(context).getTests(classId, subjectId);
    }

}
