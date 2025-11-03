package com.university.attendance.service;

import com.university.attendance.model.AttendanceRecord;
import com.university.attendance.model.AttendanceSession;
import com.university.attendance.model.User;
import com.university.attendance.service.QRCodeService.QRCodeData;
import com.university.attendance.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class AttendanceService {
    private final DatabaseService databaseService;
    private final QRCodeService qrCodeService;
    private final Properties appConfig;
    private int maxSessionHours;
    private int minSessionMinutes;

    public AttendanceService() {
        this.databaseService = new DatabaseService();
        this.qrCodeService = new QRCodeService();
        this.appConfig = loadAppConfig();
        this.maxSessionHours = Integer.parseInt(appConfig.getProperty("app.session.max_hours", "12"));
        this.minSessionMinutes = Integer.parseInt(appConfig.getProperty("app.session.min_minutes", "1"));
    }

    private Properties loadAppConfig() {
        Properties props = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("config/app.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            log.warn("Could not load app configuration, using defaults", e);
        }
        return props;
    }

    public AttendanceResult processAttendance(QRCodeData qrData, String cameraDevice) {
        try {
            log.info("Processing attendance for student: {}, action: {}", qrData.getStudentId(), qrData.getAction());

            if (!isQRCodeTimeValid(qrData.getTimestamp())) {
                return AttendanceResult.error("QR code timestamp is too old or in the future");
            }

            Optional<User> userOpt = databaseService.findUserByStudentId(qrData.getStudentId());
            if (userOpt.isEmpty()) {
                return AttendanceResult.error("Student not found: " + qrData.getStudentId());
            }

            User user = userOpt.get();
            if (!user.isActive()) {
                return AttendanceResult.error("Student account is inactive: " + qrData.getStudentId());
            }

            if (qrData.isLogin()) {
                return processLogin(user, qrData, cameraDevice);
            } else if (qrData.isLogout()) {
                return processLogout(user, qrData, cameraDevice);
            } else {
                return AttendanceResult.error("Invalid action type in QR code: " + qrData.getAction());
            }

        } catch (Exception e) {
            log.error("Error processing attendance for student: {}", qrData.getStudentId(), e);
            return AttendanceResult.error("System error: " + e.getMessage());
        }
    }

    private AttendanceResult processLogin(User user, QRCodeData qrData, String cameraDevice) {
        Optional<AttendanceSession> activeSessionOpt = databaseService.findActiveSession(user.getId());

        if (activeSessionOpt.isPresent()) {
            AttendanceSession existingSession = activeSessionOpt.get();
            if (isRecentLogin(existingSession.getLoginTime(), 5)) {
                return AttendanceResult.error("User already logged in recently at: " +
                        TimeUtil.formatTime(existingSession.getLoginTime()));
            } else {
                existingSession.markAsAbnormal();
                databaseService.saveAttendanceSession(existingSession);
                log.info("Marked previous active session as abnormal for user: {}", user.getStudentId());
            }
        }

        AttendanceSession newSession = new AttendanceSession(user.getId(), LocalDateTime.now());
        if (!databaseService.saveAttendanceSession(newSession)) {
            return AttendanceResult.error("Failed to create new attendance session");
        }

        AttendanceRecord loginRecord = new AttendanceRecord(
                user.getId(),
                newSession.getId(),
                AttendanceRecord.ActionType.LOGIN,
                qrData.getRawData(),
                cameraDevice
        );

        if (!databaseService.saveAttendanceRecord(loginRecord)) {
            return AttendanceResult.error("Failed to save login record");
        }

        log.info("Login processed successfully for user: {}", user.getStudentId());

        return AttendanceResult.success(
                "Login successful",
                user,
                newSession,
                loginRecord
        );
    }

    private AttendanceResult processLogout(User user, QRCodeData qrData, String cameraDevice) {
        Optional<AttendanceSession> activeSessionOpt = databaseService.findActiveSession(user.getId());

        if (activeSessionOpt.isEmpty()) {
            return AttendanceResult.error("No active session found for user: " + user.getStudentId() +
                    ". Please login first.");
        }

        AttendanceSession session = activeSessionOpt.get();
        LocalDateTime logoutTime = LocalDateTime.now();

        session.completeSession(logoutTime);

        if (!session.isValidDuration(minSessionMinutes, maxSessionHours)) {
            if (session.getDurationMinutes() < minSessionMinutes) {
                return AttendanceResult.error("Session too short. Minimum duration: " + minSessionMinutes + " minutes");
            } else {
                log.warn("Session duration exceeds maximum for user: {}, duration: {} minutes",
                        user.getStudentId(), session.getDurationMinutes());
                session.markAsAbnormal();
            }
        }

        if (!databaseService.saveAttendanceSession(session)) {
            return AttendanceResult.error("Failed to update attendance session");
        }

        AttendanceRecord logoutRecord = new AttendanceRecord(
                user.getId(),
                session.getId(),
                AttendanceRecord.ActionType.LOGOUT,
                qrData.getRawData(),
                cameraDevice
        );

        if (!databaseService.saveAttendanceRecord(logoutRecord)) {
            return AttendanceResult.error("Failed to save logout record");
        }

        log.info("Logout processed successfully for user: {}, duration: {} minutes",
                user.getStudentId(), session.getDurationMinutes());

        return AttendanceResult.success(
                "Logout successful",
                user,
                session,
                logoutRecord
        );
    }

    private boolean isQRCodeTimeValid(LocalDateTime qrTimestamp) {
        if (qrTimestamp == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(30);

        return !qrTimestamp.isBefore(cutoff) && !qrTimestamp.isAfter(now.plusMinutes(5));
    }

    private boolean isRecentLogin(LocalDateTime loginTime, int windowMinutes) {
        return TimeUtil.isWithinTimeWindow(LocalDateTime.now(), loginTime, windowMinutes);
    }

    public List<AttendanceSession> getTodaySessions() {
        LocalDateTime startOfDay = TimeUtil.getStartOfDay(LocalDateTime.now());
        LocalDateTime endOfDay = TimeUtil.getEndOfDay(LocalDateTime.now());

        return databaseService.getSessionsByDateRange(
                startOfDay.toLocalDate(),
                endOfDay.toLocalDate()
        );
    }

    public List<AttendanceSession> getSessionsByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return databaseService.getSessionsByDateRange(startDate, endDate);
    }

    public List<User> getAllActiveUsers() {
        return databaseService.getAllActiveUsers();
    }

    public boolean createTestUser(String studentId, String name, String email, String department, int yearLevel) {
        User user = new User(studentId, name, email, department, yearLevel);
        return databaseService.saveUser(user);
    }

    public String generateUserQRCode(User user, String action) {
        QRCodeData qrData = qrCodeService.generateQRCodeData(user, action);
        if (qrData != null) {
            return qrData.getRawData();
        }
        return null;
    }

    public AttendanceStats getTodayStats() {
        List<AttendanceSession> todaySessions = getTodaySessions();

        int totalUsers = (int) todaySessions.stream()
                .map(AttendanceSession::getUserId)
                .distinct()
                .count();

        long activeSessions = todaySessions.stream()
                .filter(AttendanceSession::isActive)
                .count();

        long completedSessions = todaySessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED)
                .count();

        double averageDuration = todaySessions.stream()
                .filter(s -> s.getDurationMinutes() != null && s.getDurationMinutes() > 0)
                .mapToInt(AttendanceSession::getDurationMinutes)
                .average()
                .orElse(0.0);

        return new AttendanceStats(totalUsers, (int) activeSessions, (int) completedSessions, averageDuration);
    }

    public boolean testDatabaseConnection() {
        return databaseService.testConnection();
    }

    public void cleanup() {
        qrCodeService.cleanupOldScans();
    }

    public static class AttendanceResult {
        private final boolean success;
        private final String message;
        private final User user;
        private final AttendanceSession session;
        private final AttendanceRecord record;

        private AttendanceResult(boolean success, String message, User user, AttendanceSession session, AttendanceRecord record) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.session = session;
            this.record = record;
        }

        public static AttendanceResult success(String message, User user, AttendanceSession session, AttendanceRecord record) {
            return new AttendanceResult(true, message, user, session, record);
        }

        public static AttendanceResult error(String message) {
            return new AttendanceResult(false, message, null, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
        public AttendanceSession getSession() { return session; }
        public AttendanceRecord getRecord() { return record; }

        public String getSuccessMessage() {
            if (!success || user == null) {
                return message;
            }

            if (session != null && session.getLogoutTime() != null) {
                return String.format("%s\nStudent: %s (%s)\nDuration: %s",
                        message,
                        user.getName(),
                        user.getStudentId(),
                        TimeUtil.formatDuration(session.getDurationMinutes())
                );
            } else {
                return String.format("%s\nStudent: %s (%s)",
                        message,
                        user.getName(),
                        user.getStudentId()
                );
            }
        }
    }

    public static class AttendanceStats {
        private final int totalUsers;
        private final int activeSessions;
        private final int completedSessions;
        private final double averageDuration;

        public AttendanceStats(int totalUsers, int activeSessions, int completedSessions, double averageDuration) {
            this.totalUsers = totalUsers;
            this.activeSessions = activeSessions;
            this.completedSessions = completedSessions;
            this.averageDuration = averageDuration;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getActiveSessions() { return activeSessions; }
        public int getCompletedSessions() { return completedSessions; }
        public double getAverageDuration() { return averageDuration; }
        public String getFormattedAverageDuration() { return TimeUtil.formatDuration((int) averageDuration); }
    }
}