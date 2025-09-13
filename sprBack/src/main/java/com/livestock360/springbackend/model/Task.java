package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;

import java.util.Date;

public class Task {
    private ObjectId _id;
    private String title;
    private String description;
    private Date dueDate;
    private String dueTime;
    private Double estimatedCost;
    private String priority; // "low", "medium", "high"
    private String status; // "pending", "in-progress", "completed"
    private String category; // "feeding", "medical", "cleaning", "breeding", "other"
    private ObjectId farmer;
    private ObjectId animal; // Optional - link to specific animal
    private Boolean isCompleted;
    private String notes;
    private Date createdAt;
    private Date updatedAt;
    private Date completedAt;

    // Constructors
    public Task() {}

    public Task(String title, String description, Date dueDate, String dueTime, 
               Double estimatedCost, String priority, String status, String category,
               ObjectId farmer, ObjectId animal, Boolean isCompleted, String notes) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.estimatedCost = estimatedCost;
        this.priority = priority;
        this.status = status;
        this.category = category;
        this.farmer = farmer;
        this.animal = animal;
        this.isCompleted = isCompleted;
        this.notes = notes;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getDueTime() {
        return dueTime;
    }

    public void setDueTime(String dueTime) {
        this.dueTime = dueTime;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ObjectId getFarmer() {
        return farmer;
    }

    public void setFarmer(ObjectId farmer) {
        this.farmer = farmer;
    }

    public ObjectId getAnimal() {
        return animal;
    }

    public void setAnimal(ObjectId animal) {
        this.animal = animal;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }
}