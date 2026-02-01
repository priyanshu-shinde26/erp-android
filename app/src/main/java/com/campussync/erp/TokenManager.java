package com.campussync.erp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TokenManager {

    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "firebase_token";
    private static final String KEY_SAVED_AT = "token_saved_at";

    public static void saveToken(Context context, String token) {
        if (context == null) return;

        if (token == null || token.trim().isEmpty()) {
            Log.e("TokenManager", "❌ Token is null/empty, not saving");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_SAVED_AT, System.currentTimeMillis())
                .apply();

        Log.d("TokenManager", "✅ Token saved successfully");
    }

    public static String getToken(Context context) {
        if (context == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);

        Log.d("TokenManager", "Token fetched: " + (token != null ? "✅ FOUND" : "❌ NULL"));
        return token;
    }

    public static boolean hasToken(Context context) {
        String token = getToken(context);
        return token != null && !token.trim().isEmpty();
    }

    public static long getTokenSavedAt(Context context) {
        if (context == null) return 0;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_SAVED_AT, 0);
    }

    public static void clearToken(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_SAVED_AT)
                .apply();

        Log.d("TokenManager", "🗑 Token cleared");
    }
}
