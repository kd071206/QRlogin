package com.university.attendance;

import com.university.attendance.service.AttendanceService;
import com.university.attendance.service.CameraService;
import com.university.attendance.util.AlertUtil;
import com.university.attendance.util.DatabaseUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class Main extends Application {
    private static Stage primaryStage;
    private AttendanceService attendanceService;
    private CameraService cameraService;

    @Override
    public void init() {
        log.info("Initializing University Attendance System...");

        try {
            log.info("Loading application configuration...");
            Properties appConfig = loadAppConfig();
            log.info("Application loaded: {} v{}",
                    appConfig.getProperty("app.name", "University Attendance System"),
                    appConfig.getProperty("app.version", "1.0.0"));

            log.info("Initializing services...");
            attendanceService = new AttendanceService();
            cameraService = new CameraService();

            log.info("Testing database connection...");
            if (!attendanceService.testDatabaseConnection()) {
                log.error("Database connection test failed");
                Platform.runLater(() -> {
                    AlertUtil.showDatabaseError("Unable to connect to the database. Please check your database configuration.");
                    Platform.exit();
                });
                return;
            }

            log.info("Checking camera availability...");
            if (cameraService.getAvailableCameras().isEmpty()) {
                log.warn("No cameras detected");
                Platform.runLater(() -> AlertUtil.showWarning(
                    "Camera Warning",
                    "No cameras were detected. The QR scanning feature will not be available."
                ));
            } else {
                log.info("Found {} camera(s)", cameraService.getAvailableCameras().size());
            }

            log.info("Application initialization completed successfully");

        } catch (Exception e) {
            log.error("Failed to initialize application", e);
            Platform.runLater(() -> {
                AlertUtil.showError("Initialization Error",
                    "Failed to initialize the application:\n" + e.getMessage());
                Platform.exit();
            });
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting University Attendance System...");

        Main.primaryStage = primaryStage;

        try {
            primaryStage.setTitle("University Attendance System");
            primaryStage.getIcons().add(loadApplicationIcon());

            loadLoginView();

            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();

            primaryStage.setOnCloseRequest(event -> {
                log.info("Application close requested");
                cleanup();
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();
            log.info("Application started successfully");

        } catch (Exception e) {
            log.error("Failed to start application", e);
            AlertUtil.showError("Startup Error",
                "Failed to start the application:\n" + e.getMessage());
            Platform.exit();
        }
    }

    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/university-theme.css").toExternalForm());

            primaryStage.setScene(scene);

        } catch (IOException e) {
            log.error("Failed to load login view", e);
            throw new RuntimeException("Cannot load login view", e);
        }
    }

    public void loadScannerView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ScannerView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/university-theme.css").toExternalForm());

            primaryStage.setScene(scene);

        } catch (IOException e) {
            log.error("Failed to load scanner view", e);
            AlertUtil.showError("View Error", "Cannot load scanner view: " + e.getMessage());
        }
    }

    public void loadDashboardView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DashboardView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/university-theme.css").toExternalForm());

            primaryStage.setScene(scene);

        } catch (IOException e) {
            log.error("Failed to load dashboard view", e);
            AlertUtil.showError("View Error", "Cannot load dashboard view: " + e.getMessage());
        }
    }

    public void loadReportView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReportView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/university-theme.css").toExternalForm());

            primaryStage.setScene(scene);

        } catch (IOException e) {
            log.error("Failed to load report view", e);
            AlertUtil.showError("View Error", "Cannot load report view: " + e.getMessage());
        }
    }

    private Properties loadAppConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/app.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                log.warn("Application configuration file not found, using defaults");
            }
        } catch (IOException e) {
            log.error("Error loading application configuration", e);
        }
        return props;
    }

    private Image loadApplicationIcon() {
        try (InputStream iconStream = getClass().getResourceAsStream("/images/university-icon.png")) {
            if (iconStream != null) {
                return new Image(iconStream);
            }
        } catch (IOException e) {
            log.warn("Could not load application icon", e);
        }

        try (InputStream iconStream = getClass().getResourceAsStream("/images/app-icon.png")) {
            if (iconStream != null) {
                return new Image(iconStream);
            }
        } catch (IOException e) {
            log.warn("Could not load default app icon", e);
        }

        return null;
    }

    @Override
    public void stop() {
        log.info("Stopping application...");
        cleanup();
        log.info("Application stopped");
    }

    private void cleanup() {
        try {
            if (cameraService != null) {
                cameraService.shutdown();
                log.info("Camera service shutdown");
            }

            if (attendanceService != null) {
                attendanceService.cleanup();
                log.info("Attendance service cleanup completed");
            }

            DatabaseUtil.shutdown();
            log.info("Database connection pool shutdown");

        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public AttendanceService getAttendanceService() {
        return attendanceService;
    }

    public CameraService getCameraService() {
        return cameraService;
    }

    public static void main(String[] args) {
        log.info("University Attendance System starting...");

        try {
            launch(args);
        } catch (Exception e) {
            log.error("Application launch failed", e);
            System.err.println("Failed to start application: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void changeView(String viewName) {
        Platform.runLater(() -> {
            Main mainApp = (Main) getPrimaryStage().getUserData();
            if (mainApp != null) {
                switch (viewName.toLowerCase()) {
                    case "scanner":
                        mainApp.loadScannerView();
                        break;
                    case "dashboard":
                        mainApp.loadDashboardView();
                        break;
                    case "reports":
                        mainApp.loadReportView();
                        break;
                    case "login":
                    default:
                        mainApp.loadLoginView();
                        break;
                }
            }
        });
    }

    public static void setApplicationData(Object data) {
        getPrimaryStage().setUserData(data);
    }

    public static Object getApplicationData() {
        return getPrimaryStage().getUserData();
    }
}