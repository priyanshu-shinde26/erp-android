package com.campussync.erp.assignment;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client to access the AssignmentApiService.
 *
 * BASE_URL points to your Spring Boot backend.
 * For Android emulator connecting to localhost: use http://10.0.2.2:9090/
 */
public class AssignmentRetrofitClient {

    private static final String TAG = "AssignmentRetrofitClient";

    // IMPORTANT: if your backend runs on your dev machine at http://localhost:9090
    // and you test on Android emulator, use 10.0.2.2
    private static final String BASE_URL = "http://10.0.2.2:9090/";

    private static Retrofit retrofit;
    private static AssignmentApiService apiService;

    private AssignmentRetrofitClient() {
        // no-op
    }

    public static AssignmentApiService getApiService() {
        if (apiService == null) {
            synchronized (AssignmentRetrofitClient.class) {
                if (apiService == null) {
                    apiService = buildRetrofit().create(AssignmentApiService.class);
                }
            }
        }
        return apiService;
    }

    private static Retrofit buildRetrofit() {
        // Logging interceptor: logs request/response for debugging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, message);
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
