package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;
import java.util.Date;

public class Message {
    private ObjectId _id;
    private ObjectId senderId;
    private String senderType; // "farmer" or "vet"
    private ObjectId receiverId;
    private String receiverType; // "farmer" or "vet"
    private String conversationId; // Generated from participant IDs
    private String content;
    private String messageType; // "text", "image", etc.
    private String priority; // "normal", "high", "urgent"
    private Boolean isRead;
    private Boolean isDeleted;
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Message() {}

    public Message(ObjectId senderId, String senderType, ObjectId receiverId, String receiverType,
                   String conversationId, String content, String messageType, String priority) {
        this.senderId = senderId;
        this.senderType = senderType;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.conversationId = conversationId;
        this.content = content;
        this.messageType = messageType != null ? messageType : "text";
        this.priority = priority != null ? priority : "normal";
        this.isRead = false;
        this.isDeleted = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public ObjectId getId() {
        return _id;
    }

    public void setId(ObjectId _id) {
        this._id = _id;
    }

    public ObjectId getSenderId() {
        return senderId;
    }

    public void setSenderId(ObjectId senderId) {
        this.senderId = senderId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public ObjectId getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(ObjectId receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}