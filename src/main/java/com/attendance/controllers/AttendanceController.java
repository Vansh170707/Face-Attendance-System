package com.attendance.controllers;

import com.attendance.services.AttendanceService;
import com.attendance.services.FaceDetectionService;
import com.attendance.services.FaceRecognitionService;
import com.attendance.services.FaceRecognitionService.RecognitionResult;
import com.attendance.ui.ToastNotification;
import com.attendance.utils.ImageUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.*;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Attendance Controller — Marks attendance via face recognition.
 * Now embedded inside the main layout.
 */
public class AttendanceController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private StackPane cameraFrame;
    @FXML private Label statusLabel;
    @FXML private Label recognizedLabel;
    @FXML private Label confidenceLabel;
    @FXML private ListView<String> markedList;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;

    private FaceDetectionService detectionService;
    private FaceRecognitionService recognitionService;
    private AttendanceService attendanceService;
    private ScheduledExecutorService cameraExecutor;
    private Set<Integer> markedToday;
    private boolean isRunning = false;
    private int frameCount = 0;
    private Timeline pulseAnimation;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        detectionService = new FaceDetectionService();
        recognitionService = new FaceRecognitionService();
        attendanceService = new AttendanceService();
        markedToday = new HashSet<>();

        stopBtn.setDisable(true);
        statusLabel.setText("Click 'Start' to begin attendance marking");
    }

    @FXML
    private void startAttendance() {
        if (!detectionService.startCamera()) {
            statusLabel.setText("❌ Camera not available!");
            statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }

        isRunning = true;
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        statusLabel.setText("📷 Scanning for faces...");
        statusLabel.setStyle("-fx-text-fill: #1E90FF;");

        // Set camera frame to scanning glow with pulse animation
        cameraFrame.getStyleClass().add("camera-glow-scanning");
        startPulseAnimation();

        cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        cameraExecutor.scheduleAtFixedRate(this::processFrame, 0, 120, TimeUnit.MILLISECONDS);
    }

    private void processFrame() {
        if (!isRunning) return;

        Mat frame = detectionService.captureFrame();
        if (frame == null || frame.empty()) return;

        if (frame.cols() > 640) {
            org.bytedeco.opencv.opencv_core.Size newSize = new org.bytedeco.opencv.opencv_core.Size(640,
                    (int) (frame.rows() * (640.0 / frame.cols())));
            org.bytedeco.opencv.global.opencv_imgproc.resize(frame, frame, newSize);
        }

        RectVector faces = detectionService.detectFaces(frame);
        Mat displayFrame = detectionService.drawFaceRectangles(frame, faces);

        Platform.runLater(() -> cameraView.setImage(ImageUtils.matToImage(displayFrame)));

        frameCount++;
        if (frameCount % 5 != 0) return;

        for (int i = 0; i < faces.size(); i++) {
            Rect faceRect = faces.get(i);
            Mat faceImage = detectionService.extractFace(frame, faceRect);
            RecognitionResult result = recognitionService.recognize(faceImage);

            Platform.runLater(() -> {
                if (result.isRecognized()) {
                    recognizedLabel.setText("👤 " + result.getName());
                    confidenceLabel.setText(String.format("Confidence: %.1f%%", result.getConfidence()));

                    // Set glow to green on recognition
                    cameraFrame.getStyleClass().remove("camera-glow-scanning");
                    cameraFrame.getStyleClass().remove("camera-glow-fail");
                    if (!cameraFrame.getStyleClass().contains("camera-glow-success")) {
                        cameraFrame.getStyleClass().add("camera-glow-success");
                    }

                    if (!markedToday.contains(result.getUserId())) {
                        boolean marked = attendanceService.markAttendance(result.getUserId(), result.getName());
                        if (marked) {
                            markedToday.add(result.getUserId());
                            markedList.getItems().add("✅ " + result.getName() + " — " +
                                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));

                            statusLabel.setText("✅ Attendance marked for " + result.getName());
                            statusLabel.setStyle("-fx-text-fill: #00C896;");

                            // Show toast notification
                            try {
                                Stage stage = (Stage) cameraView.getScene().getWindow();
                                ToastNotification.show(stage, "Attendance Marked",
                                        result.getName() + " marked present at " +
                                                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
                                        ToastNotification.ToastType.SUCCESS);
                            } catch (Exception ignored) {}
                        }
                    } else {
                        statusLabel.setText("ℹ️ " + result.getName() + " already marked today");
                        statusLabel.setStyle("-fx-text-fill: #8899AA;");
                    }
                } else {
                    recognizedLabel.setText("👤 Unknown");
                    confidenceLabel.setText("Not in database");
                    statusLabel.setText("⚠️ Face not recognized. Please register first.");
                    statusLabel.setStyle("-fx-text-fill: #FFD700;");

                    // Set glow to red
                    cameraFrame.getStyleClass().remove("camera-glow-scanning");
                    cameraFrame.getStyleClass().remove("camera-glow-success");
                    if (!cameraFrame.getStyleClass().contains("camera-glow-fail")) {
                        cameraFrame.getStyleClass().add("camera-glow-fail");
                    }
                }
            });
        }

        if (faces.size() == 0) {
            Platform.runLater(() -> {
                recognizedLabel.setText("👤 No face detected");
                confidenceLabel.setText("-");
                statusLabel.setText("📷 Scanning for faces...");
                statusLabel.setStyle("-fx-text-fill: #1E90FF;");
            });
        }
    }

    @FXML
    private void stopAttendance() {
        isRunning = false;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            try {
                cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                cameraExecutor.shutdownNow();
            }
        }
        detectionService.stopCamera();

        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        statusLabel.setText("⏹️ Attendance marking stopped");
        statusLabel.setStyle("-fx-text-fill: #8899AA;");

        cameraFrame.getStyleClass().removeAll("camera-glow-scanning", "camera-glow-success", "camera-glow-fail");
        stopPulseAnimation();
    }

    /** Pulsing glow animation during scanning */
    private void startPulseAnimation() {
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(
                20, javafx.scene.paint.Color.web("#1E90FF", 0.5));
        cameraFrame.setEffect(glow);

        pulseAnimation = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
            new KeyFrame(javafx.util.Duration.millis(700), new KeyValue(glow.radiusProperty(), 35)),
            new KeyFrame(javafx.util.Duration.millis(1400), new KeyValue(glow.radiusProperty(), 15))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
        cameraFrame.setEffect(null);
    }
}
