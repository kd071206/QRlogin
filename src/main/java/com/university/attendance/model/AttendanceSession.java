package com.university.attendance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {
    private String id;
    private String userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private Integer durationMinutes;
    private LocalDate sessionDate;
    private SessionStatus status;
    private LocalDateTime createdAt;

    public enum SessionStatus {
        ACTIVE, COMPLETED, ABNORMAL
    }

    public AttendanceSession(String userId, LocalDateTime loginTime) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.loginTime = loginTime;
        this.sessionDate = loginTime.toLocalDate();
        this.status = SessionStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void completeSession(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
        this.durationMinutes = calculateDuration(loginTime, logoutTime);
        this.status = SessionStatus.COMPLETED;
    }

    public void markAsAbnormal() {
        this.status = SessionStatus.ABNORMAL;
    }

    private int calculateDuration(LocalDateTime login, LocalDateTime logout) {
        if (login == null || logout == null) {
            return 0;
        }

        Duration duration = Duration.between(login, logout);
        return (int) Math.ceil(duration.toMinutes());
    }

    public boolean isActive() {
        return SessionStatus.ACTIVE.equals(status);
    }

    public boolean isValidDuration(int minMinutes, int maxHours) {
        if (durationMinutes == null) {
            return false;
        }

        int maxMinutes = maxHours * 60;
        return durationMinutes >= minMinutes && durationMinutes <= maxMinutes;
    }

    public long getActiveDurationMinutes() {
        if (loginTime == null) {
            return 0;
        }

        LocalDateTime endTime = logoutTime != null ? logoutTime : LocalDateTime.now();
        return Duration.between(loginTime, endTime).toMinutes();
    }
}