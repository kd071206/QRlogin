# Installation Guide

## Quick Installation

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Webcam (optional for QR scanning)

### Step 1: Setup Database
```bash
# Create database and user
mysql -u root -p < database_schema.sql
```

### Step 2: Configure Application
Edit `src/main/resources/config/database.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/university_attendance
db.username=attendance_app
db.password=your_password_here
```

### Step 3: Build and Run
```bash
# Make build script executable
chmod +x build.sh

# Run build script
./build.sh

# Or manually with Maven
mvn clean package
java -jar target/attendance-system-1.0.0.jar
```

## Manual Installation Steps

### 1. Install Java
**Windows:**
- Download from https://www.oracle.com/java/technologies/downloads/
- Install and add to PATH

**macOS:**
```bash
brew install openjdk@17
```

**Linux (Ubuntu):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Install Maven
**Windows:** Download and extract from Apache Maven website
**macOS/Linux:** `brew install maven` or `sudo apt install maven`

### 3. Install MySQL
**Windows:** Download from MySQL website
**macOS:** `brew install mysql`
**Linux:** `sudo apt install mysql-server`

### 4. Database Setup
```bash
# Start MySQL service
sudo systemctl start mysql  # Linux
brew services start mysql    # macOS

# Execute schema
mysql -u root -p < database_schema.sql
```

### 5. Run Application
```bash
mvn clean package
java -jar target/attendance-system-1.0.0.jar
```

## Troubleshooting

### Java not found
```bash
# Check Java installation
java -version

# Set JAVA_HOME (Linux/macOS)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Maven not found
```bash
# Check Maven installation
mvn -version

# Install Maven
sudo apt install maven  # Ubuntu
brew install maven       # macOS
```

### Database connection failed
1. Check MySQL service: `sudo systemctl status mysql`
2. Verify credentials in database.properties
3. Test connection: `mysql -u attendance_app -p university_attendance`

### Camera not detected
1. Check camera permissions in OS settings
2. Ensure no other app is using camera
3. Try different USB port

## Configuration Files

- `src/main/resources/config/database.properties` - Database connection
- `src/main/resources/config/app.properties` - Application settings
- `src/main/resources/logback.xml` - Logging configuration

## Default Settings

- Camera resolution: 640x480
- QR code timeout: 10 seconds
- Maximum session duration: 12 hours
- Minimum session duration: 1 minute