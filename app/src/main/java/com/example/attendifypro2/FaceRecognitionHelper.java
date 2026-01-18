package com.example.attendifypro2;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FaceRecognitionHelper {

    public interface FaceDetectionCallback {
        void onFaceDetected(Bitmap faceBitmap);
        void onNoFaceDetected();
        void onError(String error);
    }

    public interface FaceMatchCallback {
        void onMatch(boolean isMatch, float similarity);
        void onError(String error);
    }

    /**
     * Detect face in a bitmap image
     */
    public static void detectFace(Bitmap bitmap, FaceDetectionCallback callback) {
        // Configure face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        // Face detected
                        callback.onFaceDetected(bitmap);
                    } else {
                        // No face detected
                        callback.onNoFaceDetected();
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError("Face detection failed: " + e.getMessage());
                });
    }

    /**
     * Compare two face bitmaps for similarity
     */
    public static void compareFaces(Bitmap face1, Bitmap face2, int threshold, FaceMatchCallback callback) {
        try {
            // Resize both images to same size for comparison
            int size = 100;
            Bitmap resized1 = Bitmap.createScaledBitmap(face1, size, size, false);
            Bitmap resized2 = Bitmap.createScaledBitmap(face2, size, size, false);

            // Calculate pixel-by-pixel similarity
            long totalDiff = 0;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    int pixel1 = resized1.getPixel(x, y);
                    int pixel2 = resized2.getPixel(x, y);

                    int rDiff = Math.abs(android.graphics.Color.red(pixel1) - android.graphics.Color.red(pixel2));
                    int gDiff = Math.abs(android.graphics.Color.green(pixel1) - android.graphics.Color.green(pixel2));
                    int bDiff = Math.abs(android.graphics.Color.blue(pixel1) - android.graphics.Color.blue(pixel2));

                    totalDiff += rDiff + gDiff + bDiff;
                }
            }

            // Calculate similarity percentage
            long maxDiff = 255L * 3 * size * size; // Maximum possible difference
            float similarity = 100 - ((totalDiff * 100f) / maxDiff);

            boolean isMatch = similarity >= threshold;
            callback.onMatch(isMatch, similarity);

        } catch (Exception e) {
            callback.onError("Face comparison failed: " + e.getMessage());
        }
    }

    /**
     * Convert bitmap to Base64 string for Firebase storage
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * Convert Base64 string back to bitmap
     */
    public static Bitmap base64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}