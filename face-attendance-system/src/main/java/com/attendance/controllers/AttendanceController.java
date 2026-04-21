package com.attendance.controllers;

import com.attendance.services.AttendanceService;
import com.attendance.services.FaceDetectionService;
import com.attendance.services.FaceRecognitionService;
import com.attendance.services.FaceRecognitionService.RecognitionResult;
import com.attendance.utils.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
 * Attendance Controller - Marks attendance via face recognition
 */
public class AttendanceController implements Initializable {

    @FXML
    private ImageView cameraView;
    @FXML
    private Label statusLabel;
    @FXML
    private Label recognizedLabel;
    @FXML
    private Label confidenceLabel;
    @FXML
    private ListView<String> markedList;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;

    private FaceDetectionService detectionService;
    private FaceRecognitionService recognitionService;
    private AttendanceService attendanceService;
    private ScheduledExecutorService cameraExecutor;
    private Set<Integer> markedToday;
    private boolean isRunning = false;

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
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
            return;
        }

        isRunning = true;
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        statusLabel.setText("📷 Scanning for faces...");
        statusLabel.setStyle("-fx-text-fill: #17a2b8;");

        cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        cameraExecutor.scheduleAtFixedRate(this::processFrame, 0, 100, TimeUnit.MILLISECONDS);
    }

    // Frame counter to throttle recognition
    private int frameCount = 0;

    private void processFrame() {
        if (!isRunning)
            return;

        // 1. Capture Frame
        Mat frame = detectionService.captureFrame();
        if (frame == null || frame.empty())
            return;

        // 2. Resize for faster display/processing if frame is huge
        if (frame.cols() > 640) {
            org.bytedeco.opencv.opencv_core.Size newSize = new org.bytedeco.opencv.opencv_core.Size(640,
                    (int) (frame.rows() * (640.0 / frame.cols())));
            org.bytedeco.opencv.global.opencv_imgproc.resize(frame, frame, newSize);
        }

        // 3. Detect Faces
        RectVector faces = detectionService.detectFaces(frame);
        Mat displayFrame = detectionService.drawFaceRectangles(frame, faces);

        // 4. Update UI (Camera Feed)
        Platform.runLater(() -> {
            cameraView.setImage(ImageUtils.matToImage(displayFrame));
        });

        // 5. Run Recognition (Throttled - every 5 frames)
        frameCount++;
        if (frameCount % 5 != 0) {
            return; // Skip recognition this frame
        }

        // Process each detected face
        for (int i = 0; i < faces.size(); i++) {
            Rect faceRect = faces.get(i);
            Mat faceImage = detectionService.extractFace(frame, faceRect);

            RecognitionResult result = recognitionService.recognize(faceImage);

            Platform.runLater(() -> {
                if (result.isRecognized()) {
                    recognizedLabel.setText("👤 " + result.getName());
                    confidenceLabel.setText(String.format("Confidence: %.1f%%", result.getConfidence()));

                    // Mark attendance if not already marked
                    if (!markedToday.contains(result.getUserId())) {
                        boolean marked = attendanceService.markAttendance(
                                result.getUserId(),
                                result.getName());

                        if (marked) {
                            markedToday.add(result.getUserId());
                            markedList.getItems().add("✅ " + result.getName() + " - " +
                                    java.time.LocalTime.now().format(
                                            java.time.format.DateTimeFormatter.ofPattern("hh:mm a")));

                            statusLabel.setText("✅ Attendance marked for " + result.getName());
                            statusLabel.setStyle("-fx-text-fill: #28a745;");
                        }
                    } else {
                        statusLabel.setText("ℹ️ " + result.getName() + " already marked today");
                        statusLabel.setStyle("-fx-text-fill: #6c757d;");
                    }
                } else {
                    recognizedLabel.setText("👤 Unknown");
                    confidenceLabel.setText("Not in database");
                    statusLabel.setText("⚠️ Face not recognized. Please register first.");
                    statusLabel.setStyle("-fx-text-fill: #ffc107;");
                }
            });
        }

        if (faces.size() == 0) {
            Platform.runLater(() -> {
                recognizedLabel.setText("👤 No face detected");
                confidenceLabel.setText("-");
                statusLabel.setText("📷 Scanning for faces...");
                statusLabel.setStyle("-fx-text-fill: #17a2b8;");
            });
        }
    }

    @FXML
    private void stopAttendance() {
        isRunning = false;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        detectionService.stopCamera();

        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        statusLabel.setText("⏹️ Attendance marking stopped");
        statusLabel.setStyle("-fx-text-fill: #6c757d;");
    }

    @FXML
    private void closeWindow() {
        stopAttendance();
        Stage stage = (Stage) cameraView.getScene().getWindow();
        stage.close();
    }
}
