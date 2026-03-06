package com.campussync.erp.ai;

public class AiSummaryDto {
    private String noteUrl;
    private String fileBase64;
    private String mimeType;
    private String noteTitle;
    private String role;

    public AiSummaryDto(String noteUrl, String fileBase64,
                        String mimeType, String noteTitle, String role) {
        this.noteUrl = noteUrl;
        this.fileBase64 = fileBase64;
        this.mimeType = mimeType;
        this.noteTitle = noteTitle;
        this.role = role;
    }

    public String getNoteUrl() { return noteUrl; }
    public String getFileBase64() { return fileBase64; }
    public String getMimeType() { return mimeType; }
    public String getNoteTitle() { return noteTitle; }
    public String getRole() { return role; }
}