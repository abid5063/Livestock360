package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;
import java.util.Date;

public class Vaccine {
    private ObjectId _id;
    private String vaccine_name;
    private ObjectId animal;
    private String animal_name;
    private Date vaccine_date;
    private String notes;
    private Date next_due_date;
    private ObjectId farmer;
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Vaccine() {}

    public Vaccine(String vaccine_name, ObjectId animal, String animal_name, Date vaccine_date, 
                   String notes, Date next_due_date, ObjectId farmer) {
        this.vaccine_name = vaccine_name;
        this.animal = animal;
        this.animal_name = animal_name;
        this.vaccine_date = vaccine_date;
        this.notes = notes;
        this.next_due_date = next_due_date;
        this.farmer = farmer;
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

    public String getVaccineName() {
        return vaccine_name;
    }

    public void setVaccineName(String vaccine_name) {
        this.vaccine_name = vaccine_name;
    }

    public ObjectId getAnimal() {
        return animal;
    }

    public void setAnimal(ObjectId animal) {
        this.animal = animal;
    }

    public String getAnimalName() {
        return animal_name;
    }

    public void setAnimalName(String animal_name) {
        this.animal_name = animal_name;
    }

    public Date getVaccineDate() {
        return vaccine_date;
    }

    public void setVaccineDate(Date vaccine_date) {
        this.vaccine_date = vaccine_date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getNextDueDate() {
        return next_due_date;
    }

    public void setNextDueDate(Date next_due_date) {
        this.next_due_date = next_due_date;
    }

    public ObjectId getFarmer() {
        return farmer;
    }

    public void setFarmer(ObjectId farmer) {
        this.farmer = farmer;
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