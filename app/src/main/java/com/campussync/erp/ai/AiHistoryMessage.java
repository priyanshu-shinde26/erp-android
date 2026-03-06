package com.campussync.erp.ai;

import com.google.gson.annotations.SerializedName;

public class AiHistoryMessage {

    @SerializedName("role")
    private String role;

    @SerializedName("content")
    private String content;

    public AiHistoryMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
}