package com.attendance.services;

import com.attendance.database.DatabaseManager;
import com.attendance.models.User;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Face Recognition Service using LBPH (Local Binary Patterns Histograms)
 */
public class FaceRecognitionService {
    private LBPHFaceRecognizer recognizer;
    private static final String FACES_DIR = "faces/";
    private static final double CONFIDENCE_THRESHOLD = 70.0;

    public FaceRecognitionService() {
        recognizer = LBPHFaceRecognizer.create();
        ensureFacesDirectory();
        trainRecognizer();
    }

    private void ensureFacesDirectory() {
        File facesDir = new File(FACES_DIR);
        if (!facesDir.exists()) {
            facesDir.mkdirs();
        }
    }

    public void trainRecognizer() {
        try {
            List<User> users = getAllUsersWithFaces();
            if (users.isEmpty()) {
                System.out.println("ℹ️ No faces to train on yet.");
                return;
            }

            MatVector images = new MatVector();
            Mat labels = new Mat(users.size(), 1, CV_32SC1);

            int index = 0;
            for (User user : users) {
                String imagePath = user.getImagePath();
                if (imagePath != null && new File(imagePath).exists()) {
                    Mat img = imread(imagePath, IMREAD_GRAYSCALE);
                    if (!img.empty()) {
                        resize(img, img, new Size(150, 150));
                        images.push_back(img);
                        labels.ptr(index).putInt(user.getId());
                        index++;
                    }
                }
            }

            if (images.size() > 0) {
                recognizer.train(images, labels);
                System.out.println("✅ Recognizer trained with " + images.size() + " faces!");
            }

        } catch (Exception e) {
            System.err.println("❌ Training failed: " + e.getMessage());
        }
    }

    public RecognitionResult recognize(Mat faceImage) {
        if (faceImage == null || faceImage.empty()) {
            return new RecognitionResult(-1, "Unknown", 0);
        }

        try {
            Mat grayFace = new Mat();
            if (faceImage.channels() > 1) {
                cvtColor(faceImage, grayFace, COLOR_BGR2GRAY);
            } else {
                grayFace = faceImage.clone();
            }
            resize(grayFace, grayFace, new Size(150, 150));

            int[] predictedLabel = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(grayFace, predictedLabel, confidence);

            int userId = predictedLabel[0];
            double conf = confidence[0];

            // Lower confidence = better match in LBPH
            if (conf < CONFIDENCE_THRESHOLD) {
                User user = getUserById(userId);
                if (user != null) {
                    return new RecognitionResult(userId, user.getName(), 100 - conf);
                }
            }

        } catch (Exception e) {
            System.err.println("Recognition error: " + e.getMessage());
        }

        return new RecognitionResult(-1, "Unknown", 0);
    }

    public boolean registerFace(User user, Mat faceImage) {
        try {
            // Ensure faces directory exists
            ensureFacesDirectory();

            String imagePath = FACES_DIR + "user_" + System.currentTimeMillis() + ".jpg";

            Mat grayFace = new Mat();
            if (faceImage.channels() > 1) {
                cvtColor(faceImage, grayFace, COLOR_BGR2GRAY);
            } else {
                grayFace = faceImage.clone();
            }
            resize(grayFace, grayFace, new Size(150, 150));

            boolean saved = imwrite(imagePath, grayFace);
            if (!saved) {
                System.err.println("❌ Failed to save face image to: " + imagePath);
                return false;
            }
            user.setImagePath(imagePath);

            // Save to database - this is the critical operation
            saveUser(user);
            System.out.println("✅ Face registered for: " + user.getName());

            // Retrain recognizer in background - don't fail if this errors
            try {
                trainRecognizer();
            } catch (Exception trainError) {
                System.out.println("⚠️ Retrain deferred: " + trainError.getMessage());
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void saveUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, department, image_path) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            // Handle empty email - set to null or generate unique one
            String email = user.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = null; // Allow null emails (not enforced as unique)
            }
            pstmt.setString(2, email);
            pstmt.setString(3, user.getDepartment());
            pstmt.setString(4, user.getImagePath());
            pstmt.executeUpdate();

            // Get the last inserted ID using SQLite's last_insert_rowid()
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
    }

    private List<User> getAllUsersWithFaces() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE image_path IS NOT NULL";

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setDepartment(rs.getString("department"));
                user.setImagePath(rs.getString("image_path"));
                users.add(user);
            }
        }
        return users;
    }

    private User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setDepartment(rs.getString("department"));
                user.setImagePath(rs.getString("image_path"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Inner class for recognition results
    public static class RecognitionResult {
        private final int userId;
        private final String name;
        private final double confidence;

        public RecognitionResult(int userId, String name, double confidence) {
            this.userId = userId;
            this.name = name;
            this.confidence = confidence;
        }

        public int getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public double getConfidence() {
            return confidence;
        }

        public boolean isRecognized() {
            return userId > 0;
        }
    }
}
