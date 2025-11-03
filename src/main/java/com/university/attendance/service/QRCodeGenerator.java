package com.university.attendance.service;

import com.university.attendance.model.User;
import com.university.attendance.service.QRCodeService.QRCodeData;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class QRCodeGenerator {
    private final QRCodeService qrCodeService;

    public QRCodeGenerator() {
        this.qrCodeService = new QRCodeService();
    }

    public static class QRCodeGenerationResult {
        private final boolean success;
        private final String message;
        private final String filePath;
        private final String studentId;
        private final String action;

        public QRCodeGenerationResult(boolean success, String message, String filePath, String studentId, String action) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
            this.studentId = studentId;
            this.action = action;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFilePath() { return filePath; }
        public String getStudentId() { return studentId; }
        public String getAction() { return action; }
    }

    public QRCodeGenerationResult generateStudentQR(String studentId, String name, String email,
                                                   String department, int yearLevel, String action) {
        try {
            User user = new User(studentId, name, email, department, yearLevel);
            QRCodeData qrData = qrCodeService.generateQRCodeData(user, action);

            if (qrData == null) {
                return new QRCodeGenerationResult(false, "Failed to generate QR code data", null, studentId, action);
            }

            String fileName = String.format("QR_%s_%s_%s.png",
                    studentId.replaceAll("[^a-zA-Z0-9]", "_"),
                    action,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            Path outputDir = Paths.get("generated_qr_codes");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path filePath = outputDir.resolve(fileName);

            BufferedImage qrImage = qrCodeService.createQRCodeImage(qrData.getRawData(), 300, 300);
            if (qrImage == null) {
                return new QRCodeGenerationResult(false, "Failed to create QR code image", null, studentId, action);
            }

            BufferedImage finalImage = createDecoratedQRCode(qrImage, user, action);

            boolean saved = ImageIO.write(finalImage, "PNG", filePath.toFile());
            if (!saved) {
                return new QRCodeGenerationResult(false, "Failed to save QR code image", null, studentId, action);
            }

            log.info("QR code generated successfully for student: {}, action: {}, file: {}", studentId, action, filePath);
            return new QRCodeGenerationResult(true, "QR code generated successfully", filePath.toString(), studentId, action);

        } catch (Exception e) {
            log.error("Error generating QR code for student: {}", studentId, e);
            return new QRCodeGenerationResult(false, "Error: " + e.getMessage(), null, studentId, action);
        }
    }

    public List<QRCodeGenerationResult> generateStudentQRSet(String studentId, String name, String email,
                                                           String department, int yearLevel) {
        List<QRCodeGenerationResult> results = new ArrayList<>();

        results.add(generateStudentQR(studentId, name, email, department, yearLevel, "login"));
        results.add(generateStudentQR(studentId, name, email, department, yearLevel, "logout"));

        return results;
    }

    public BufferedImage createDecoratedQRCode(BufferedImage qrImage, User user, String action) {
        int width = 400;
        int height = 500;

        BufferedImage decoratedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = decoratedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(0, 51, 102));
        g2d.fillRect(0, 0, width, 80);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String title = "UNIVERSITY ATTENDANCE";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleWidth = titleMetrics.stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 35);

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String actionText = action.toUpperCase();
        FontMetrics actionMetrics = g2d.getFontMetrics();
        int actionWidth = actionMetrics.stringWidth(actionText);
        g2d.drawString(actionText, (width - actionWidth) / 2, 60);

        int qrX = (width - qrImage.getWidth()) / 2;
        int qrY = 100;
        g2d.drawImage(qrImage, qrX, qrY, null);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Name:", 20, 440);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(user.getName(), 70, 440);

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("ID:", 20, 460);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(user.getStudentId(), 70, 460);

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Dept:", 20, 480);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(user.getDepartment() != null ? user.getDepartment() : "N/A", 70, 480);

        g2d.setColor(new Color(0, 51, 102));
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        String validityText = "Valid: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        FontMetrics validityMetrics = g2d.getFontMetrics();
        int validityWidth = validityMetrics.stringWidth(validityText);
        g2d.drawString(validityText, (width - validityWidth) / 2, 495);

        g2d.dispose();
        return decoratedImage;
    }

    public boolean generateBatchQRCodes(List<User> users, String action) {
        boolean allSuccessful = true;

        String batchDir = String.format("batch_qr_%s_%s_%s",
                action,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                UUID.randomUUID().toString().substring(0, 8));

        Path batchPath = Paths.get("generated_qr_codes", batchDir);

        try {
            Files.createDirectories(batchPath);
        } catch (IOException e) {
            log.error("Failed to create batch directory: {}", batchPath, e);
            return false;
        }

        for (User user : users) {
            try {
                String fileName = String.format("QR_%s_%s.png",
                        user.getStudentId().replaceAll("[^a-zA-Z0-9]", "_"),
                        action);

                Path filePath = batchPath.resolve(fileName);

                QRCodeData qrData = qrCodeService.generateQRCodeData(user, action);
                if (qrData == null) {
                    log.error("Failed to generate QR data for user: {}", user.getStudentId());
                    allSuccessful = false;
                    continue;
                }

                BufferedImage qrImage = qrCodeService.createQRCodeImage(qrData.getRawData(), 300, 300);
                if (qrImage == null) {
                    log.error("Failed to create QR image for user: {}", user.getStudentId());
                    allSuccessful = false;
                    continue;
                }

                BufferedImage decoratedImage = createDecoratedQRCode(qrImage, user, action);

                boolean saved = ImageIO.write(decoratedImage, "PNG", filePath.toFile());
                if (!saved) {
                    log.error("Failed to save QR image for user: {}", user.getStudentId());
                    allSuccessful = false;
                } else {
                    log.info("Generated QR code for user: {} -> {}", user.getStudentId(), filePath);
                }

            } catch (Exception e) {
                log.error("Error generating QR code for user: {}", user.getStudentId(), e);
                allSuccessful = false;
            }
        }

        log.info("Batch QR generation completed. Success: {}, Output directory: {}", allSuccessful, batchPath);
        return allSuccessful;
    }

    public String validateQRCodeImage(String qrCodeFilePath) {
        try {
            File qrFile = new File(qrCodeFilePath);
            if (!qrFile.exists()) {
                return "QR code file does not exist";
            }

            BufferedImage image = ImageIO.read(qrFile);
            if (image == null) {
                return "QR code file is not a valid image";
            }

            BufferedImage qrImage = extractQRCodeFromDecorated(image);
            if (qrImage == null) {
                return "Could not extract QR code from image";
            }

            QRCodeData qrData = qrCodeService.scanQRCode(qrImage);
            if (qrData == null) {
                return "No valid QR code found in image";
            }

            if (!qrData.isValid()) {
                return "QR code is invalid or corrupted";
            }

            return "Valid QR code:\n" + qrData.getDisplayInfo();

        } catch (Exception e) {
            log.error("Error validating QR code image: {}", qrCodeFilePath, e);
            return "Error validating QR code: " + e.getMessage();
        }
    }

    private BufferedImage extractQRCodeFromDecorated(BufferedImage decoratedImage) {
        int qrX = 50;
        int qrY = 100;
        int qrSize = 300;

        if (decoratedImage.getWidth() < qrX + qrSize || decoratedImage.getHeight() < qrY + qrSize) {
            log.warn("Decorated image is too small to contain QR code");
            return null;
        }

        return decoratedImage.getSubimage(qrX, qrY, qrSize, qrSize);
    }

    public void createSampleQRCodes() {
        log.info("Creating sample QR codes for testing...");

        List<User> sampleUsers = List.of(
                new User("2024-CS-001", "Alice Johnson", "alice.j@university.edu", "Computer Science", 3),
                new User("2024-EE-002", "Bob Smith", "bob.s@university.edu", "Electrical Engineering", 2),
                new User("2024-ME-003", "Carol Davis", "carol.d@university.edu", "Mechanical Engineering", 4)
        );

        for (User user : sampleUsers) {
            generateStudentQRSet(user.getStudentId(), user.getName(), user.getEmail(),
                    user.getDepartment(), user.getYearLevel());
        }
    }

    public static void main(String[] args) {
        QRCodeGenerator generator = new QRCodeGenerator();

        QRCodeGenerationResult result = generator.generateStudentQR(
                "2024-CS-001",
                "Alice Johnson",
                "alice.j@university.edu",
                "Computer Science",
                3,
                "login"
        );

        if (result.isSuccess()) {
            System.out.println("QR code generated: " + result.getFilePath());
        } else {
            System.out.println("Failed to generate QR code: " + result.getMessage());
        }

        generator.createSampleQRCodes();
    }
}