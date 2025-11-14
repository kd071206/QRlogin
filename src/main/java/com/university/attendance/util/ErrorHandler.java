package com.university.attendance.util;

import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ErrorHandler {

    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ErrorCategory {
        DATABASE, CAMERA, QR_CODE, VALIDATION, UI, SYSTEM, NETWORK, FILE_IO
    }

    public static class ErrorReport {
        private final String id;
        private final LocalDateTime timestamp;
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String message;
        private final String details;
        private final String stackTrace;
        private final String userId;
        private final String sessionId;

        public ErrorReport(String id, LocalDateTime timestamp, ErrorCategory category, ErrorSeverity severity,
                          String message, String details, String stackTrace, String userId, String sessionId) {
            this.id = id;
            this.timestamp = timestamp;
            this.category = category;
            this.severity = severity;
            this.message = message;
            this.details = details;
            this.stackTrace = stackTrace;
            this.userId = userId;
            this.sessionId = sessionId;
        }

        public String getId() { return id; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public ErrorCategory getCategory() { return category; }
        public ErrorSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        public String getStackTrace() { return stackTrace; }
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }

        public String getFormattedSummary() {
            return String.format("[%s] %s: %s", severity, category, message);
        }

        public String getFullReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("Error Report: ").append(id).append("\n");
            sb.append("Timestamp: ").append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            sb.append("Category: ").append(category).append("\n");
            sb.append("Severity: ").append(severity).append("\n");
            sb.append("Message: ").append(message).append("\n");
            if (details != null && !details.isEmpty()) {
                sb.append("Details: ").append(details).append("\n");
            }
            if (userId != null) {
                sb.append("User ID: ").append(userId).append("\n");
            }
            if (sessionId != null) {
                sb.append("Session ID: ").append(sessionId).append("\n");
            }
            if (stackTrace != null && !stackTrace.isEmpty()) {
                sb.append("Stack Trace:\n").append(stackTrace).append("\n");
            }
            return sb.toString();
        }
    }

    private static final ConcurrentHashMap<String, ErrorReport> errorReports = new ConcurrentHashMap<>();
    private static final List<ErrorReport> recentErrors = new ArrayList<>();
    private static final AtomicInteger errorCounter = new AtomicInteger(0);
    private static final int MAX_RECENT_ERRORS = 100;

    private ErrorHandler() {
        // Utility class - prevent instantiation
    }

    public static String handleError(ErrorCategory category, ErrorSeverity severity, String message, Throwable throwable) {
        return handleError(category, severity, message, null, throwable, null, null);
    }

    public static String handleError(ErrorCategory category, ErrorSeverity severity, String message, String details, Throwable throwable) {
        return handleError(category, severity, message, details, throwable, null, null);
    }

    public static String handleError(ErrorCategory category, ErrorSeverity severity, String message, String details, Throwable throwable, String userId, String sessionId) {
        String errorId = generateErrorId();
        String stackTrace = throwable != null ? getStackTrace(throwable) : null;

        ErrorReport report = new ErrorReport(errorId, LocalDateTime.now(), category, severity, message, details, stackTrace, userId, sessionId);

        errorReports.put(errorId, report);
        addToRecentErrors(report);

        log.error("Error [{}]: {} - {}", errorId, category, message, throwable);

        if (severity == ErrorSeverity.CRITICAL) {
            handleCriticalError(report);
        }

        return errorId;
    }

    public static void handleDatabaseError(String operation, Throwable throwable) {
        handleError(ErrorCategory.DATABASE, ErrorSeverity.HIGH,
                   "Database operation failed: " + operation,
                   "Database error during: " + operation,
                   throwable);
    }

    public static void handleCameraError(String operation, Throwable throwable) {
        handleError(ErrorCategory.CAMERA, ErrorSeverity.MEDIUM,
                   "Camera operation failed: " + operation,
                   "Camera error during: " + operation,
                   throwable);
    }

    public static void handleQRCodeError(String operation, Throwable throwable) {
        handleError(ErrorCategory.QR_CODE, ErrorSeverity.MEDIUM,
                   "QR code operation failed: " + operation,
                   "QR code error during: " + operation,
                   throwable);
    }

    public static void handleValidationError(String field, String value, String reason) {
        handleError(ErrorCategory.VALIDATION, ErrorSeverity.LOW,
                   "Validation failed for field: " + field,
                   "Field: " + field + ", Value: " + value + ", Reason: " + reason,
                   null);
    }

    public static void handleUIError(String component, String operation, Throwable throwable) {
        handleError(ErrorCategory.UI, ErrorSeverity.LOW,
                   "UI operation failed: " + operation + " on component: " + component,
                   "UI error during: " + operation,
                   throwable);
    }

    public static void handleSystemError(String operation, Throwable throwable) {
        handleError(ErrorCategory.SYSTEM, ErrorSeverity.HIGH,
                   "System operation failed: " + operation,
                   "System error during: " + operation,
                   throwable);
    }

    public static void handleNetworkError(String operation, Throwable throwable) {
        handleError(ErrorCategory.NETWORK, ErrorSeverity.MEDIUM,
                   "Network operation failed: " + operation,
                   "Network error during: " + operation,
                   throwable);
    }

    public static void handleFileError(String operation, String filePath, Throwable throwable) {
        handleError(ErrorCategory.FILE_IO, ErrorSeverity.MEDIUM,
                   "File operation failed: " + operation,
                   "File: " + filePath + ", Operation: " + operation,
                   throwable);
    }

    private static void handleCriticalError(ErrorReport report) {
        log.error("CRITICAL ERROR DETECTED: {}", report.getFullReport());

        try {
            String filename = String.format("critical_error_%s.log",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            java.nio.file.Path logFile = java.nio.file.Paths.get("logs", filename);
            java.nio.file.Files.createDirectories(logFile.getParent());

            java.nio.file.Files.write(logFile, report.getFullReport().getBytes());

        } catch (Exception e) {
            log.error("Failed to write critical error report to file", e);
        }
    }

    public static ErrorReport getErrorReport(String errorId) {
        return errorReports.get(errorId);
    }

    public static List<ErrorReport> getRecentErrors() {
        synchronized (recentErrors) {
            return new ArrayList<>(recentErrors);
        }
    }

    public static List<ErrorReport> getErrorsByCategory(ErrorCategory category) {
        return recentErrors.stream()
                .filter(report -> report.getCategory() == category)
                .toList();
    }

    public static List<ErrorReport> getErrorsBySeverity(ErrorSeverity severity) {
        return recentErrors.stream()
                .filter(report -> report.getSeverity() == severity)
                .toList();
    }

    public static List<ErrorReport> getErrorsByUser(String userId) {
        return recentErrors.stream()
                .filter(report -> userId.equals(report.getUserId()))
                .toList();
    }

    public static ErrorStatistics getErrorStatistics() {
        synchronized (recentErrors) {
            return new ErrorStatistics(recentErrors);
        }
    }

    public static void clearOldErrors(int hoursToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursToKeep);

        synchronized (recentErrors) {
            recentErrors.removeIf(report -> report.getTimestamp().isBefore(cutoff));
        }

        errorReports.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));

        log.info("Cleared error reports older than {} hours", hoursToKeep);
    }

    private static String generateErrorId() {
        return String.format("ERR-%d-%s", errorCounter.incrementAndGet(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
    }

    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private static void addToRecentErrors(ErrorReport report) {
        synchronized (recentErrors) {
            recentErrors.add(report);

            while (recentErrors.size() > MAX_RECENT_ERRORS) {
                recentErrors.remove(0);
            }
        }
    }

    public static class ErrorStatistics {
        private final int totalErrors;
        private final int criticalErrors;
        private final int highErrors;
        private final int mediumErrors;
        private final int lowErrors;
        private final int databaseErrors;
        private final int cameraErrors;
        private final int qrCodeErrors;
        private final int validationErrors;
        private final int uiErrors;
        private final int systemErrors;
        private final int networkErrors;
        private final int fileErrors;

        public ErrorStatistics(List<ErrorReport> errors) {
            this.totalErrors = errors.size();

            this.criticalErrors = (int) errors.stream()
                    .filter(e -> e.getSeverity() == ErrorSeverity.CRITICAL)
                    .count();

            this.highErrors = (int) errors.stream()
                    .filter(e -> e.getSeverity() == ErrorSeverity.HIGH)
                    .count();

            this.mediumErrors = (int) errors.stream()
                    .filter(e -> e.getSeverity() == ErrorSeverity.MEDIUM)
                    .count();

            this.lowErrors = (int) errors.stream()
                    .filter(e -> e.getSeverity() == ErrorSeverity.LOW)
                    .count();

            this.databaseErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.DATABASE)
                    .count();

            this.cameraErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.CAMERA)
                    .count();

            this.qrCodeErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.QR_CODE)
                    .count();

            this.validationErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.VALIDATION)
                    .count();

            this.uiErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.UI)
                    .count();

            this.systemErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.SYSTEM)
                    .count();

            this.networkErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.NETWORK)
                    .count();

            this.fileErrors = (int) errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.FILE_IO)
                    .count();
        }

        public int getTotalErrors() { return totalErrors; }
        public int getCriticalErrors() { return criticalErrors; }
        public int getHighErrors() { return highErrors; }
        public int getMediumErrors() { return mediumErrors; }
        public int getLowErrors() { return lowErrors; }
        public int getDatabaseErrors() { return databaseErrors; }
        public int getCameraErrors() { return cameraErrors; }
        public int getQrCodeErrors() { return qrCodeErrors; }
        public int getValidationErrors() { return validationErrors; }
        public int getUiErrors() { return uiErrors; }
        public int getSystemErrors() { return systemErrors; }
        public int getNetworkErrors() { return networkErrors; }
        public int getFileErrors() { return fileErrors; }

        public String getSummary() {
            return String.format(
                    "Total Errors: %d (Critical: %d, High: %d, Medium: %d, Low: %d)\n" +
                    "By Category: Database(%d), Camera(%d), QR Code(%d), Validation(%d), UI(%d), System(%d), Network(%d), File I/O(%d)",
                    totalErrors, criticalErrors, highErrors, mediumErrors, lowErrors,
                    databaseErrors, cameraErrors, qrCodeErrors, validationErrors, uiErrors, systemErrors, networkErrors, fileErrors
            );
        }
    }

    public static void initializeErrorHandling() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Error handling shutdown - final statistics: {}", getErrorStatistics().getSummary());
        }));

        log.info("Error handling system initialized");
    }
}