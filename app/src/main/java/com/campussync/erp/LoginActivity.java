package com.campussync.erp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private EditText emailEt, passEt;
    private Button loginBtn, signupBtn;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private static final String BACKEND_TEST_URL = "http://10.0.2.2:9090/api/test/firebase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();
        emailEt = findViewById(R.id.email);
        passEt = findViewById(R.id.pass);
        loginBtn = findViewById(R.id.login);
        signupBtn = findViewById(R.id.signup);

        signupBtn.setOnClickListener(v -> {
            setButtonsEnabled(false);
            String email = emailEt.getText().toString().trim();
            String pass = passEt.getText().toString().trim();

            if (!isValidEmail(email) || !isValidPassword(pass)) {
                Toast.makeText(this, "Enter valid email and password (min 6 chars)", Toast.LENGTH_SHORT).show();
                setButtonsEnabled(true);
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Signup successful");
                            onAuthSuccess();
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                mAuth.signInWithEmailAndPassword(email, pass)
                                        .addOnCompleteListener(signInTask -> {
                                            if (signInTask.isSuccessful()) {
                                                onAuthSuccess();
                                            } else {
                                                setButtonsEnabled(true);
                                            }
                                        });
                            } else {
                                setButtonsEnabled(true);
                            }
                        }
                    });
        });

        loginBtn.setOnClickListener(v -> {
            setButtonsEnabled(false);
            String email = emailEt.getText().toString().trim();
            String pass = passEt.getText().toString().trim();

            if (!isValidEmail(email) || pass.isEmpty()) {
                Toast.makeText(this, "Enter a valid email and password", Toast.LENGTH_SHORT).show();
                setButtonsEnabled(true);
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            onAuthSuccess();
                        } else {
                            setButtonsEnabled(true);
                        }
                    });
        });
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String pass) {
        return pass != null && pass.length() >= 6;
    }

    private void setButtonsEnabled(boolean enabled) {
        loginBtn.setEnabled(enabled);
        signupBtn.setEnabled(enabled);
    }

    private void onAuthSuccess() {
        if (mAuth.getCurrentUser() == null) return;
        openDashboardBasedOnRole(mAuth.getCurrentUser().getUid());
    }

    private void openDashboardBasedOnRole(@NonNull String uid) {
        FirebaseDatabase.getInstance().getReference("roles")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String role = snapshot.getValue() == null
                                ? "STUDENT"
                                : snapshot.getValue().toString().toUpperCase();

                        Intent intent;
                        switch (role) {
                            case "ADMIN":
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                break;
                            case "TEACHER":
                                intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
                                break;
                            default:
                                intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                                break;
                        }

                        FirebaseAuth.getInstance().getCurrentUser()
                                .getIdToken(true)
                                .addOnSuccessListener(result -> {

                                    String token = result.getToken();
                                    Log.d("AUTH_DEBUG", "🔥 Firebase token = " + token);

                                    if (token != null) {
                                        TokenManager.saveToken(LoginActivity.this, token);

                                        // 🔥 REQUIRED CHANGE
                                        RetrofitClient.resetClient();

                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);

                        FirebaseAuth.getInstance().getCurrentUser()
                                .getIdToken(true)
                                .addOnSuccessListener(result -> {

                                    String token = result.getToken();
                                    if (token != null) {
                                        TokenManager.saveToken(LoginActivity.this, token);

                                        // 🔥 REQUIRED CHANGE
                                        RetrofitClient.resetClient();

                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    }
                });
    }

    private void copyToClipboard(@NonNull String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Firebase ID Token", text));
        }
    }

    private void sendTokenToBackend(@NonNull String idToken) {
        Request request = new Request.Builder()
                .url(BACKEND_TEST_URL)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }
}
