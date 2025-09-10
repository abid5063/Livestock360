package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;

public class Farmer {
    private ObjectId _id;
    private String name;
    private String email;
    private String password;
    private String salt;
    private String phone;
    private String location;
    private String address;
    private String profilePicture;
    private String dateJoined;

    // Constructors
    public Farmer() {}

    public Farmer(String name, String email, String password, String salt, String phone, 
                 String location, String address, String profilePicture, String dateJoined) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.phone = phone;
        this.location = location;
        this.address = address;
        this.profilePicture = profilePicture;
        this.dateJoined = dateJoined;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(String dateJoined) {
        this.dateJoined = dateJoined;
    }
}
