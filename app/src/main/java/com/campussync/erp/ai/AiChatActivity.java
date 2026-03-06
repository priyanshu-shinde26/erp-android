package com.campussync.erp.ai;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.erp.R;
import com.campussync.erp.TokenManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AiChatAdapter adapter;
    private List<AiMessage> messages;
    private List<AiHistoryMessage> conversationHistory;

    private EditText etMessage;
    private ImageButton btnSend, btnAttach, btnBack;
    private TextView tvTitle;

    private String userRole = "student";
    private Uri selectedFileUri;
    private String selectedFileName;
    private String selectedMimeType;

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    selectedFileName = getFileName(uri);
                    selectedMimeType = getMimeType(uri);
                    showFilePreview();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        userRole = getIntent().getStringExtra("role") != null
                ? getIntent().getStringExtra("role") : "student";

        initViews();
        setupRecyclerView();
        showWelcomeMessage();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_chat);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        btnAttach    = findViewById(R.id.btn_attach);
        btnBack      = findViewById(R.id.btn_back);
        tvTitle      = findViewById(R.id.tv_ai_title);

        String roleLabel;
        switch (userRole) {
            case "teacher":
                roleLabel = "Teacher AI";
                break;
            case "admin":
                roleLabel = "Admin AI";
                break;
            default:
                roleLabel = "Study AI";
                break;
        }
        tvTitle.setText("CampusSync " + roleLabel);
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (selectedFileUri != null) {
                sendFileMessage(text);
            } else if (!text.isEmpty()) {
                sendTextMessage(text);
            }
        });

        btnAttach.setOnClickListener(v ->
                filePicker.launch(new String[]{
                        "application/pdf",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "image/*",
                        "text/plain"
                })
        );
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        conversationHistory = new ArrayList<>();
        adapter = new AiChatAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void showWelcomeMessage() {
        String welcome;

        switch (userRole) {
            case "teacher":
                welcome = "👋 Hello Teacher! I can help you with lesson content, creating quiz questions, explaining topics, or analyzing uploaded materials.";
                break;

            case "admin":
                welcome = "👋 Hello Admin! I can assist with academic queries, student information, and institutional questions.";
                break;

            default:
                welcome = "👋 Hello! I'm your CampusSync AI. Ask me any doubts, upload your notes for a summary, or share files for analysis. Let's study together! 📚";
                break;
        }

        addAiMessage(welcome);
    }

    // ── Text message ──────────────────────────────────────────────────────────

    private void sendTextMessage(String text) {
        etMessage.setText("");
        addUserMessage(text, null);
        showLoadingIndicator();

        conversationHistory.add(new AiHistoryMessage("user", text));

        AiRequestDto request = new AiRequestDto(text, userRole,
                new ArrayList<>(conversationHistory.subList(
                        0, Math.max(0, conversationHistory.size() - 1))));

        AiRetrofitClient.getInstance().chat(request)
                .enqueue(new Callback<AiResponseDto>() {
                    @Override
                    public void onResponse(Call<AiResponseDto> call,
                                           Response<AiResponseDto> response) {
                        removeLoadingIndicator();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            String reply = response.body().getReply();
                            conversationHistory.add(new AiHistoryMessage("model", reply));
                            addAiMessage(reply);
                        } else {
                            addAiMessage("❌ Sorry, I couldn't process your request. Please try again.");
                        }
                    }

                    @Override
                    public void onFailure(Call<AiResponseDto> call, Throwable t) {
                        removeLoadingIndicator();
                        addAiMessage("❌ Connection error. Please check your internet connection.");
                    }
                });
    }

    // ── File message ──────────────────────────────────────────────────────────

    private void sendFileMessage(String additionalText) {
        String userText = additionalText.isEmpty()
                ? "Please analyze this file: " + selectedFileName
                : additionalText;

        etMessage.setText("");
        addUserMessage(userText, selectedFileName);
        clearFilePreview();
        showLoadingIndicator();

        final Uri fileUri = selectedFileUri;
        final String mime = selectedMimeType;
        final String fname = selectedFileName;
        selectedFileUri = null;

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                byte[] bytes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bytes = is.readAllBytes();
                }
                is.close();
                String base64 = Base64.getEncoder().encodeToString(bytes);

                AiMultimodalDto request = new AiMultimodalDto(
                        userText, base64, mime, fname, userRole);

                runOnUiThread(() ->
                        AiRetrofitClient.getInstance().analyzeFile(request)
                                .enqueue(new Callback<AiResponseDto>() {
                                    @Override
                                    public void onResponse(Call<AiResponseDto> call,
                                                           Response<AiResponseDto> response) {
                                        removeLoadingIndicator();
                                        if (response.isSuccessful() && response.body() != null
                                                && response.body().isSuccess()) {
                                            addAiMessage(response.body().getReply());
                                        } else {
                                            // ADD THIS to see what the backend is actually returning
                                            String errorBody = "";
                                            try {
                                                if (response.errorBody() != null)
                                                    errorBody = response.errorBody().string();
                                            } catch (Exception ignored) {}
                                            android.util.Log.e("AiChat", "Failed response code: "
                                                    + response.code() + " body: " + errorBody);

                                            addAiMessage("❌ Sorry, I couldn't process your request. Please try again.");
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<AiResponseDto> call, Throwable t) {
                                        removeLoadingIndicator();
                                        addAiMessage("❌ Upload failed. Check your connection.");
                                    }
                                })
                );
            } catch (Exception e) {
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    addAiMessage("❌ Failed to read file: " + e.getMessage());
                });
            }
        }).start();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void addUserMessage(String text, String fileName) {
        messages.add(new AiMessage(AiMessage.TYPE_USER, text, fileName));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAiMessage(String text) {
        messages.add(new AiMessage(AiMessage.TYPE_AI, text));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void showLoadingIndicator() {
        messages.add(AiMessage.loadingMessage());
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void removeLoadingIndicator() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isLoading()) {
                messages.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void showFilePreview() {
        etMessage.setHint("📎 " + selectedFileName + " — add a message or send");
    }

    private void clearFilePreview() {
        etMessage.setHint("Ask anything...");
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "file";
    }

    private String getMimeType(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime == null) {
            String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }
        return mime != null ? mime : "application/octet-stream";
    }
}