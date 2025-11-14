package com.university.attendance.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CameraService implements WebcamDiscoveryListener {
    private Webcam currentWebcam;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final List<CameraListener> listeners = new CopyOnWriteArrayList<>();
    private final Object webcamLock = new Object();

    public interface CameraListener {
        void onFrameCaptured(Image image);
        void onCameraError(String error);
        void onCameraStatusChanged(boolean connected);
    }

    public CameraService() {
        Webcam.addDiscoveryListener(this);
    }

    public List<String> getAvailableCameras() {
        List<String> cameraNames = new ArrayList<>();
        for (Webcam webcam : Webcam.getWebcams()) {
            cameraNames.add(webcam.getName());
        }
        return cameraNames;
    }

    public boolean initializeCamera(int cameraIndex) {
        return initializeCamera(cameraIndex, 640, 480);
    }

    public boolean initializeCamera(int cameraIndex, int width, int height) {
        try {
            synchronized (webcamLock) {
                if (currentWebcam != null) {
                    stopCamera();
                }

                List<Webcam> webcams = Webcam.getWebcams();
                if (cameraIndex < 0 || cameraIndex >= webcams.size()) {
                    log.error("Invalid camera index: {}", cameraIndex);
                    notifyCameraError("Invalid camera index: " + cameraIndex);
                    return false;
                }

                currentWebcam = webcams.get(cameraIndex);

                Dimension[] customSizes = new Dimension[] {
                    new Dimension(width, height),
                    WebcamResolution.VGA.getSize(),
                    WebcamResolution.HD720.getSize()
                };

                currentWebcam.setCustomViewSizes(customSizes);
                currentWebcam.setViewSize(new Dimension(width, height));

                if (!currentWebcam.open()) {
                    log.error("Failed to open camera: {}", currentWebcam.getName());
                    notifyCameraError("Failed to open camera: " + currentWebcam.getName());
                    return false;
                }

                log.info("Camera initialized successfully: {} ({}x{})",
                        currentWebcam.getName(),
                        currentWebcam.getViewSize().width,
                        currentWebcam.getViewSize().height);

                notifyCameraStatusChanged(true);
                return true;
            }

        } catch (Exception e) {
            log.error("Error initializing camera at index: " + cameraIndex, e);
            notifyCameraError("Camera initialization failed: " + e.getMessage());
            return false;
        }
    }

    public boolean initializeCameraByName(String cameraName) {
        List<String> cameras = getAvailableCameras();
        int index = cameras.indexOf(cameraName);
        return index >= 0 ? initializeCamera(index) : false;
    }

    public void startCapture() {
        if (currentWebcam == null) {
            notifyCameraError("No camera initialized");
            return;
        }

        if (isRunning.get()) {
            log.warn("Camera capture is already running");
            return;
        }

        isRunning.set(true);
        log.info("Starting camera capture");

        CompletableFuture.runAsync(() -> {
            while (isRunning.get() && currentWebcam != null) {
                try {
                    BufferedImage image = currentWebcam.getImage();
                    if (image != null) {
                        Image fxImage = convertToFXImage(image);
                        notifyFrameCaptured(fxImage);
                    } else {
                        log.warn("Received null image from camera");
                    }

                    Thread.sleep(33); // ~30 FPS

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error during camera capture", e);
                    notifyCameraError("Capture error: " + e.getMessage());
                    break;
                }
            }
            log.info("Camera capture stopped");
        });
    }

    public void stopCapture() {
        isRunning.set(false);
        log.info("Stopping camera capture");
    }

    public void stopCamera() {
        synchronized (webcamLock) {
            isRunning.set(false);

            if (currentWebcam != null) {
                currentWebcam.close();
                log.info("Camera stopped: {}", currentWebcam.getName());
                notifyCameraStatusChanged(false);
                currentWebcam = null;
            }
        }
    }

    public BufferedImage captureSingleFrame() {
        if (currentWebcam == null) {
            log.error("No camera available for single frame capture");
            return null;
        }

        try {
            BufferedImage image = currentWebcam.getImage();
            if (image != null) {
                log.debug("Single frame captured successfully");
                return image;
            } else {
                log.warn("Received null image for single frame capture");
                return null;
            }
        } catch (Exception e) {
            log.error("Error capturing single frame", e);
            notifyCameraError("Single frame capture failed: " + e.getMessage());
            return null;
        }
    }

    public boolean isCameraAvailable() {
        return currentWebcam != null && currentWebcam.isOpen();
    }

    public boolean isCapturing() {
        return isRunning.get();
    }

    public String getCurrentCameraName() {
        return currentWebcam != null ? currentWebcam.getName() : null;
    }

    public Dimension getCurrentResolution() {
        return currentWebcam != null ? currentWebcam.getViewSize() : null;
    }

    public void addListener(CameraListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CameraListener listener) {
        listeners.remove(listener);
    }

    private void notifyFrameCaptured(Image image) {
        for (CameraListener listener : listeners) {
            try {
                listener.onFrameCaptured(image);
            } catch (Exception e) {
                log.error("Error notifying frame captured listener", e);
            }
        }
    }

    private void notifyCameraError(String error) {
        for (CameraListener listener : listeners) {
            try {
                listener.onCameraError(error);
            } catch (Exception e) {
                log.error("Error notifying camera error listener", e);
            }
        }
    }

    private void notifyCameraStatusChanged(boolean connected) {
        for (CameraListener listener : listeners) {
            try {
                listener.onCameraStatusChanged(connected);
            } catch (Exception e) {
                log.error("Error notifying camera status listener", e);
            }
        }
    }

    private Image convertToFXImage(BufferedImage bufferedImage) {
        try {
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            log.error("Error converting buffered image to FX image", e);
            return null;
        }
    }

    public byte[] captureFrameAsByteArray() {
        BufferedImage image = captureSingleFrame();
        if (image == null) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error converting image to byte array", e);
            return null;
        }
    }

    public Image captureFrameAsImage() {
        BufferedImage bufferedImage = captureSingleFrame();
        return bufferedImage != null ? convertToFXImage(bufferedImage) : null;
    }

    @Override
    public void webcamFound(WebcamDiscoveryEvent event) {
        log.info("Camera discovered: {}", event.getWebcam().getName());
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent event) {
        log.info("Camera disconnected: {}", event.getWebcam().getName());
        if (currentWebcam != null && currentWebcam.equals(event.getWebcam())) {
            stopCamera();
            notifyCameraError("Camera disconnected: " + event.getWebcam().getName());
        }
    }

    public void shutdown() {
        log.info("Shutting down camera service");
        stopCapture();
        stopCamera();
        listeners.clear();
        Webcam.removeDiscoveryListener(this);
    }

    public boolean switchCamera(int cameraIndex) {
        if (cameraIndex < 0 || cameraIndex >= Webcam.getWebcams().size()) {
            return false;
        }

        stopCamera();
        return initializeCamera(cameraIndex);
    }

    public List<Dimension> getSupportedResolutions() {
        if (currentWebcam == null) {
            return new ArrayList<>();
        }

        List<Dimension> resolutions = new ArrayList<>();
        for (Dimension size : currentWebcam.getViewSizes()) {
            resolutions.add(new Dimension(size.width, size.height));
        }
        return resolutions;
    }

    public boolean setResolution(int width, int height) {
        if (currentWebcam == null) {
            return false;
        }

        try {
            boolean wasCapturing = isCapturing();

            if (wasCapturing) {
                stopCapture();
            }

            Dimension newSize = new Dimension(width, height);
            if (currentWebcam.setViewSize(newSize)) {
                log.info("Camera resolution changed to {}x{}", width, height);

                if (wasCapturing) {
                    startCapture();
                }
                return true;
            } else {
                log.error("Failed to set camera resolution to {}x{}", width, height);
                if (wasCapturing) {
                    startCapture();
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Error setting camera resolution", e);
            return false;
        }
    }
}