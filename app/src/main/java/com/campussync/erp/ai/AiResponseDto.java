package com.campussync.erp.ai;

import com.google.gson.annotations.SerializedName;

public class AiResponseDto {

    @SerializedName("reply")
    private String reply;

    @SerializedName("success")
    private boolean success;

    @SerializedName("error")
    private String error;

    public String getReply() { return reply; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
}