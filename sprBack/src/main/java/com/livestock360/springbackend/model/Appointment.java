package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "appointments")
public class Appointment {
    @Id
    private ObjectId id;
    private ObjectId farmerId;
    private ObjectId vetId;
    private ObjectId animalId; // Optional - for farmer-created appointments
    private String animalName; // Optional - for vet-created appointments
    private String appointmentType; // "consultation", "checkup", "surgery", etc.
    private String priority; // "normal", "urgent", "emergency"
    private Date scheduledDate;
    private String scheduledTime; // "09:00", "14:30", etc.
    private Integer duration; // Duration in minutes
    private String symptoms; // Primary complaint/reason
    private String description; // Additional details/notes
    private String status; // "pending", "accepted", "in-progress", "completed", "cancelled"
    
    // Vet-filled fields after appointment
    private String diagnosis;
    private String treatment;
    private String prescription;
    private String vetNotes;
    private Boolean followUpRequired;
    private Date followUpDate;
    
    // Cancellation fields
    private String cancelledBy; // "farmer" or "vet"
    private String cancellationReason;
    private Date cancelledAt;
    
    // Timestamps
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Appointment() {}

    public Appointment(ObjectId farmerId, ObjectId vetId, Date scheduledDate, String scheduledTime, String symptoms) {
        this.farmerId = farmerId;
        this.vetId = vetId;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.symptoms = symptoms;
        this.status = "pending";
        this.appointmentType = "consultation";
        this.priority = "normal";
        this.duration = 30;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(ObjectId farmerId) {
        this.farmerId = farmerId;
    }

    public ObjectId getVetId() {
        return vetId;
    }

    public void setVetId(ObjectId vetId) {
        this.vetId = vetId;
    }

    public ObjectId getAnimalId() {
        return animalId;
    }

    public void setAnimalId(ObjectId animalId) {
        this.animalId = animalId;
    }

    public String getAnimalName() {
        return animalName;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getVetNotes() {
        return vetNotes;
    }

    public void setVetNotes(String vetNotes) {
        this.vetNotes = vetNotes;
    }

    public Boolean getFollowUpRequired() {
        return followUpRequired;
    }

    public void setFollowUpRequired(Boolean followUpRequired) {
        this.followUpRequired = followUpRequired;
    }

    public Date getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(Date followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
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