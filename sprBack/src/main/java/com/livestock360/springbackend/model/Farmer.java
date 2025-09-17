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
    private Integer tokenCount; // Token balance for subscriptions
    
    // Product selling fields
    private Boolean isSeller; // Whether farmer sells products
    private Boolean sellsMilkCow; // Sells cow milk
    private Boolean sellsMilkBuffalo; // Sells buffalo milk
    private Boolean sellsMilkGoat; // Sells goat milk
    private Boolean sellsButter; // Sells butter
    private Boolean sellsHenEggs; // Sells hen eggs
    private Boolean sellsDuckEggs; // Sells duck eggs

    // Constructors
    public Farmer() {
        this.tokenCount = 0; // Default token count is 0
        // Initialize product selling fields to false by default
        this.isSeller = false;
        this.sellsMilkCow = false;
        this.sellsMilkBuffalo = false;
        this.sellsMilkGoat = false;
        this.sellsButter = false;
        this.sellsHenEggs = false;
        this.sellsDuckEggs = false;
    }

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
        this.tokenCount = 0; // Default token count is 0
        // Initialize product selling fields to false by default
        this.isSeller = false;
        this.sellsMilkCow = false;
        this.sellsMilkBuffalo = false;
        this.sellsMilkGoat = false;
        this.sellsButter = false;
        this.sellsHenEggs = false;
        this.sellsDuckEggs = false;
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

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    // Helper methods for token management
    public void addTokens(Integer tokens) {
        if (tokens != null && tokens > 0) {
            this.tokenCount = (this.tokenCount != null ? this.tokenCount : 0) + tokens;
        }
    }
    
    public boolean deductTokens(Integer tokens) {
        if (tokens != null && tokens > 0 && this.tokenCount != null && this.tokenCount >= tokens) {
            this.tokenCount -= tokens;
            return true;
        }
        return false;
    }
    
    public boolean hasEnoughTokens(Integer requiredTokens) {
        return this.tokenCount != null && requiredTokens != null && this.tokenCount >= requiredTokens;
    }
    
    // Product selling getters and setters
    public Boolean getIsSeller() {
        return isSeller;
    }

    public void setIsSeller(Boolean isSeller) {
        this.isSeller = isSeller != null ? isSeller : false;
    }

    public Boolean getSellsMilkCow() {
        return sellsMilkCow;
    }

    public void setSellsMilkCow(Boolean sellsMilkCow) {
        this.sellsMilkCow = sellsMilkCow != null ? sellsMilkCow : false;
    }

    public Boolean getSellsMilkBuffalo() {
        return sellsMilkBuffalo;
    }

    public void setSellsMilkBuffalo(Boolean sellsMilkBuffalo) {
        this.sellsMilkBuffalo = sellsMilkBuffalo != null ? sellsMilkBuffalo : false;
    }

    public Boolean getSellsMilkGoat() {
        return sellsMilkGoat;
    }

    public void setSellsMilkGoat(Boolean sellsMilkGoat) {
        this.sellsMilkGoat = sellsMilkGoat != null ? sellsMilkGoat : false;
    }

    public Boolean getSellsButter() {
        return sellsButter;
    }

    public void setSellsButter(Boolean sellsButter) {
        this.sellsButter = sellsButter != null ? sellsButter : false;
    }

    public Boolean getSellsHenEggs() {
        return sellsHenEggs;
    }

    public void setSellsHenEggs(Boolean sellsHenEggs) {
        this.sellsHenEggs = sellsHenEggs != null ? sellsHenEggs : false;
    }

    public Boolean getSellsDuckEggs() {
        return sellsDuckEggs;
    }

    public void setSellsDuckEggs(Boolean sellsDuckEggs) {
        this.sellsDuckEggs = sellsDuckEggs != null ? sellsDuckEggs : false;
    }
    
    // Helper methods for product management
    public void enableSelling() {
        this.isSeller = true;
    }
    
    public void disableSelling() {
        this.isSeller = false;
        // Optionally clear all product flags when disabling selling
        this.sellsMilkCow = false;
        this.sellsMilkBuffalo = false;
        this.sellsMilkGoat = false;
        this.sellsButter = false;
        this.sellsHenEggs = false;
        this.sellsDuckEggs = false;
    }
    
    public boolean sellsAnyProduct() {
        return (isSeller != null && isSeller) && 
               (Boolean.TRUE.equals(sellsMilkCow) || 
                Boolean.TRUE.equals(sellsMilkBuffalo) || 
                Boolean.TRUE.equals(sellsMilkGoat) ||
                Boolean.TRUE.equals(sellsButter) || 
                Boolean.TRUE.equals(sellsHenEggs) || 
                Boolean.TRUE.equals(sellsDuckEggs));
    }
    
    public int getProductCount() {
        int count = 0;
        if (Boolean.TRUE.equals(sellsMilkCow)) count++;
        if (Boolean.TRUE.equals(sellsMilkBuffalo)) count++;
        if (Boolean.TRUE.equals(sellsMilkGoat)) count++;
        if (Boolean.TRUE.equals(sellsButter)) count++;
        if (Boolean.TRUE.equals(sellsHenEggs)) count++;
        if (Boolean.TRUE.equals(sellsDuckEggs)) count++;
        return count;
    }
}
