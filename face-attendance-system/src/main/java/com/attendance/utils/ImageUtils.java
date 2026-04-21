package com.attendance.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Utility class for image conversions between OpenCV Mat and JavaFX Image
 * Optimized for performance with caching and faster algorithms
 */
public class ImageUtils {

    // Cache for reusing WritableImage to reduce GC pressure
    private static WritableImage cachedWritableImage;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;

    /**
     * Convert OpenCV Mat to JavaFX Image - OPTIMIZED
     */
    /**
     * Convert OpenCV Mat to JavaFX Image - ULTRA OPTIMIZED
     * Writes bytes directly to the image buffer, bypassing Swing and BufferedImage.
     */
    public static Image matToImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        // Re-initialize buffer if dimensions change
        if (cachedWritableImage == null || cachedWidth != width || cachedHeight != height) {
            cachedWritableImage = new WritableImage(width, height);
            cachedWidth = width;
            cachedHeight = height;
        }

        try {
            // Get pixel writer and format (Always use BGRA for simplicity and speed
            // compatibility)
            javafx.scene.image.PixelWriter pw = cachedWritableImage.getPixelWriter();
            javafx.scene.image.PixelFormat<java.nio.ByteBuffer> pixelFormat = javafx.scene.image.PixelFormat
                    .getByteBgraInstance();

            Mat converted = new Mat();
            if (channels == 3) {
                // Convert BGR -> BGRA
                org.bytedeco.opencv.global.opencv_imgproc.cvtColor(mat, converted,
                        org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2BGRA);
            } else if (channels == 1) {
                // Convert Gray -> BGRA
                org.bytedeco.opencv.global.opencv_imgproc.cvtColor(mat, converted,
                        org.bytedeco.opencv.global.opencv_imgproc.COLOR_GRAY2BGRA);
            } else {
                return null;
            }

            // Get direct byte access
            int dataSize = width * height * 4; // 4 bytes for BGRA
            byte[] buffer = new byte[dataSize];
            converted.data().get(buffer);

            // Write to WritableImage
            pw.setPixels(0, 0, width, height, pixelFormat, buffer, 0, width * 4);

            return cachedWritableImage;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resize Mat for faster processing - uses INTER_NEAREST for speed
     */
    public static Mat resizeForProcessing(Mat mat, int maxWidth) {
        if (mat == null || mat.empty()) {
            return mat;
        }

        int width = mat.cols();
        int height = mat.rows();

        if (width <= maxWidth) {
            return mat;
        }

        double scale = (double) maxWidth / width;
        int newHeight = (int) (height * scale);

        Mat resized = new Mat();
        resize(mat, resized, new Size(maxWidth, newHeight), 0, 0, INTER_NEAREST);
        return resized;
    }

    /**
     * Resize Mat preserving aspect ratio - quality resize for display
     */
    public static Mat resizeMat(Mat mat, int maxWidth, int maxHeight) {
        if (mat == null || mat.empty()) {
            return mat;
        }

        int width = mat.cols();
        int height = mat.rows();

        double aspectRatio = (double) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = Math.min(width, maxWidth);
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newHeight = Math.min(height, maxHeight);
            newWidth = (int) (newHeight * aspectRatio);
        }

        Mat resized = new Mat();
        resize(mat, resized, new Size(newWidth, newHeight), 0, 0, INTER_LINEAR);
        return resized;
    }
}
