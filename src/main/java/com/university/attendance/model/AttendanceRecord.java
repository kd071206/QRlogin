package com.university.attendance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    private String id;
    private String userId;
    private String sessionId;
    private ActionType actionType;
    private LocalDateTime timestamp;
    private String qrCodeData;
    private String cameraDevice;

    public enum ActionType {
        LOGIN, LOGOUT
    }

    public AttendanceRecord(String userId, String sessionId, ActionType actionType,
                           String qrCodeData, String cameraDevice) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.sessionId = sessionId;
        this.actionType = actionType;
        this.timestamp = LocalDateTime.now();
        this.qrCodeData = qrCodeData;
        this.cameraDevice = cameraDevice;
    }

    public boolean isLogin() {
        return ActionType.LOGIN.equals(actionType);
    }

    public boolean isLogout() {
        return ActionType.LOGOUT.equals(actionType);
    }

    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
               sessionId != null && !sessionId.trim().isEmpty() &&
               actionType != null &&
               qrCodeData != null && !qrCodeData.trim().isEmpty();
    }

    public String getDisplayTimestamp() {
        if (timestamp == null) {
            return "N/A";
        }
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}