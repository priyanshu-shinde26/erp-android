package com.campussync.erp.ai;

public class AiMultimodalDto {
    private String message;
    private String fileBase64;
    private String mimeType;
    private String fileName;
    private String role;

    public AiMultimodalDto(String message, String fileBase64,
                           String mimeType, String fileName, String role) {
        this.message = message;
        this.fileBase64 = fileBase64;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.role = role;
    }

    public String getMessage() { return message; }
    public String getFileBase64() { return fileBase64; }
    public String getMimeType() { return mimeType; }
    public String getFileName() { return fileName; }
    public String getRole() { return role; }
}