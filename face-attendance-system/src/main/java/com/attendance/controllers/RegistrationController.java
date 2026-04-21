package com.attendance.controllers;

import com.attendance.models.User;
import com.attendance.services.FaceDetectionService;
import com.attendance.services.FaceRecognitionService;
import com.attendance.utils.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Registration Controller - Registers new users with face capture
 */
public class RegistrationController implements Initializable {

    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<String> departmentCombo;
    @FXML
    private ImageView cameraView;
    @FXML
    private Button captureBtn;
    @FXML
    private Button registerBtn;
    @FXML
    private Label statusLabel;

    private FaceDetectionService detectionService;
    private FaceRecognitionService recognitionService;
    private ScheduledExecutorService cameraExecutor;
    private Mat capturedFace;
    private boolean faceCaptured = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        detectionService = new FaceDetectionService();
        recognitionService = new FaceRecognitionService();

        // Setup department dropdown
        departmentCombo.getItems().addAll(
                "Computer Science",
                "Information Technology",
                "Electronics",
                "Mechanical",
                "Civil Engineering",
                "Administration",
                "Other");
        departmentCombo.setValue("Computer Science");

        registerBtn.setDisable(true);
        startCamera();
    }

    private void startCamera() {
        if (!detectionService.startCamera()) {
            statusLabel.setText("❌ Camera not available!");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
            return;
        }

        cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        cameraExecutor.scheduleAtFixedRate(() -> {
            Mat frame = detectionService.captureFrame();
            if (frame != null && !frame.empty()) {
                // Resize for faster display/processing if frame is huge
                if (frame.cols() > 640) {
                    org.bytedeco.opencv.opencv_core.Size newSize = new org.bytedeco.opencv.opencv_core.Size(640,
                            (int) (frame.rows() * (640.0 / frame.cols())));
                    org.bytedeco.opencv.global.opencv_imgproc.resize(frame, frame, newSize);
                }

                RectVector faces = detectionService.detectFaces(frame);
                Mat displayFrame = detectionService.drawFaceRectangles(frame, faces);

                Platform.runLater(() -> {
                    cameraView.setImage(ImageUtils.matToImage(displayFrame));
                    if (faces.size() > 0 && !faceCaptured) {
                        statusLabel.setText("✅ Face detected! Click 'Capture Face'");
                        statusLabel.setStyle("-fx-text-fill: #28a745;");
                        captureBtn.setDisable(false);
                    } else if (!faceCaptured) {
                        statusLabel.setText("👀 Position your face in the frame...");
                        statusLabel.setStyle("-fx-text-fill: #ffc107;");
                    }
                });
            }
        }, 0, 80, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void captureFace() {
        Mat frame = detectionService.captureFrame();
        if (frame != null) {
            RectVector faces = detectionService.detectFaces(frame);
            if (faces.size() > 0) {
                capturedFace = detectionService.extractFace(frame, faces.get(0));
                faceCaptured = true;
                registerBtn.setDisable(false);
                captureBtn.setText("✓ Captured!");
                captureBtn.setDisable(true);
                statusLabel.setText("✅ Face captured! Fill details and register.");
                statusLabel.setStyle("-fx-text-fill: #28a745;");
            }
        }
    }

    @FXML
    private void registerUser() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String department = departmentCombo.getValue();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a name!");
            return;
        }

        if (capturedFace == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please capture your face first!");
            return;
        }

        User user = new User(name, email, department);
        boolean success = recognitionService.registerFace(user, capturedFace);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "🎉 User '" + name + "' registered successfully!");
            closeWindow();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed. Please try again.");
        }
    }

    @FXML
    private void retakePhoto() {
        faceCaptured = false;
        capturedFace = null;
        captureBtn.setText("📸 Capture Face");
        captureBtn.setDisable(false);
        registerBtn.setDisable(true);
        statusLabel.setText("👀 Position your face in the frame...");
    }

    @FXML
    private void closeWindow() {
        stopCamera();
        Stage stage = (Stage) cameraView.getScene().getWindow();
        stage.close();
    }

    private void stopCamera() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        detectionService.stopCamera();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
