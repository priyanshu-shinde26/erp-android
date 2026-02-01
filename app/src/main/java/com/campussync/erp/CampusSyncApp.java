package com.campussync.erp;

import android.app.Application;
import android.util.Log;

public class CampusSyncApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 🔥 Initialize Retrofit ONCE for whole app
        RetrofitClient.init(this);

        Log.d("CampusSyncApp", "✅ Retrofit initialized");
    }
}
