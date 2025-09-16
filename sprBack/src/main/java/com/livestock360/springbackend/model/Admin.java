package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;

import java.util.Date;

public class Admin {
    private ObjectId _id;
    private String email;
    private String password; // Hashed password
    private String salt; // Salt for password hashing
    private String name;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    private Date lastLoginAt;
    private String role; // "admin", "super_admin" for future extensions
    
    // Default constructor
    public Admin() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isActive = true;
        this.role = "admin";
    }
    
    // Constructor with required fields
    public Admin(String email, String password, String salt, String name) {
        this();
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.name = name;
    }
    
    // Getters and Setters
    public ObjectId getId() {
        return _id;
    }
    
    public void setId(ObjectId _id) {
        this._id = _id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getSalt() {
        return salt;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
    
    public Date getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    // Helper method to update last login
    public void updateLastLogin() {
        this.lastLoginAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Helper method to check if admin is default admin
    public boolean isDefaultAdmin() {
        return "admin@gmail.com".equals(this.email);
    }
}