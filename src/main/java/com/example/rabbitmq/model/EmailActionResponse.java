package com.example.rabbitmq.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailActionResponse {
    @JsonProperty("hashID")
    private String hashID;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    public EmailActionResponse() {
    }

    public EmailActionResponse(String hashID, String status, String message) {
        this.hashID = hashID;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static EmailActionResponse success(String hashID, String message) {
        return new EmailActionResponse(hashID, "SUCCESS", message);
    }

    public static EmailActionResponse failure(String hashID, String message) {
        return new EmailActionResponse(hashID, "FAILED", message);
    }

    // Getters and Setters
    public String getHashID() {
        return hashID;
    }

    public void setHashID(String hashID) {
        this.hashID = hashID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}