package com.university.attendance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String studentId;
    private String name;
    private String email;
    private String department;
    private Integer yearLevel;
    private String qrCodeData;
    private LocalDateTime createdAt;
    private boolean isActive;

    public User(String studentId, String name, String email, String department, Integer yearLevel, String qrCodeData) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.yearLevel = yearLevel;
        this.qrCodeData = qrCodeData;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public User(String studentId, String name, String email, String department, Integer yearLevel) {
        this(studentId, name, email, department, yearLevel, generateQRCodeData(studentId, name));
    }

    private static String generateQRCodeData(String studentId, String name) {
        return String.format("{\"studentId\":\"%s\",\"name\":\"%s\",\"type\":\"attendance\"}",
                           studentId, name);
    }

    public boolean isValid() {
        return studentId != null && !studentId.trim().isEmpty() &&
               name != null && !name.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               qrCodeData != null && !qrCodeData.trim().isEmpty();
    }
}