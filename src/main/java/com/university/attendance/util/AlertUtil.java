package com.university.attendance.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Optional;

public class AlertUtil {

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        VBox vBox = new VBox(contentLabel);
        vBox.setSpacing(10);

        alert.getDialogPane().setContent(vBox);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        VBox vBox = new VBox(contentLabel);
        vBox.setSpacing(10);

        alert.getDialogPane().setContent(vBox);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        VBox vBox = new VBox(contentLabel);
        vBox.setSpacing(10);

        alert.getDialogPane().setContent(vBox);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        VBox vBox = new VBox(contentLabel);
        vBox.setSpacing(10);

        alert.getDialogPane().setContent(vBox);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        VBox vBox = new VBox(contentLabel);
        vBox.setSpacing(10);

        alert.getDialogPane().setContent(vBox);
        alert.showAndWait();
    }

    public static void showQRCodeSuccess(String studentName, String studentId, String action) {
        String message = String.format(
            "Attendance %s successful!\n\nStudent: %s\nID: %s\nTime: %s",
            action,
            studentName,
            studentId,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        showSuccess("Attendance Recorded", message);
    }

    public static void showQRCodeError(String error) {
        showError("QR Code Error", "Failed to process QR code:\n\n" + error);
    }

    public static void showCameraError(String error) {
        showError("Camera Error", "Camera access error:\n\n" + error +
                 "\n\nPlease check:\n" +
                 "• Camera is connected\n" +
                 "• Camera permissions are granted\n" +
                 "• Camera is not being used by another application");
    }

    public static void showDatabaseError(String error) {
        showError("Database Error", "Database connection error:\n\n" + error +
                 "\n\nPlease check:\n" +
                 "• Database server is running\n" +
                 "• Network connection is stable\n" +
                 "• Database credentials are correct");
    }
}