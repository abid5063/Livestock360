package com.livestock360.springbackend.dto;

public class RefreshRequest {
    private String token;

    // Constructors
    public RefreshRequest() {}

    public RefreshRequest(String token) {
        this.token = token;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
