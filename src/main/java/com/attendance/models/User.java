package com.attendance.models;

import java.time.LocalDateTime;

/**
 * User model representing a registered person in the system
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String department;
    private byte[] faceEncoding;
    private String imagePath;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String name, String email, String department) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public byte[] getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(byte[] faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + department + ")";
    }
}
