package com.ecommerce.authservice.model;


import java.time.LocalDateTime;

public class SuspendedRequest{

    private String userId;
    private String status;
    private LocalDateTime timestamp;

    public SuspendedRequest() {
    }

    public SuspendedRequest(String userId, String status, LocalDateTime timestamp) {
        this.userId = userId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SuspendedRequest{" +
                "userId='" + userId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}