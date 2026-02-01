package com.campussync.erp.academics;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.Log;

import retrofit2.Callback;

public class AcademicUtils {

    // ================= TOKEN =================
    public static String getToken(Context c) {
        if (c == null) return null;

        String token = c.getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .getString("firebase_token", null);

        Log.d("AcademicUtils", "Token fetched: " + (token != null ? "FOUND" : "NULL"));
        return token;
    }

    // ================= ROLE =================
    public static String getRoleFromPrefs(Context c) {
        if (c == null) return null;

        return c.getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .getString("role", null);
    }

    // ================= DEFAULT CALLBACK =================
    public static <T> Callback<T> defaultCallback(Runnable success) {
        return new retrofit2.Callback<T>() {
            @Override
            public void onResponse(retrofit2.Call<T> c,
                                   retrofit2.Response<T> r) {
                if (r.isSuccessful() && success != null) {
                    success.run();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<T> c, Throwable t) {
                t.printStackTrace();
            }
        };
    }
    public static String getStudentClass(Context c) {
        return c.getSharedPreferences("user", MODE_PRIVATE)
                .getString("classId", null);
    }

}
