package com.campussync.erp.lms;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Note {
    public String id;
    public String title;
    public String subject;
    public String url;
    public String filename;
    public long timestamp;      // ✅ can hold map/long/string

    public String uploadedBy;       // ✅ UID (added)
    public String uploadedByName;   // ✅ Teacher Name

    public Note() {
    }

    public String getFormattedTime() {
        if (timestamp == 0) return "Just now";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {           // < 1 min
            return "Just now";
        } else if (diff < 3600000) {  // < 1 hour
            long mins = diff / 60000;
            return mins + "m ago";
        } else if (diff < 86400000) { // < 1 day
            long hours = diff / 3600000;
            return hours + "h ago";
        } else {                      // ≥ 1 day
            long days = diff / 86400000;
            return days + "d ago";
        }
    }
}
