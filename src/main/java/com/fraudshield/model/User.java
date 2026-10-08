package com.fraudshield.model;

import java.time.LocalDateTime;

/**
 * A person known to the system: console staff (ADMIN / ANALYST) or a monitored
 * account holder (CUSTOMER). Fields are private (encapsulation) and validated in setters.
 */
public class User {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_ANALYST = "ANALYST";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String role = ROLE_CUSTOMER;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String name, String email, String passwordHash, String phone, String role) {
        setName(name);
        setEmail(email);
        this.passwordHash = passwordHash;
        this.phone = phone;
        setRole(role);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        this.name = name.trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("A valid e-mail address is required");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) {
        if (!ROLE_ADMIN.equals(role) && !ROLE_ANALYST.equals(role) && !ROLE_CUSTOMER.equals(role)) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
        this.role = role;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isStaff() { return ROLE_ADMIN.equals(role) || ROLE_ANALYST.equals(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', role=" + role + "}";
    }
}
