package com.example.rabbitmq.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailActionRequest {
    @JsonProperty("action")
    private String action;

    @JsonProperty("sourceMailboxID")
    private String sourceMailboxID;

    @JsonProperty("sourceMessageID")
    private String sourceMessageID;

    @JsonProperty("destinationMailboxID")
    private String destinationMailboxID;

    @JsonProperty("hashID")
    private String hashID;

    // Default constructor
    public EmailActionRequest() {
    }

    // Constructor
    public EmailActionRequest(String action, String sourceMailboxID,
                              String sourceMessageID, String destinationMailboxID,
                              String hashID) {
        this.action = action;
        this.sourceMailboxID = sourceMailboxID;
        this.sourceMessageID = sourceMessageID;
        this.destinationMailboxID = destinationMailboxID;
        this.hashID = hashID;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSourceMailboxID() {
        return sourceMailboxID;
    }

    public void setSourceMailboxID(String sourceMailboxID) {
        this.sourceMailboxID = sourceMailboxID;
    }

    public String getSourceMessageID() {
        return sourceMessageID;
    }

    public void setSourceMessageID(String sourceMessageID) {
        this.sourceMessageID = sourceMessageID;
    }

    public String getDestinationMailboxID() {
        return destinationMailboxID;
    }

    public void setDestinationMailboxID(String destinationMailboxID) {
        this.destinationMailboxID = destinationMailboxID;
    }

    public String getHashID() {
        return hashID;
    }

    public void setHashID(String hashID) {
        this.hashID = hashID;
    }

    @Override
    public String toString() {
        return "EmailActionRequest{" +
                "action='" + action + '\'' +
                ", sourceMailboxID='" + sourceMailboxID + '\'' +
                ", sourceMessageID='" + sourceMessageID + '\'' +
                ", destinationMailboxID='" + destinationMailboxID + '\'' +
                ", hashID='" + hashID + '\'' +
                '}';
    }
}