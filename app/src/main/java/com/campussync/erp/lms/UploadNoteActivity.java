package com.campussync.erp.lms;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.erp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UploadNoteActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etSubject;
    private MaterialButton btnChooseFile, btnUpload;
    private TextView tvSelectedFile;
    private ProgressBar progressBar;

    private Uri selectedFileUri;
    private String classId, uid;
    private LmsApi lmsApi;

    private final ActivityResultLauncher<String[]> pickFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    tvSelectedFile.setText(getFileName(uri));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_note);

        classId = getIntent().getStringExtra("classId");
        uid = getIntent().getStringExtra("uid");

        initViews();
        initRetrofit();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etSubject = findViewById(R.id.etSubject);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnUpload = findViewById(R.id.btnUpload);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initRetrofit() {
        // NOTE: BODY logging prints the whole PDF in Logcat; use BASIC for safety.
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:9090/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        lmsApi = retrofit.create(LmsApi.class);
    }

    private void setupListeners() {
        btnChooseFile.setOnClickListener(v -> pickDocument());
        btnUpload.setOnClickListener(v -> uploadNote());
    }

    private void pickDocument() {
        pickFileLauncher.launch(new String[]{
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
    }

    private void uploadNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String subject = etSubject.getText() != null ? etSubject.getText().toString().trim() : "";

        if (classId == null || classId.trim().isEmpty()) {
            toast("ClassId missing");
            return;
        }
        if (title.isEmpty()) {
            etTitle.setError("Enter title");
            return;
        }
        if (subject.isEmpty()) {
            etSubject.setError("Enter subject");
            return;
        }
        if (selectedFileUri == null) {
            toast("Select file first");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            toast("User not logged in");
            return;
        }

        String uid = user.getUid(); // ✅ Firebase UID

        setLoading(true);

        // Get Firebase ID token
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getToken() == null) {
                setLoading(false);
                toast("Token error");
                return;
            }

            String idToken = task.getResult().getToken();
            String authHeader = "Bearer " + idToken;

            try {
                // ✅ Fixed RequestBody creation
                RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), title);
                RequestBody subjectBody = RequestBody.create(MediaType.parse("text/plain"), subject);
                MultipartBody.Part filePart = createFilePart("file", selectedFileUri);

                // ✅ Matches your backend exactly
                lmsApi.uploadNote(authHeader, uid, classId, titleBody, subjectBody, filePart)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                setLoading(false);
                                if (response.isSuccessful()) {
                                    toast("✅ Uploaded successfully!");
                                    finish();
                                } else {
                                    toast("Upload failed (" + response.code() + ")");
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                setLoading(false);
                                toast("Network error: " + t.getMessage());
                            }
                        });


            } catch (Exception e) {
                setLoading(false);
                toast("File error: " + e.getMessage());
            }
        });
    }

    private MultipartBody.Part createFilePart(String partName, Uri uri) throws IOException {
        String fileName = getFileName(uri);
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";

        byte[] bytes = readBytesFromUri(uri);
        RequestBody fileBody = RequestBody.create(MediaType.parse(mime), bytes);
        return MultipartBody.Part.createFormData(partName, fileName, fileBody);
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Cannot open file");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        inputStream.close();
        return buffer.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = "file";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) result = cursor.getString(index);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!loading);
        btnChooseFile.setEnabled(!loading);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
