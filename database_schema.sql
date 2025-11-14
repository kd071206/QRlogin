-- University Attendance System Database Schema
-- Create database
CREATE DATABASE IF NOT EXISTS university_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE university_attendance;

-- Create database user
CREATE USER IF NOT EXISTS 'attendance_app'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON university_attendance.* TO 'attendance_app'@'localhost';
FLUSH PRIVILEGES;

-- users Table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    department VARCHAR(50),
    year_level INT,
    qr_code_data VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- attendance_sessions Table
CREATE TABLE attendance_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    login_time TIMESTAMP NOT NULL,
    logout_time TIMESTAMP,
    duration_minutes INT,
    session_date DATE NOT NULL,
    status ENUM('ACTIVE', 'COMPLETED', 'ABNORMAL') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_date (user_id, session_date),
    INDEX idx_login_time (login_time)
);

-- attendance_records Table
CREATE TABLE attendance_records (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    action_type ENUM('LOGIN', 'LOGOUT') NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    qr_code_data VARCHAR(255) NOT NULL,
    camera_device VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_qr_code (qr_code_data)
);