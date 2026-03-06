package com.campussync.erp.ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AiRequestDto {

    @SerializedName("message")
    private String message;

    @SerializedName("role")
    private String role;

    @SerializedName("conversationHistory")
    private List<AiHistoryMessage> conversationHistory;

    public AiRequestDto(String message, String role, List<AiHistoryMessage> conversationHistory) {
        this.message = message;
        this.role = role;
        this.conversationHistory = conversationHistory;
    }

    public String getMessage() { return message; }
    public String getRole() { return role; }
    public List<AiHistoryMessage> getConversationHistory() { return conversationHistory; }
}