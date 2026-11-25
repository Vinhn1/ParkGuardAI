package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Model biểu diễn thông báo gửi cho sinh viên.
 */
public class StudentNotificationItem implements Serializable {
    private String id;
    private String title;
    private String message;
    private String type; // VIOLATION, SECURITY_ALERT, VEHICLE, SYSTEM
    private String timeFormatted;
    private long timestamp;
    private boolean isRead;
    private boolean requiresAction;
    private String userResponse;
    private String relatedId;

    public StudentNotificationItem() {
    }

    public StudentNotificationItem(String id, String title, String message, String type,
                                   String timeFormatted, long timestamp, boolean isRead,
                                   boolean requiresAction, String relatedId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timeFormatted = timeFormatted;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.requiresAction = requiresAction;
        this.relatedId = relatedId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimeFormatted() {
        return timeFormatted;
    }

    public void setTimeFormatted(String timeFormatted) {
        this.timeFormatted = timeFormatted;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public String getUserResponse() {
        return userResponse;
    }

    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }
}
