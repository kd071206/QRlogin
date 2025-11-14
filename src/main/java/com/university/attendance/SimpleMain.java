package com.university.attendance;

import com.university.attendance.service.AttendanceService;
import com.university.attendance.service.CameraService;
import com.university.attendance.util.DatabaseUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SimpleMain extends Application {
    private AttendanceService attendanceService;
    private CameraService cameraService;
    private VBox mainLayout;
    private Label statusLabel;
    private TextArea outputArea;

    @Override
    public void init() {
        log.info("Initializing University Attendance System...");

        try {
            attendanceService = new AttendanceService();
            cameraService = new CameraService();

            if (!attendanceService.testDatabaseConnection()) {
                log.error("Database connection test failed");
                Platform.runLater(() -> showError("Database Error",
                    "Unable to connect to database. Please check configuration."));
                return;
            }

            log.info("Application initialization completed successfully");

        } catch (Exception e) {
            log.error("Failed to initialize application", e);
            Platform.runLater(() -> showError("Initialization Error",
                "Failed to initialize: " + e.getMessage()));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting University Attendance System...");

        primaryStage.setTitle("University Attendance System");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        mainLayout = createMainLayout();

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/university-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();

        primaryStage.setOnCloseRequest(event -> {
            log.info("Application close requested");
            cleanup();
            Platform.exit();
        });

        primaryStage.show();
        log.info("Application started successfully");
    }

    private VBox createMainLayout() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("University Attendance System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #003366;");

        statusLabel = new Label("System Ready");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(300);
        outputArea.setFont(Font.font("Consolas", 12));

        HBox buttonBox = createButtonBox();

        VBox systemInfoBox = createSystemInfoBox();

        root.getChildren().addAll(
            titleLabel,
            statusLabel,
            buttonBox,
            systemInfoBox,
            new Label("System Output:"),
            outputArea
        );

        return root;
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button testDbBtn = new Button("Test Database");
        testDbBtn.setOnAction(e -> testDatabaseConnection());

        Button testCameraBtn = new Button("Test Camera");
        testCameraBtn.setOnAction(e -> testCamera());

        Button generateTestDataBtn = new Button("Generate Test Data");
        generateTestDataBtn.setOnAction(e -> generateTestData());

        Button statsBtn = new Button("Show Statistics");
        statsBtn.setOnAction(e -> showStatistics());

        styleButton(testDbBtn);
        styleButton(testCameraBtn);
        styleButton(generateTestDataBtn);
        styleButton(statsBtn);

        buttonBox.getChildren().addAll(testDbBtn, testCameraBtn, generateTestDataBtn, statsBtn);
        return buttonBox;
    }

    private VBox createSystemInfoBox() {
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 5;");

        Label infoTitle = new Label("System Information");
        infoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label dbStatus = new Label("Database: " + (attendanceService != null && attendanceService.testDatabaseConnection() ? "Connected" : "Not Connected"));
        Label cameraStatus = new Label("Cameras: " + (cameraService != null ? cameraService.getAvailableCameras().size() : 0) + " available");

        infoBox.getChildren().addAll(infoTitle, dbStatus, cameraStatus);
        return infoBox;
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #003366; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-border-radius: 5;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #002244; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-border-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #003366; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-border-radius: 5;"));
    }

    private void testDatabaseConnection() {
        if (attendanceService == null) {
            updateStatus("Attendance service not initialized");
            return;
        }

        updateStatus("Testing database connection...");
        boolean connected = attendanceService.testDatabaseConnection();

        if (connected) {
            updateStatus("Database connection successful");
            appendOutput("✓ Database connection test successful");
        } else {
            updateStatus("Database connection failed");
            appendOutput("✗ Database connection test failed");
        }
    }

    private void testCamera() {
        if (cameraService == null) {
            updateStatus("Camera service not initialized");
            return;
        }

        List<String> cameras = cameraService.getAvailableCameras();
        updateStatus("Found " + cameras.size() + " camera(s)");

        appendOutput("=== Camera Test Results ===");
        if (cameras.isEmpty()) {
            appendOutput("✗ No cameras detected");
        } else {
            appendOutput("✓ Cameras detected:");
            for (int i = 0; i < cameras.size(); i++) {
                appendOutput("  " + (i + 1) + ". " + cameras.get(i));
            }
        }
        appendOutput("");
    }

    private void generateTestData() {
        if (attendanceService == null) {
            updateStatus("Attendance service not initialized");
            return;
        }

        updateStatus("Generating test data...");
        appendOutput("=== Generating Test Data ===");

        try {
            boolean user1 = attendanceService.createTestUser("2024-CS-001", "Alice Johnson",
                "alice.j@university.edu", "Computer Science", 3);
            boolean user2 = attendanceService.createTestUser("2024-EE-002", "Bob Smith",
                "bob.s@university.edu", "Electrical Engineering", 2);
            boolean user3 = attendanceService.createTestUser("2024-ME-003", "Carol Davis",
                "carol.d@university.edu", "Mechanical Engineering", 4);

            if (user1) appendOutput("✓ Created test user: Alice Johnson");
            if (user2) appendOutput("✓ Created test user: Bob Smith");
            if (user3) appendOutput("✓ Created test user: Carol Davis");

            updateStatus("Test data generation completed");

        } catch (Exception e) {
            appendOutput("✗ Error generating test data: " + e.getMessage());
            updateStatus("Test data generation failed");
        }
        appendOutput("");
    }

    private void showStatistics() {
        if (attendanceService == null) {
            updateStatus("Attendance service not initialized");
            return;
        }

        updateStatus("Loading statistics...");
        appendOutput("=== Today's Statistics ===");

        try {
            var stats = attendanceService.getTodayStats();
            appendOutput("Total Users: " + stats.getTotalUsers());
            appendOutput("Active Sessions: " + stats.getActiveSessions());
            appendOutput("Completed Sessions: " + stats.getCompletedSessions());
            appendOutput("Average Duration: " + stats.getFormattedAverageDuration());

            var sessions = attendanceService.getTodaySessions();
            appendOutput("");
            appendOutput("Today's Sessions:");
            if (sessions.isEmpty()) {
                appendOutput("No sessions recorded today.");
            } else {
                for (var session : sessions) {
                    appendOutput("  - Session ID: " + session.getId().substring(0, 8) + "...");
                    appendOutput("    Status: " + session.getStatus());
                    appendOutput("    Login: " + (session.getLoginTime() != null ? session.getLoginTime().toString() : "N/A"));
                    if (session.getLogoutTime() != null) {
                        appendOutput("    Logout: " + session.getLogoutTime().toString());
                        appendOutput("    Duration: " + session.getDurationMinutes() + " minutes");
                    }
                    appendOutput("");
                }
            }

            updateStatus("Statistics loaded successfully");

        } catch (Exception e) {
            appendOutput("✗ Error loading statistics: " + e.getMessage());
            updateStatus("Statistics loading failed");
        }
    }

    private void updateStatus(String status) {
        statusLabel.setText("Status: " + status);
        log.info("Status: {}", status);
    }

    private void appendOutput(String message) {
        Platform.runLater(() -> {
            outputArea.appendText(message + "\n");
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cleanup() {
        try {
            if (cameraService != null) {
                cameraService.shutdown();
            }
            if (attendanceService != null) {
                attendanceService.cleanup();
            }
            DatabaseUtil.shutdown();
        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }

    public static void main(String[] args) {
        log.info("University Attendance System starting...");
        launch(args);
    }
}