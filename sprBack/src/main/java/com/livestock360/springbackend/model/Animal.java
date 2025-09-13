package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;
import java.util.Date;

public class Animal {
    private ObjectId _id;
    private String name;
    private String type;
    private String breed;
    private Integer age;
    private String gender;
    private String details;
    private String photo_url;
    private ObjectId farmer; // Reference to farmer ObjectId
    private Date createdAt;

    // Constructors
    public Animal() {}

    public Animal(String name, String type, String breed, Integer age, String gender, 
                  String details, String photo_url, ObjectId farmer, Date createdAt) {
        this.name = name;
        this.type = type;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.details = details;
        this.photo_url = photo_url;
        this.farmer = farmer;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public ObjectId getId() {
        return _id;
    }

    public void setId(ObjectId _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
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
}