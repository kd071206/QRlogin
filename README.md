# University Attendance System

A professional desktop application for university attendance tracking using QR code scanning. The application automatically records user login/logout times, calculates total duration, and provides comprehensive attendance management with a professional UI suitable for university environments.

## Features

- **QR Code Scanning**: Real-time camera-based QR code detection for automated attendance
- **Time Tracking**: Automatic login/logout recording with duration calculation
- **Professional UI**: University-themed interface with modern design
- **Database Integration**: MySQL database for reliable data storage
- **Report Generation**: Export attendance data in CSV, PDF, and Excel formats
- **QR Code Generation**: Administrative tools for creating student QR codes
- **Error Handling**: Comprehensive validation and error reporting
- **Cross-Platform**: Works on Windows, macOS, and Linux

## Quick Start

### Prerequisites

1. **Java Development Kit (JDK)** 17 or later
2. **Maven** 3.6.0 or later
3. **MySQL Server** 8.0 or later
4. **Webcam** for QR code scanning

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd QRlogin
   ```

2. **Database Setup**
   ```bash
   mysql -u root -p < database_schema.sql
   ```

3. **Configure Database**
   Edit `src/main/resources/config/database.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/university_attendance
   db.username=attendance_app
   db.password=your_secure_password
   ```

4. **Build and Run**
   ```bash
   mvn clean package
   java -jar target/attendance-system-1.0.0.jar
   ```

## System Requirements

### Hardware Requirements
- **Processor**: Intel Core i3 or equivalent
- **Memory**: 4GB RAM minimum (8GB recommended)
- **Storage**: 500MB free disk space
- **Camera**: USB webcam or built-in camera
- **Display**: 1024x768 resolution minimum

### Software Requirements
- **Operating System**: Windows 10/11, macOS 10.15+, or Linux (Ubuntu 18.04+)
- **Java**: JDK 17 or later
- **Database**: MySQL 8.0+ or MariaDB 10.5+
- **Network**: Internet connection for database connectivity

## Detailed Installation Guide

### 1. Java Installation

**Windows:**
1. Download JDK 17+ from [Oracle JDK Downloads](https://www.oracle.com/java/technologies/downloads/)
2. Run the installer and follow the setup wizard
3. Set JAVA_HOME environment variable:
   ```
   set JAVA_HOME=C:\Program Files\Java\jdk-17
   set PATH=%PATH%;%JAVA_HOME%\bin
   ```

**macOS:**
```bash
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Maven Installation

**Windows:**
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH:
   ```
   set PATH=%PATH%;C:\Program Files\Apache\maven\bin
   ```

**macOS/Linux:**
```bash
brew install maven
```

### 3. MySQL Installation

**Windows:**
1. Download MySQL Community Server from [MySQL Downloads](https://dev.mysql.com/downloads/mysql/)
2. Run the installer and remember root password
3. Configure MySQL to start automatically

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 4. Database Configuration

1. **Login to MySQL:**
   ```bash
   mysql -u root -p
   ```

2. **Execute Schema:**
   ```sql
   source database_schema.sql;
   ```

3. **Verify Database:**
   ```sql
   USE university_attendance;
   SHOW TABLES;
   ```

### 5. Application Configuration

Edit configuration files in `src/main/resources/config/`:

**database.properties:**
```properties
db.url=jdbc:mysql://localhost:3306/university_attendance
db.username=attendance_app
db.password=your_secure_password
db.driver=com.mysql.cj.jdbc.Driver
db.pool.size=10
```

**app.properties:**
```properties
app.name=University Attendance System
app.version=1.0.0
app.camera.resolution=640x480
app.camera.fps=30
app.qr.timeout=10000
app.session.max_hours=12
app.session.min_minutes=1
```

## Usage Guide

### Starting the Application

1. **Command Line:**
   ```bash
   java -jar target/attendance-system-1.0.0.jar
   ```

2. **Or Run from IDE:**
   - Open project in IntelliJ IDEA or Eclipse
   - Run `SimpleMain.java` class

### Basic Operations

#### 1. Test System Configuration
- Click "Test Database" to verify database connection
- Click "Test Camera" to verify camera availability
- Check system information panel

#### 2. Generate Test Data
- Click "Generate Test Data" to create sample users
- This creates 3 test students with different departments

#### 3. View Statistics
- Click "Show Statistics" to display today's attendance data
- View active sessions, completed sessions, and average duration

#### 4. QR Code Scanning (Advanced Mode)
- Initialize camera service
- Start real-time QR code detection
- Process login/logout actions

### QR Code Format

The system uses JSON format QR codes:
```json
{
  "type": "attendance",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "studentId": "2024-CS-0156",
  "timestamp": "2024-01-15T10:30:00Z",
  "action": "login|logout",
  "checksum": "md5_hash"
}
```

## Admin Tools

### Generating QR Codes

**Programmatic Generation:**
```java
QRCodeGenerator generator = new QRCodeGenerator();
QRCodeGenerationResult result = generator.generateStudentQR(
    "2024-CS-001", "Alice Johnson", "alice@university.edu",
    "Computer Science", 3, "login"
);
```

**Batch Generation:**
```java
List<User> users = getAllUsers();
boolean success = generator.generateBatchQRCodes(users, "login");
```

### Exporting Reports

**CSV Export:**
```java
ReportExportService exportService = new ReportExportService();
ExportResult result = exportService.exportAttendanceToCSV(sessions, users, startDate, endDate);
```

**PDF Export:**
```java
ExportResult result = exportService.exportAttendanceToPDF(sessions, users, startDate, endDate);
```

## Troubleshooting

### Common Issues

#### Camera Not Detected
**Problem**: "No cameras detected" message
**Solutions**:
1. Check camera is properly connected
2. Verify camera permissions in OS settings
3. Close other applications using the camera
4. Try a different USB port
5. Restart the application with administrator privileges

#### Database Connection Failed
**Problem**: "Unable to connect to database"
**Solutions**:
1. Verify MySQL service is running:
   - Windows: Check Services panel
   - macOS/Linux: `sudo systemctl status mysql`
2. Check database credentials in `database.properties`
3. Test connection manually:
   ```bash
   mysql -u attendance_app -p university_attendance
   ```
4. Verify firewall settings

#### QR Code Not Scanning
**Problem**: QR codes not recognized
**Solutions**:
1. Ensure adequate lighting conditions
2. Hold QR code steady and at appropriate distance (15-30cm)
3. Clean camera lens
4. Check QR code quality and contrast
5. Verify QR code contains correct JSON format

#### Build Errors
**Problem**: Maven compilation fails
**Solutions**:
1. Verify Java version is 17+:
   ```bash
   java -version
   ```
2. Clear Maven cache:
   ```bash
   mvn clean
   ```
3. Update Maven dependencies:
   ```bash
   mvn dependency:resolve
   ```

### Log Files

**Location**: `logs/attendance-system.log`

**Common Log Messages**:
- `Database connection test failed` - Database connectivity issue
- `Camera not available` - Camera hardware issue
- `Invalid QR code format` - QR code parsing issue
- `User not found` - User management issue

## Security Considerations

### Data Protection
- Database connection uses encrypted passwords
- QR codes include checksums for data integrity
- Sensitive data is not logged in plain text
- Regular backups recommended

### Camera Security
- Application requests camera permissions only when needed
- No camera data is stored permanently
- Camera feed is processed locally only

### Network Security
- Database connections use SSL when available
- No data is transmitted to external servers
- All processing happens on local machine

## Performance Optimization

### Database Optimization
- Use connection pooling (configured in database.properties)
- Regular database maintenance recommended
- Consider indexing for large datasets

### Camera Performance
- Optimize camera resolution (640x480 recommended)
- Adjust frame rate for better performance
- Use adequate lighting for faster QR detection

### System Resources
- Minimum 4GB RAM recommended
- Solid-state drive improves startup time
- Close unnecessary applications during use

## Development and Customization

### Building from Source
```bash
mvn clean compile
mvn package
```

### Running Tests
```bash
mvn test
```

### Custom Configuration
- Modify `app.properties` for application settings
- Adjust `university-theme.css` for UI customization
- Update database schema for additional fields

## Support and Maintenance

### Regular Maintenance Tasks
1. **Weekly**: Check error logs
2. **Monthly**: Database backup and optimization
3. **Quarterly**: Review user accounts and permissions
4. **Annually**: System security audit

### Getting Help
- Check troubleshooting section first
- Review log files for error details
- Test with minimal configuration
- Report issues with system specifications

### System Updates
1. Backup database before updating
2. Test updates in development environment
3. Update Java and MySQL versions as needed
4. Monitor performance after updates

## License

This project is for educational and institutional use. Please ensure compliance with your institution's policies regarding attendance systems and data privacy.

## Credits

- JavaFX for UI framework
- ZXing for QR code processing
- Sarxos Webcam Capture for camera integration
- MySQL for database storage
- Lombok for code generation