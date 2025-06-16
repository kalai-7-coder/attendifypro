package com.example.attendifypro2;

import android.graphics.Bitmap;
import android.graphics.Color;

public class FaceMatcher {
    public static boolean compareFaces(Bitmap face1, Bitmap face2, int confidenceThreshold) {
        int width1 = face1.getWidth();
        int height1 = face1.getHeight();
        int width2 = face2.getWidth();
        int height2 = face2.getHeight();

        if (width1 != width2 || height1 != height2) {
            return false; // Faces must have the same resolution
        }

        int matchedPixels = 0;
        int totalPixels = width1 * height1;

        for (int x = 0; x < width1; x++) {
            for (int y = 0; y < height1; y++) {
                int pixel1 = face1.getPixel(x, y);
                int pixel2 = face2.getPixel(x, y);

                if (Color.red(pixel1) == Color.red(pixel2) &&
                        Color.green(pixel1) == Color.green(pixel2) &&
                        Color.blue(pixel1) == Color.blue(pixel2)) {
                    matchedPixels++;
                }
            }
        }

        int matchPercentage = (matchedPixels * 100) / totalPixels;
        return matchPercentage >= confidenceThreshold;
    }
}