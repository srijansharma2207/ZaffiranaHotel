package com.hotel.model;

import java.time.LocalDateTime;

public class AppUser {
    private int userId;
    private String email;
    private String fullName;
    private String phone;
    private Integer guestId;
    private String role;
    private LocalDateTime createdAt;

    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Integer getGuestId() { return guestId; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGuestId(Integer guestId) { this.guestId = guestId; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
