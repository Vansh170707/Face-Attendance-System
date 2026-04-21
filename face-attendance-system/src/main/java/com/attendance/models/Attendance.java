package com.attendance.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Attendance record model
 */
public class Attendance {
    private int id;
    private int userId;
    private String userName;
    private String department;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;

    public Attendance() {
        this.status = "PRESENT";
    }

    public Attendance(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.checkIn = LocalDateTime.now();
        this.status = "PRESENT";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDateTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDateTime checkOut) {
        this.checkOut = checkOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedCheckIn() {
        return checkIn != null ? checkIn.format(DateTimeFormatter.ofPattern("hh:mm a")) : "-";
    }

    public String getFormattedCheckOut() {
        return checkOut != null ? checkOut.format(DateTimeFormatter.ofPattern("hh:mm a")) : "-";
    }

    public String getFormattedDate() {
        return checkIn != null ? checkIn.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "-";
    }
}
