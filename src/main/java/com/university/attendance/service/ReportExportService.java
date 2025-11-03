package com.university.attendance.service;

import com.university.attendance.model.AttendanceSession;
import com.university.attendance.model.User;
import com.university.attendance.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ReportExportService {

    public enum ExportFormat {
        CSV, PDF, EXCEL
    }

    public static class ExportResult {
        private final boolean success;
        private final String message;
        private final String filePath;
        private final int recordCount;

        public ExportResult(boolean success, String message, String filePath, int recordCount) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
            this.recordCount = recordCount;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFilePath() { return filePath; }
        public int getRecordCount() { return recordCount; }
    }

    public ExportResult exportAttendanceToCSV(List<AttendanceSession> sessions, List<User> users,
                                            LocalDate startDate, LocalDate endDate) {
        try {
            String fileName = String.format("attendance_report_%s_to_%s.csv",
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

            Path outputDir = Paths.get("reports");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path filePath = outputDir.resolve(fileName);

            Map<String, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println("Student ID,Name,Email,Department,Year Level,Login Time,Logout Time,Duration (minutes),Date,Status");

                for (AttendanceSession session : sessions) {
                    User user = userMap.get(session.getUserId());
                    if (user == null) {
                        log.warn("User not found for session: {}", session.getId());
                        continue;
                    }

                    String csvLine = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%s\"",
                            escapeCSV(user.getStudentId()),
                            escapeCSV(user.getName()),
                            escapeCSV(user.getEmail()),
                            escapeCSV(user.getDepartment() != null ? user.getDepartment() : ""),
                            user.getYearLevel() != null ? user.getYearLevel() : "",
                            session.getLoginTime() != null ? TimeUtil.formatDateTime(session.getLoginTime()) : "",
                            session.getLogoutTime() != null ? TimeUtil.formatDateTime(session.getLogoutTime()) : "",
                            session.getDurationMinutes() != null ? session.getDurationMinutes() : 0,
                            session.getSessionDate() != null ? session.getSessionDate().toString() : "",
                            session.getStatus() != null ? session.getStatus().toString() : ""
                    );

                    writer.println(csvLine);
                }
            }

            log.info("CSV export completed: {} records -> {}", sessions.size(), filePath);
            return new ExportResult(true, "CSV export successful", filePath.toString(), sessions.size());

        } catch (Exception e) {
            log.error("Error exporting attendance to CSV", e);
            return new ExportResult(false, "CSV export failed: " + e.getMessage(), null, 0);
        }
    }

    public ExportResult exportAttendanceToPDF(List<AttendanceSession> sessions, List<User> users,
                                            LocalDate startDate, LocalDate endDate) {
        try {
            String fileName = String.format("attendance_report_%s_to_%s.pdf",
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

            Path outputDir = Paths.get("reports");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path filePath = outputDir.resolve(fileName);

            Map<String, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println("%PDF-1.4");
                writer.println("1 0 obj");
                writer.println("<< /Type /Catalog /Pages 2 0 R >>");
                writer.println("endobj");
                writer.println("2 0 obj");
                writer.println("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
                writer.println("endobj");
                writer.println("3 0 obj");
                writer.println("<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>");
                writer.println("endobj");
                writer.println("4 0 obj");
                writer.println("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
                writer.println("endobj");
                writer.println("5 0 obj");
                writer.println("<< /Length 200 >>");
                writer.println("stream");
                writer.println("BT");
                writer.println("/F1 12 Tf");
                writer.println("100 700 Td");
                writer.println("(University Attendance Report) Tj");
                writer.println("100 680 Td");
                writer.println("(" + startDate + " to " + endDate + ") Tj");
                writer.println("100 640 Td");
                writer.println("(Student ID      Name            Login Time      Logout Time    Duration) Tj");

                int yPosition = 620;
                for (AttendanceSession session : sessions) {
                    User user = userMap.get(session.getUserId());
                    if (user != null) {
                        writer.println("100 " + yPosition + " Td");
                        writer.println("(" + user.getStudentId() + "    " + user.getName() + "    " +
                                (session.getLoginTime() != null ? TimeUtil.formatTime(session.getLoginTime()) : "") + "    " +
                                (session.getLogoutTime() != null ? TimeUtil.formatTime(session.getLogoutTime()) : "") + "    " +
                                (session.getDurationMinutes() != null ? session.getDurationMinutes() + " min" : "") + ") Tj");
                        yPosition -= 20;
                        if (yPosition < 100) {
                            break;
                        }
                    }
                }

                writer.println("ET");
                writer.println("endstream");
                writer.println("endobj");
                writer.println("xref");
                writer.println("0 6");
                writer.println("0000000000 65535 f ");
                writer.println("0000000009 00000 n ");
                writer.println("0000000054 00000 n ");
                writer.println("0000000110 00000 n ");
                writer.println("0000000267 00000 n ");
                writer.println("0000000345 00000 n ");
                writer.println("trailer");
                writer.println("<< /Size 6 /Root 1 0 R >>");
                writer.println("startxref");
                writer.println("500");
                writer.println("%%EOF");
            }

            log.info("PDF export completed: {} records -> {}", sessions.size(), filePath);
            return new ExportResult(true, "PDF export successful", filePath.toString(), sessions.size());

        } catch (Exception e) {
            log.error("Error exporting attendance to PDF", e);
            return new ExportResult(false, "PDF export failed: " + e.getMessage(), null, 0);
        }
    }

    public ExportResult exportAttendanceToExcel(List<AttendanceSession> sessions, List<User> users,
                                              LocalDate startDate, LocalDate endDate) {
        try {
            String fileName = String.format("attendance_report_%s_to_%s.html",
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

            Path outputDir = Paths.get("reports");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path filePath = outputDir.resolve(fileName);

            Map<String, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html>");
                writer.println("<head>");
                writer.println("<title>University Attendance Report</title>");
                writer.println("<style>");
                writer.println("body { font-family: Arial, sans-serif; margin: 20px; }");
                writer.println("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
                writer.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
                writer.println("th { background-color: #003366; color: white; }");
                writer.println("tr:nth-child(even) { background-color: #f2f2f2; }");
                writer.println(".header { text-align: center; margin-bottom: 30px; }");
                writer.println(".summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }");
                writer.println("</style>");
                writer.println("</head>");
                writer.println("<body>");

                writer.println("<div class='header'>");
                writer.println("<h1>University Attendance Report</h1>");
                writer.println("<h2>" + startDate + " to " + endDate + "</h2>");
                writer.println("<p>Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
                writer.println("</div>");

                writer.println("<div class='summary'>");
                writer.println("<h3>Summary</h3>");
                writer.println("<p>Total Sessions: " + sessions.size() + "</p>");

                long activeSessions = sessions.stream()
                        .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.ACTIVE)
                        .count();

                long completedSessions = sessions.stream()
                        .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED)
                        .count();

                double averageDuration = sessions.stream()
                        .filter(s -> s.getDurationMinutes() != null && s.getDurationMinutes() > 0)
                        .mapToInt(AttendanceSession::getDurationMinutes)
                        .average()
                        .orElse(0.0);

                writer.println("<p>Active Sessions: " + activeSessions + "</p>");
                writer.println("<p>Completed Sessions: " + completedSessions + "</p>");
                writer.println("<p>Average Duration: " + TimeUtil.formatDuration((int) averageDuration) + "</p>");
                writer.println("</div>");

                writer.println("<table>");
                writer.println("<thead>");
                writer.println("<tr>");
                writer.println("<th>Student ID</th>");
                writer.println("<th>Name</th>");
                writer.println("<th>Email</th>");
                writer.println("<th>Department</th>");
                writer.println("<th>Year Level</th>");
                writer.println("<th>Login Time</th>");
                writer.println("<th>Logout Time</th>");
                writer.println("<th>Duration (min)</th>");
                writer.println("<th>Status</th>");
                writer.println("</tr>");
                writer.println("</thead>");
                writer.println("<tbody>");

                for (AttendanceSession session : sessions) {
                    User user = userMap.get(session.getUserId());
                    if (user != null) {
                        writer.println("<tr>");
                        writer.println("<td>" + escapeHtml(user.getStudentId()) + "</td>");
                        writer.println("<td>" + escapeHtml(user.getName()) + "</td>");
                        writer.println("<td>" + escapeHtml(user.getEmail()) + "</td>");
                        writer.println("<td>" + escapeHtml(user.getDepartment() != null ? user.getDepartment() : "") + "</td>");
                        writer.println("<td>" + (user.getYearLevel() != null ? user.getYearLevel() : "") + "</td>");
                        writer.println("<td>" + (session.getLoginTime() != null ? TimeUtil.formatDateTime(session.getLoginTime()) : "") + "</td>");
                        writer.println("<td>" + (session.getLogoutTime() != null ? TimeUtil.formatDateTime(session.getLogoutTime()) : "") + "</td>");
                        writer.println("<td>" + (session.getDurationMinutes() != null ? session.getDurationMinutes() : "") + "</td>");
                        writer.println("<td>" + (session.getStatus() != null ? session.getStatus().toString() : "") + "</td>");
                        writer.println("</tr>");
                    }
                }

                writer.println("</tbody>");
                writer.println("</table>");
                writer.println("</body>");
                writer.println("</html>");
            }

            log.info("Excel export completed: {} records -> {}", sessions.size(), filePath);
            return new ExportResult(true, "Excel export successful", filePath.toString(), sessions.size());

        } catch (Exception e) {
            log.error("Error exporting attendance to Excel", e);
            return new ExportResult(false, "Excel export failed: " + e.getMessage(), null, 0);
        }
    }

    public ExportResult exportDailySummary(LocalDate date, List<AttendanceSession> sessions, List<User> users) {
        try {
            String fileName = String.format("daily_summary_%s.csv", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            Path outputDir = Paths.get("reports");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Path filePath = outputDir.resolve(fileName);

            Map<String, List<AttendanceSession>> userSessions = sessions.stream()
                    .collect(Collectors.groupingBy(AttendanceSession::getUserId));

            Map<String, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
                writer.println("Daily Attendance Summary - " + date);
                writer.println();
                writer.println("Student ID,Name,Department,Total Sessions,Total Duration (minutes),First Login,Last Logout");

                for (Map.Entry<String, List<AttendanceSession>> entry : userSessions.entrySet()) {
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    if (user == null) continue;

                    List<AttendanceSession> userSessionList = entry.getValue();
                    int totalSessions = userSessionList.size();
                    int totalDuration = userSessionList.stream()
                            .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                            .sum();

                    LocalDateTime firstLogin = userSessionList.stream()
                            .map(AttendanceSession::getLoginTime)
                            .filter(java.util.Objects::nonNull)
                            .min(java.util.Comparator.naturalOrder())
                            .orElse(null);

                    LocalDateTime lastLogout = userSessionList.stream()
                            .map(AttendanceSession::getLogoutTime)
                            .filter(java.util.Objects::nonNull)
                            .max(java.util.Comparator.naturalOrder())
                            .orElse(null);

                    String csvLine = String.format("\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\"",
                            escapeCSV(user.getStudentId()),
                            escapeCSV(user.getName()),
                            escapeCSV(user.getDepartment() != null ? user.getDepartment() : ""),
                            totalSessions,
                            totalDuration,
                            firstLogin != null ? TimeUtil.formatDateTime(firstLogin) : "",
                            lastLogout != null ? TimeUtil.formatDateTime(lastLogout) : ""
                    );

                    writer.println(csvLine);
                }
            }

            log.info("Daily summary export completed: {} users -> {}", userSessions.size(), filePath);
            return new ExportResult(true, "Daily summary export successful", filePath.toString(), userSessions.size());

        } catch (Exception e) {
            log.error("Error exporting daily summary", e);
            return new ExportResult(false, "Daily summary export failed: " + e.getMessage(), null, 0);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains("\"") || value.contains(",") || value.contains("\n")) {
            return value.replace("\"", "\"\"");
        }
        return value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public void createReportDirectory() {
        try {
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
                log.info("Created reports directory: {}", reportsDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create reports directory", e);
        }
    }
}