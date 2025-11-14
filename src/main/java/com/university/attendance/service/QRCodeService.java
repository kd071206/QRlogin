package com.university.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.university.attendance.model.User;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class QRCodeService {
    private final ObjectMapper objectMapper;
    private final MultiFormatReader reader;
    private final Map<String, LocalDateTime> recentScans;
    private static final int SCAN_TIMEOUT_SECONDS = 10;

    public interface QRCodeScanCallback {
        void onQRCodeDetected(QRCodeData qrData);
        void onQRCodeError(String error);
        void onInvalidQRCode(String error);
    }

    public static class QRCodeData {
        private final String type;
        private final String userId;
        private final String studentId;
        private final String name;
        private final LocalDateTime timestamp;
        private final String action;
        private final String checksum;
        private final String rawData;

        public QRCodeData(String type, String userId, String studentId, String name,
                         LocalDateTime timestamp, String action, String checksum, String rawData) {
            this.type = type;
            this.userId = userId;
            this.studentId = studentId;
            this.name = name;
            this.timestamp = timestamp;
            this.action = action;
            this.checksum = checksum;
            this.rawData = rawData;
        }

        public String getType() { return type; }
        public String getUserId() { return userId; }
        public String getStudentId() { return studentId; }
        public String getName() { return name; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getChecksum() { return checksum; }
        public String getRawData() { return rawData; }

        public boolean isLogin() { return "login".equalsIgnoreCase(action); }
        public boolean isLogout() { return "logout".equalsIgnoreCase(action); }

        public boolean isValid() {
            if (type == null || !type.equals("attendance")) {
                return false;
            }

            if (studentId == null || studentId.trim().isEmpty()) {
                return false;
            }

            if (name == null || name.trim().isEmpty()) {
                return false;
            }

            if (action == null || (!action.equals("login") && !action.equals("logout"))) {
                return false;
            }

            if (timestamp == null) {
                return false;
            }

            if (checksum == null || checksum.trim().isEmpty()) {
                return false;
            }

            return validateChecksum();
        }

        private boolean validateChecksum() {
            String expectedChecksum = calculateChecksum(userId != null ? userId : studentId, timestamp, action);
            return expectedChecksum.equalsIgnoreCase(checksum);
        }

        private String calculateChecksum(String identifier, LocalDateTime ts, String act) {
            String data = identifier + ts.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + act;
            return md5(data);
        }

        private String md5(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(input.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                return "";
            }
        }

        public String getDisplayInfo() {
            return String.format("Student: %s (%s)\nAction: %s\nTime: %s",
                    name, studentId, action.toUpperCase(),
                    timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    public QRCodeService() {
        this.objectMapper = new ObjectMapper();
        this.reader = new MultiFormatReader();
        this.recentScans = new HashMap<>();
    }

    public CompletableFuture<QRCodeData> scanQRCodeAsync(BufferedImage image) {
        return CompletableFuture.supplyAsync(() -> scanQRCode(image));
    }

    public QRCodeData scanQRCode(BufferedImage image) {
        if (image == null) {
            log.warn("Received null image for QR scanning");
            return null;
        }

        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = reader.decode(bitmap);
            String qrContent = result.getText();

            log.debug("QR code detected: {}", qrContent);

            return parseQRCodeContent(qrContent);

        } catch (NotFoundException e) {
            log.debug("No QR code found in image");
            return null;
        } catch (Exception e) {
            log.error("Error scanning QR code", e);
            return null;
        }
    }

    public QRCodeData parseQRCodeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Empty QR code content");
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);

            String type = getTextValue(jsonNode, "type");
            String userId = getTextValue(jsonNode, "userId");
            String studentId = getTextValue(jsonNode, "studentId");
            String name = getTextValue(jsonNode, "name");
            String action = getTextValue(jsonNode, "action");
            String checksum = getTextValue(jsonNode, "checksum");

            LocalDateTime timestamp = null;
            String timestampStr = getTextValue(jsonNode, "timestamp");
            if (timestampStr != null) {
                try {
                    timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException e) {
                    log.warn("Invalid timestamp format in QR code: {}", timestampStr);
                }
            }

            QRCodeData qrData = new QRCodeData(type, userId, studentId, name, timestamp, action, checksum, content);

            if (qrData.isValid()) {
                log.info("Valid QR code parsed: student={}, action={}", studentId, action);
                return qrData;
            } else {
                log.warn("Invalid QR code content: {}", content);
                return null;
            }

        } catch (Exception e) {
            log.error("Error parsing QR code JSON content: {}", content, e);
            return null;
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    public boolean isRecentDuplicateScan(String studentId, String action) {
        String key = studentId + ":" + action;
        LocalDateTime lastScan = recentScans.get(key);

        if (lastScan == null) {
            recentScans.put(key, LocalDateTime.now());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (lastScan.plusSeconds(SCAN_TIMEOUT_SECONDS).isAfter(now)) {
            log.debug("Duplicate scan detected for student: {}, action: {}", studentId, action);
            return true;
        }

        recentScans.put(key, now);
        return false;
    }

    public void cleanupOldScans() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        recentScans.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    public QRCodeData generateQRCodeData(User user, String action) {
        if (user == null || action == null) {
            return null;
        }

        try {
            LocalDateTime timestamp = LocalDateTime.now();
            String checksum = calculateChecksum(user.getId(), timestamp, action);

            String jsonContent = String.format(
                    "{\"type\":\"attendance\",\"userId\":\"%s\",\"studentId\":\"%s\",\"name\":\"%s\",\"timestamp\":\"%s\",\"action\":\"%s\",\"checksum\":\"%s\"}",
                    user.getId(),
                    user.getStudentId(),
                    user.getName(),
                    timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    action,
                    checksum
            );

            return new QRCodeData("attendance", user.getId(), user.getStudentId(),
                    user.getName(), timestamp, action, checksum, jsonContent);

        } catch (Exception e) {
            log.error("Error generating QR code data for user: {}", user.getStudentId(), e);
            return null;
        }
    }

    private String calculateChecksum(String userId, LocalDateTime timestamp, String action) {
        String data = userId + timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + action;
        return md5(data);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public BufferedImage createQRCodeImage(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            return image;

        } catch (Exception e) {
            log.error("Error creating QR code image", e);
            return null;
        }
    }

    public byte[] createQRCodeBytes(String content, int width, int height) {
        BufferedImage image = createQRCodeImage(content, width, height);
        if (image == null) {
            return null;
        }

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error converting QR code image to bytes", e);
            return null;
        }
    }

    public boolean isValidQRCodeFormat(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            return jsonNode.has("type") &&
                   jsonNode.has("studentId") &&
                   jsonNode.has("action") &&
                   "attendance".equals(jsonNode.get("type").asText());
        } catch (Exception e) {
            return false;
        }
    }

    public void processFrameWithCallback(BufferedImage image, QRCodeScanCallback callback) {
        CompletableFuture.supplyAsync(() -> scanQRCode(image))
                .thenAccept(qrData -> {
                    if (qrData != null) {
                        if (isRecentDuplicateScan(qrData.getStudentId(), qrData.getAction())) {
                            callback.onInvalidQRCode("Duplicate scan detected. Please wait " + SCAN_TIMEOUT_SECONDS + " seconds.");
                            return;
                        }

                        if (qrData.isValid()) {
                            callback.onQRCodeDetected(qrData);
                        } else {
                            callback.onInvalidQRCode("Invalid QR code format or checksum");
                        }
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error processing QR code frame", throwable);
                    callback.onQRCodeError("QR code processing failed: " + throwable.getMessage());
                    return null;
                });
    }
}