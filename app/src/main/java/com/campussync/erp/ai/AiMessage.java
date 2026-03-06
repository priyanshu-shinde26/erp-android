package com.campussync.erp.ai;

public class AiMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI   = 1;
    public static final int TYPE_FILE = 2;

    private int type;
    private String text;
    private String fileName;   // for file messages
    private boolean isLoading; // for typing indicator

    public AiMessage(int type, String text) {
        this.type = type;
        this.text = text;
        this.isLoading = false;
    }

    public AiMessage(int type, String text, String fileName) {
        this.type = type;
        this.text = text;
        this.fileName = fileName;
        this.isLoading = false;
    }

    public static AiMessage loadingMessage() {
        AiMessage msg = new AiMessage(TYPE_AI, "");
        msg.isLoading = true;
        return msg;
    }

    public int getType() { return type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getFileName() { return fileName; }
    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }
}