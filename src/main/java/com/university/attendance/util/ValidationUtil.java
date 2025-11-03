package com.university.attendance.util;

import com.university.attendance.model.User;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile(
            "^[A-Za-z]{4}-[A-Za-z]{2}-\\d{4}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-z\\s\\'-]{2,50}$"
    );

    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String field;

        public ValidationResult(boolean valid, String message, String field) {
            this.valid = valid;
            this.message = message;
            this.field = field;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getField() { return field; }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String message, String field) {
            return new ValidationResult(false, message, field);
        }
    }

    public static ValidationResult validateUser(User user) {
        if (user == null) {
            return ValidationResult.error("User cannot be null", "user");
        }

        ValidationResult studentIdResult = validateStudentId(user.getStudentId());
        if (!studentIdResult.isValid()) {
            return studentIdResult;
        }

        ValidationResult nameResult = validateName(user.getName());
        if (!nameResult.isValid()) {
            return nameResult;
        }

        ValidationResult emailResult = validateEmail(user.getEmail());
        if (!emailResult.isValid()) {
            return emailResult;
        }

        ValidationResult departmentResult = validateDepartment(user.getDepartment());
        if (!departmentResult.isValid()) {
            return departmentResult;
        }

        ValidationResult yearLevelResult = validateYearLevel(user.getYearLevel());
        if (!yearLevelResult.isValid()) {
            return yearLevelResult;
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return ValidationResult.error("Student ID is required", "studentId");
        }

        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            return ValidationResult.error("Student ID must be in format: YYYY-DE-0000 (e.g., 2024-CS-0015)", "studentId");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.error("Name is required", "name");
        }

        if (name.length() < 2 || name.length() > 50) {
            return ValidationResult.error("Name must be between 2 and 50 characters", "name");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.error("Name can only contain letters, spaces, hyphens, and apostrophes", "name");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("Email is required", "email");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("Invalid email format", "email");
        }

        if (email.length() > 100) {
            return ValidationResult.error("Email cannot exceed 100 characters", "email");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return ValidationResult.error("Department is required", "department");
        }

        if (department.length() < 2 || department.length() > 50) {
            return ValidationResult.error("Department must be between 2 and 50 characters", "department");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateYearLevel(Integer yearLevel) {
        if (yearLevel == null) {
            return ValidationResult.error("Year level is required", "yearLevel");
        }

        if (yearLevel < 1 || yearLevel > 6) {
            return ValidationResult.error("Year level must be between 1 and 6", "yearLevel");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateQRCodeData(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            return ValidationResult.error("QR code data cannot be empty", "qrData");
        }

        if (qrData.length() > 1000) {
            return ValidationResult.error("QR code data is too long", "qrData");
        }

        try {
            if (!qrData.startsWith("{") || !qrData.endsWith("}")) {
                return ValidationResult.error("QR code must be valid JSON", "qrData");
            }

            if (!qrData.contains("\"type\":\"attendance\"")) {
                return ValidationResult.error("QR code must be attendance type", "qrData");
            }

            if (!qrData.contains("studentId") || !qrData.contains("action")) {
                return ValidationResult.error("QR code missing required fields", "qrData");
            }

            return ValidationResult.success();

        } catch (Exception e) {
            return ValidationResult.error("Invalid QR code format: " + e.getMessage(), "qrData");
        }
    }

    public static ValidationResult validateTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return ValidationResult.error("Timestamp is required", "timestamp");
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            if (timestamp.isAfter(now.plusMinutes(5))) {
                return ValidationResult.error("Timestamp is in the future", "timestamp");
            }

            if (timestamp.isBefore(now.minusHours(24))) {
                return ValidationResult.error("Timestamp is too old (more than 24 hours)", "timestamp");
            }

            return ValidationResult.success();

        } catch (DateTimeParseException e) {
            return ValidationResult.error("Invalid timestamp format. Use ISO format: yyyy-MM-ddTHH:mm:ss", "timestamp");
        }
    }

    public static ValidationResult validateSessionDuration(Integer minutes) {
        if (minutes == null) {
            return ValidationResult.error("Duration cannot be null", "duration");
        }

        if (minutes < 0) {
            return ValidationResult.error("Duration cannot be negative", "duration");
        }

        if (minutes > 720) { // 12 hours
            return ValidationResult.error("Duration exceeds maximum allowed (12 hours)", "duration");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateCameraDevice(String cameraDevice) {
        if (cameraDevice != null && cameraDevice.length() > 100) {
            return ValidationResult.error("Camera device name is too long", "cameraDevice");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null) {
            return ValidationResult.error("Start date is required", "startDate");
        }

        if (endDate == null) {
            return ValidationResult.error("End date is required", "endDate");
        }

        if (startDate.isAfter(endDate)) {
            return ValidationResult.error("Start date cannot be after end date", "dateRange");
        }

        if (startDate.isBefore(java.time.LocalDate.now().minusYears(1))) {
            return ValidationResult.error("Start date cannot be more than 1 year in the past", "startDate");
        }

        if (endDate.isAfter(java.time.LocalDate.now().plusMonths(1))) {
            return ValidationResult.error("End date cannot be more than 1 month in the future", "endDate");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 365) {
            return ValidationResult.error("Date range cannot exceed 1 year", "dateRange");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateExportFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return ValidationResult.error("Export format is required", "format");
        }

        String normalizedFormat = format.toUpperCase().trim();
        if (!normalizedFormat.equals("CSV") && !normalizedFormat.equals("PDF") && !normalizedFormat.equals("EXCEL")) {
            return ValidationResult.error("Export format must be one of: CSV, PDF, EXCEL", "format");
        }

        return ValidationResult.success();
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try {
            if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
                return false;
            }

            int braceCount = 0;
            boolean inString = false;
            boolean escaped = false;

            for (int i = 0; i < jsonString.length(); i++) {
                char c = jsonString.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"' && !escaped) {
                    inString = !inString;
                    continue;
                }

                if (!inString) {
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount < 0) {
                            return false;
                        }
                    }
                }
            }

            return braceCount == 0 && !inString;

        } catch (Exception e) {
            log.error("Error validating JSON", e);
            return false;
        }
    }

    public static boolean isSecurePassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static String formatValidationErrors(ValidationResult... results) {
        StringBuilder errors = new StringBuilder();
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                if (errors.length() > 0) {
                    errors.append("\n");
                }
                errors.append(result.getField()).append(": ").append(result.getMessage());
            }
        }
        return errors.toString();
    }

    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    public static boolean isValidDatabaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        return url.startsWith("jdbc:mysql://") || url.startsWith("jdbc:postgresql://");
    }
}