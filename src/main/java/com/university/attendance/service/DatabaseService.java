package com.university.attendance.service;

import com.university.attendance.model.AttendanceRecord;
import com.university.attendance.model.AttendanceSession;
import com.university.attendance.model.User;
import com.university.attendance.util.DatabaseUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DatabaseService {

    public boolean saveUser(User user) {
        String sql = "INSERT INTO users (id, student_id, name, email, department, year_level, qr_code_data, created_at, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name=?, email=?, department=?, year_level=?, qr_code_data=?, is_active=?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getStudentId());
            stmt.setString(3, user.getName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getDepartment());
            stmt.setInt(6, user.getYearLevel() != null ? user.getYearLevel() : 0);
            stmt.setString(7, user.getQrCodeData());
            stmt.setTimestamp(8, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setBoolean(9, user.isActive());

            stmt.setString(10, user.getName());
            stmt.setString(11, user.getEmail());
            stmt.setString(12, user.getDepartment());
            stmt.setInt(13, user.getYearLevel() != null ? user.getYearLevel() : 0);
            stmt.setString(14, user.getQrCodeData());
            stmt.setBoolean(15, user.isActive());

            int rowsAffected = stmt.executeUpdate();
            log.info("User saved/updated: {} (rows affected: {})", user.getStudentId(), rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            log.error("Error saving user: {}", user.getStudentId(), e);
            return false;
        }
    }

    public Optional<User> findUserByStudentId(String studentId) {
        String sql = "SELECT * FROM users WHERE student_id = ? AND is_active = true";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error finding user by student ID: {}", studentId, e);
        }

        return Optional.empty();
    }

    public Optional<User> findUserByQRCodeData(String qrCodeData) {
        String sql = "SELECT * FROM users WHERE qr_code_data = ? AND is_active = true";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, qrCodeData);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error finding user by QR code data", e);
        }

        return Optional.empty();
    }

    public Optional<AttendanceSession> findActiveSession(String userId) {
        String sql = "SELECT * FROM attendance_sessions WHERE user_id = ? AND status = 'ACTIVE' ORDER BY login_time DESC LIMIT 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error finding active session for user: {}", userId, e);
        }

        return Optional.empty();
    }

    public boolean saveAttendanceSession(AttendanceSession session) {
        String sql = "INSERT INTO attendance_sessions (id, user_id, login_time, logout_time, duration_minutes, session_date, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE logout_time=?, duration_minutes=?, status=?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getId());
            stmt.setString(2, session.getUserId());
            stmt.setTimestamp(3, session.getLoginTime() != null ? Timestamp.valueOf(session.getLoginTime()) : null);
            stmt.setTimestamp(4, session.getLogoutTime() != null ? Timestamp.valueOf(session.getLogoutTime()) : null);
            stmt.setInt(5, session.getDurationMinutes() != null ? session.getDurationMinutes() : 0);
            stmt.setDate(6, session.getSessionDate() != null ? Date.valueOf(session.getSessionDate()) : null);
            stmt.setString(7, session.getStatus().name());
            stmt.setTimestamp(8, session.getCreatedAt() != null ? Timestamp.valueOf(session.getCreatedAt()) : null);

            stmt.setTimestamp(9, session.getLogoutTime() != null ? Timestamp.valueOf(session.getLogoutTime()) : null);
            stmt.setInt(10, session.getDurationMinutes() != null ? session.getDurationMinutes() : 0);
            stmt.setString(11, session.getStatus().name());

            int rowsAffected = stmt.executeUpdate();
            log.info("Attendance session saved: {} (rows affected: {})", session.getId(), rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            log.error("Error saving attendance session: {}", session.getId(), e);
            return false;
        }
    }

    public boolean saveAttendanceRecord(AttendanceRecord record) {
        String sql = "INSERT INTO attendance_records (id, user_id, session_id, action_type, timestamp, qr_code_data, camera_device) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, record.getId());
            stmt.setString(2, record.getUserId());
            stmt.setString(3, record.getSessionId());
            stmt.setString(4, record.getActionType().name());
            stmt.setTimestamp(5, record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp()) : null);
            stmt.setString(6, record.getQrCodeData());
            stmt.setString(7, record.getCameraDevice());

            int rowsAffected = stmt.executeUpdate();
            log.info("Attendance record saved: {} (rows affected: {})", record.getId(), rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            log.error("Error saving attendance record: {}", record.getId(), e);
            return false;
        }
    }

    public List<AttendanceSession> getSessionsByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT s.*, u.name, u.student_id " +
                     "FROM attendance_sessions s " +
                     "JOIN users u ON s.user_id = u.id " +
                     "WHERE s.session_date BETWEEN ? AND ? " +
                     "ORDER BY s.login_time DESC";

        List<AttendanceSession> sessions = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error getting sessions by date range: {} to {}", startDate, endDate, e);
        }

        return sessions;
    }

    public List<User> getAllActiveUsers() {
        String sql = "SELECT * FROM users WHERE is_active = true ORDER BY name";

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            log.error("Error getting all active users", e);
        }

        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setStudentId(rs.getString("student_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setDepartment(rs.getString("department"));
        user.setYearLevel(rs.getInt("year_level"));
        user.setQrCodeData(rs.getString("qr_code_data"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    private AttendanceSession mapResultSetToSession(ResultSet rs) throws SQLException {
        AttendanceSession session = new AttendanceSession();
        session.setId(rs.getString("id"));
        session.setUserId(rs.getString("user_id"));
        session.setLoginTime(rs.getTimestamp("login_time") != null ?
                           rs.getTimestamp("login_time").toLocalDateTime() : null);
        session.setLogoutTime(rs.getTimestamp("logout_time") != null ?
                            rs.getTimestamp("logout_time").toLocalDateTime() : null);
        session.setDurationMinutes(rs.getInt("duration_minutes"));
        session.setSessionDate(rs.getDate("session_date") != null ?
                             rs.getDate("session_date").toLocalDate() : null);
        session.setStatus(AttendanceSession.SessionStatus.valueOf(rs.getString("status")));
        session.setCreatedAt(rs.getTimestamp("created_at") != null ?
                           rs.getTimestamp("created_at").toLocalDateTime() : null);
        return session;
    }

    public boolean testConnection() {
        return DatabaseUtil.testConnection();
    }
}