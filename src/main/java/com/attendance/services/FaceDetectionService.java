package com.attendance.services;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Face Detection Service using OpenCV Haar Cascade
 */
public class FaceDetectionService {
    private CascadeClassifier faceDetector;
    private VideoCapture camera;
    private boolean isRunning;

    public FaceDetectionService() {
        initializeDetector();
    }

    private void initializeDetector() {
        try {
            // Extract haar cascade from resources
            InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) {
                // Use default OpenCV path or download
                String cascadePath = downloadHaarCascade();
                faceDetector = new CascadeClassifier(cascadePath);
            } else {
                File tempFile = File.createTempFile("haarcascade", ".xml");
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());
                tempFile.deleteOnExit();
            }
            System.out.println("✅ Face detector initialized!");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize face detector: " + e.getMessage());
        }
    }

    private String downloadHaarCascade() {
        // Default cascade path - user may need to download manually
        String userHome = System.getProperty("user.home");
        return userHome + "/.opencv/haarcascade_frontalface_default.xml";
    }

    public boolean startCamera() {
        try {
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("❌ Could not open webcam!");
                return false;
            }
            isRunning = true;
            System.out.println("📷 Camera started successfully!");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Camera error: " + e.getMessage());
            return false;
        }
    }

    public void stopCamera() {
        isRunning = false;
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
        System.out.println("📷 Camera stopped.");
    }

    public Mat captureFrame() {
        if (camera == null || !camera.isOpened()) {
            return null;
        }
        Mat frame = new Mat();
        camera.read(frame);
        return frame;
    }

    public RectVector detectFaces(Mat frame) {
        if (faceDetector == null || frame == null || frame.empty()) {
            return new RectVector();
        }

        // Downscale for faster processing
        double scale = 1.0;
        Mat processFrame = frame;
        if (frame.cols() > 480) {
            scale = 480.0 / frame.cols();
            processFrame = new Mat();
            resize(frame, processFrame, new Size(480, (int) (frame.rows() * scale)), 0, 0, INTER_NEAREST);
        }

        Mat grayFrame = new Mat();
        cvtColor(processFrame, grayFrame, COLOR_BGR2GRAY);
        equalizeHist(grayFrame, grayFrame);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(
                grayFrame,
                faces,
                1.1, // scaleFactor - 1.1 = more thorough scanning (detects easier)
                3, // minNeighbors - 3 = more sensitive (detects easier but slightly more false
                   // positives)
                0, // flags
                new Size(30, 30), // minSize - smaller = detects faces from further away
                new Size(400, 400) // maxSize
        );

        // Scale coordinates back to original frame size
        if (scale != 1.0) {
            RectVector scaledFaces = new RectVector();
            for (int i = 0; i < faces.size(); i++) {
                Rect face = faces.get(i);
                scaledFaces.push_back(new Rect(
                        (int) (face.x() / scale),
                        (int) (face.y() / scale),
                        (int) (face.width() / scale),
                        (int) (face.height() / scale)));
            }
            return scaledFaces;
        }

        return faces;
    }

    public Mat drawFaceRectangles(Mat frame, RectVector faces) {
        Mat result = frame.clone();
        for (int i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            rectangle(
                    result,
                    new Point(face.x(), face.y()),
                    new Point(face.x() + face.width(), face.y() + face.height()),
                    new Scalar(0, 255, 0, 0), // Green color
                    3, // thickness
                    LINE_AA,
                    0);
        }
        return result;
    }

    public Mat extractFace(Mat frame, Rect faceRect) {
        return new Mat(frame, faceRect);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isCameraAvailable() {
        return camera != null && camera.isOpened();
    }
}
