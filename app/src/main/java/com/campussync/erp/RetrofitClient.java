package com.campussync.erp;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // IMPORTANT: use 10.0.2.2 for emulator talking to backend on localhost:9090
    private static final String BASE_URL = "http://10.0.2.2:9090/";

    private static Retrofit retrofitNoAuth;
    private static Retrofit retrofitWithAuth;

    /**
     * Client without Authorization header (if you ever need it)
     */
    public static Retrofit getClient() {
        if (retrofitNoAuth == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                    message -> Log.d("Retrofit", message)
            );
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofitNoAuth = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitNoAuth;
    }

    /**
     * Client that sends: Authorization: Bearer <idToken>
     */
    public static Retrofit getClient(String idToken) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> Log.d("Retrofit", message)
        );
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();

                        if (idToken != null && !idToken.isEmpty()) {
                            builder.header("Authorization", "Bearer " + idToken);
                        }

                        return chain.proceed(builder.build());
                    }
                })
                .build();

        retrofitWithAuth = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofitWithAuth;
    }
}
