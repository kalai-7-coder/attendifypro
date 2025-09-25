package com.example.attendifypro2;

import android.graphics.Bitmap;
import android.graphics.Color;

public class FaceMatcher {
    public static boolean compareFaces(Bitmap face1, Bitmap face2, int confidenceThreshold) {
        int size = 100;
        Bitmap resized1 = Bitmap.createScaledBitmap(face1, size, size, false);
        Bitmap resized2 = Bitmap.createScaledBitmap(face2, size, size, false);

        long totalDiff = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int p1 = resized1.getPixel(x, y);
                int p2 = resized2.getPixel(x, y);

                int rDiff = Math.abs(Color.red(p1) - Color.red(p2));
                int gDiff = Math.abs(Color.green(p1) - Color.green(p2));
                int bDiff = Math.abs(Color.blue(p1) - Color.blue(p2));

                totalDiff += rDiff + gDiff + bDiff;
            }
        }

        long maxDiff = 255L * 3 * size * size;
        int similarity = 100 - (int) ((totalDiff * 100) / maxDiff);
        return similarity >= confidenceThreshold;
    }
}