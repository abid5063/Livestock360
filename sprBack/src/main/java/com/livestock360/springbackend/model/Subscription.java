package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;

import java.util.Date;

public class Subscription {
    private ObjectId _id;
    private String userId; // Farmer ID who is subscribing
    private String userType; // "farmer" (can extend to other user types later)
    private Double amount; // Amount paid (50, 100, or 500)
    private Integer tokens; // Tokens to be credited (10, 25, or 150)
    private String transactionId; // bKash transaction ID provided by user
    private String subscriptionPackage; // "basic" (50tk/10tokens), "standard" (100tk/25tokens), "premium" (500tk/150tokens)
    private String status; // "pending", "approved", "rejected"
    private Date createdAt;
    private Date updatedAt;
    private String adminNotes; // Optional notes from admin during approval/rejection
    private String userEmail; // Email of the user for reference
    private String userName; // Name of the user for reference
    
    // Default constructor
    public Subscription() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "pending";
    }
    
    // Constructor with required fields
    public Subscription(String userId, String userType, String userEmail, String userName, 
                       Double amount, Integer tokens, String transactionId, String subscriptionPackage) {
        this();
        this.userId = userId;
        this.userType = userType;
        this.userEmail = userEmail;
        this.userName = userName;
        this.amount = amount;
        this.tokens = tokens;
        this.transactionId = transactionId;
        this.subscriptionPackage = subscriptionPackage;
    }
    
    // Getters and Setters
    public ObjectId getId() {
        return _id;
    }
    
    public void setId(ObjectId _id) {
        this._id = _id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public Integer getTokens() {
        return tokens;
    }
    
    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getSubscriptionPackage() {
        return subscriptionPackage;
    }
    
    public void setSubscriptionPackage(String subscriptionPackage) {
        this.subscriptionPackage = subscriptionPackage;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = new Date(); // Update timestamp when status changes
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
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
        this.updatedAt = new Date(); // Update timestamp when notes are added
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    // Helper method to get package details
    public static String getPackageDetails(String packageType) {
        switch (packageType) {
            case "basic":
                return "50 TK - 10 Tokens";
            case "standard":
                return "100 TK - 25 Tokens";
            case "premium":
                return "500 TK - 150 Tokens";
            default:
                return "Unknown Package";
        }
    }
    
    // Helper method to validate package and get corresponding amount/tokens
    public static boolean isValidPackage(String packageType, Double amount, Integer tokens) {
        switch (packageType) {
            case "basic":
                return amount == 50.0 && tokens == 10;
            case "standard":
                return amount == 100.0 && tokens == 25;
            case "premium":
                return amount == 500.0 && tokens == 150;
            default:
                return false;
        }
    }
}