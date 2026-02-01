package com.campussync.erp;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static Context appContext;

    private static final String BASE_URL = "http://10.0.2.2:9090/";

    // 🔥 Called once from Application
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        Log.d("RetrofitClient", "✅ init()");
    }

    // 🔥 MUST call after token save
    public static void resetClient() {
        retrofit = null;
        Log.d("RetrofitClient", "♻️ Retrofit reset");
    }

    public static Retrofit getClient() {
        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {

                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder();

                            String token = TokenManager.getToken(appContext);
                            Log.d("AUTH_DEBUG", "Token = " + token);

                            if (token != null && !token.isEmpty()) {
                                builder.addHeader("Authorization", "Bearer " + token);
                                Log.d("AUTH_DEBUG", "✅ Authorization header added");
                            } else {
                                Log.e("AUTH_DEBUG", "❌ Missing token → 401");
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }

        return retrofit;
    }
}
