package com.nima.tempconv.dto;

import com.nima.tempconv.model.User;

public class UserResponse {

    private String googleId;
    private String email;
    private String name;
    private String picture;

    public UserResponse() {
    }

    public UserResponse(String googleId, String email, String name, String picture) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getGoogleId(), user.getEmail(), user.getName(), user.getPicture());
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
