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

import com.campussync.erp.attendance.ManageAttendanceActivity;
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

    // backend test endpoint
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
                            onAuthSuccess();   // ✅ no role write here
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                Log.w(TAG, "Email already in use, attempting sign-in");
                                mAuth.signInWithEmailAndPassword(email, pass)
                                        .addOnCompleteListener(signInTask -> {
                                            if (signInTask.isSuccessful()) {
                                                Log.d(TAG, "Signed in after collision");
                                                onAuthSuccess();
                                            } else {
                                                String msg = signInTask.getException() != null
                                                        ? signInTask.getException().getMessage()
                                                        : "Sign-in failed";
                                                Toast.makeText(this, "Sign-in failed: " + msg, Toast.LENGTH_LONG).show();
                                                Log.e(TAG, "Sign-in after collision failed", signInTask.getException());
                                                setButtonsEnabled(true);
                                            }
                                        });
                            } else {
                                String msg = e != null ? e.getMessage() : "unknown error";
                                Toast.makeText(this, "Signup failed: " + msg, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Signup failed", e);
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
                            Log.d(TAG, "Login successful");
                            onAuthSuccess();   // ✅ no role write here
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                            Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Login failed", task.getException());
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

    /**
     * After successful auth, fetch ID token, test backend, then open dashboard based on role.
     */
    private void onAuthSuccess() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "onAuthSuccess: currentUser is null");
            setButtonsEnabled(true);
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "onAuthSuccess: uid=" + uid);

        // Get fresh ID token (also copied to clipboard + sent to backend)
        mAuth.getCurrentUser().getIdToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> tokenTask) {
                        if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                            String idToken = tokenTask.getResult().getToken();
                            if (idToken != null) {
                                Log.d(TAG, "Firebase ID Token obtained");
                                copyToClipboard(idToken);
                                sendTokenToBackend(idToken);
                            }
                        } else {
                            Log.e(TAG, "Failed to get ID token", tokenTask.getException());
                        }
                    }
                });

        // 🔥 Decide dashboard by role from Realtime DB
        openDashboardBasedOnRole(uid);
    }

    /**
     * Read /roles/{uid} and route to correct dashboard.
     */
    private void openDashboardBasedOnRole(@NonNull String uid) {
        Log.d(TAG, "openDashboardBasedOnRole: uid=" + uid);

        FirebaseDatabase.getInstance().getReference("roles")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object raw = snapshot.getValue();
                        Log.d(TAG, "Role raw snapshot for uid=" + uid + " -> " + raw);

                        String role;
                        if (raw == null) {
                            role = "STUDENT";
                        } else {
                            role = String.valueOf(raw).trim().toUpperCase();
                        }

                        Log.d(TAG, "Normalized role = " + role);
                        Toast.makeText(LoginActivity.this, "Role: " + role, Toast.LENGTH_SHORT).show();

                        Intent intent;
                        switch (role) {
                            case "ADMIN":
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                Log.d(TAG, "Opening AdminDashboardActivity");
                                break;
                            case "TEACHER":
                                intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
                                Log.d(TAG, "Opening TeacherDashboardActivity");
                                break;
                            default:
                                intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                                Log.d(TAG, "Opening StudentDashboardActivity (default)");
                                break;
                        }

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read role: " + error.getMessage());
                        Toast.makeText(LoginActivity.this,
                                "Role read error, going to Student dashboard",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void copyToClipboard(@NonNull String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Firebase ID Token", text);
                clipboard.setPrimaryClip(clip);
                Log.d(TAG, "ID token copied to clipboard");
            } else {
                Log.w(TAG, "ClipboardManager is null; cannot copy token");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to copy token to clipboard", e);
        }
    }

    private void sendTokenToBackend(@NonNull String idToken) {
        Request request = new Request.Builder()
                .url(BACKEND_TEST_URL)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "Backend test call failed: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "<no-body>";
                Log.i(TAG, "Backend responded: " + response.code() + " " + body);
            }
        });
    }
}
