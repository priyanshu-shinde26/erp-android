package com.campussync.erp.attendance;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class BackendClient {

    private static final String TAG = "BackendClient";
    // Emulator to Spring Boot on PC
    public static final String BASE_URL = "http://10.0.2.2:9090";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public interface TokenCallback {
        void onToken(String token);
        void onError(Exception e);
    }

    public static void getIdToken(TokenCallback callback) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.onError(new IllegalStateException("Not logged in"));
            return;
        }
        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(false)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            callback.onToken(task.getResult().getToken());
                        } else {
                            callback.onError(task.getException() != null
                                    ? task.getException()
                                    : new Exception("Failed to get ID token"));
                        }
                    }
                });
    }

    public static void get(String urlPath, String idToken, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + urlPath)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public static void postJson(String urlPath, String idToken, String jsonBody, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + urlPath)
                .addHeader("Authorization", "Bearer " + idToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}
