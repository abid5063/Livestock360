package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;

public class Vet {
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
    private String specialization;
    private String specialty;
    private String licenseNumber;
    private String userType;
    private String phoneNo; // Alternative to phone for compatibility
    private String experience;
    private String profileImage; // Alternative to profilePicture for compatibility
    private Boolean isVerified;
    private Boolean isActive;
    private Double rating;
    private Integer totalReviews;
    private String registrationDate;
    private String createdAt;
    private String updatedAt;
    private Integer totalAppointments;
    private Integer completedAppointments;
    private Integer cancelledAppointments;
    private Double consultationFee;
    private String bio;
    private Double latitude;
    private Double longitude;

    // Constructors
    public Vet() {}

    public Vet(String name, String email, String password, String salt, String phone, 
               String location, String address, String profilePicture, String dateJoined,
               String specialization, Double latitude, Double longitude) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.phone = phone;
        this.location = location;
        this.address = address;
        this.profilePicture = profilePicture;
        this.dateJoined = dateJoined;
        this.specialization = specialization;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(Integer totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public Integer getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(Integer completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public Integer getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(Integer cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
    }

    public Double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
