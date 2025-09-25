package com.example.attendifypro2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AttendanceActivity extends AppCompatActivity {

    private TextView dateTextView, latitudeTextView, longitudeTextView;
    private Button markPresentButton;
    private CheckBox locationCheckBox, fingerprintCheckBox;
//    private CheckBox facialCheckBox;

    private String lobbyCode;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private Executor executor;
    private Bitmap capturedImage;
    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_student);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Lobbies").child(lobbyCode);

        dateTextView = findViewById(R.id.dateTextView);
        markPresentButton = findViewById(R.id.markPresentButton);
        locationCheckBox = findViewById(R.id.locationCheckBox);
        fingerprintCheckBox = findViewById(R.id.fingerprintCheckBox);
//        facialCheckBox = findViewById(R.id.facialCheckBox);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executor = ContextCompat.getMainExecutor(this);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateTextView.setText(currentDate);

        markPresentButton.setOnClickListener(view -> markAttendance());
    }

    private void markAttendance() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                latitudeTextView.setText(String.valueOf(lat));
                longitudeTextView.setText(String.valueOf(lng));
                verifyLocation(lat, lng);
            } else {
                Toast.makeText(this, "Unable to detect location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyLocation(double studentLat, double studentLng) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double adminLat = snapshot.child("latitude").getValue(Double.class);
                    double adminLng = snapshot.child("longitude").getValue(Double.class);
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(adminLat, adminLng, studentLat, studentLng, results);

                    if (results[0] <= 50) {
                        locationCheckBox.setChecked(true);
                        authenticateBiometric();
                    } else {
                        Toast.makeText(AttendanceActivity.this, "Too far from attendance location.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AttendanceActivity.this, "Error retrieving location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateBiometric() {
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                fingerprintCheckBox.setChecked(true);
//                captureImage();
                //facialCheckBox.setChecked(true);
                saveAttendance();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(AttendanceActivity.this, "Fingerprint failed.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify Your Identity")
                .setSubtitle("Use fingerprint")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

//    private void captureImage() {
//        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
//            capturedImage = (Bitmap) data.getExtras().get("data");
//            authenticateFace(capturedImage);
//        }
//    }

//    private void authenticateFace(Bitmap capturedBitmap) {
//        InputImage image = InputImage.fromBitmap(capturedBitmap, 0);
//        FaceDetector detector = FaceDetection.getClient(
//                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                        .build()
//        );
//
//        detector.process(image)
//                .addOnSuccessListener(faces -> {
//                    if (!faces.isEmpty()) {
//                        retrieveStoredFaceImage(); // continue to fetch & compare
//                    } else {
//                        Toast.makeText(this, "No face detected. Try again.", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> Toast.makeText(this, "Face detection failed.", Toast.LENGTH_SHORT).show());
//    }

//    private void retrieveStoredFaceImage() {
//        String userId = firebaseAuth.getCurrentUser().getUid();
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("faceImage");
//
//        userRef.get()
//                .addOnSuccessListener(snapshot -> {
//                    if (snapshot.exists()) {
//                        decodeBase64Image(snapshot.getValue(String.class));
//                    } else {
//                        Toast.makeText(this, "No stored face image.", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> Toast.makeText(this, "Image fetch error.", Toast.LENGTH_SHORT).show());
//    }

//    private void decodeBase64Image(String base64Image) {
//        try {
//            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
//            Bitmap storedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
//            compareFaces(storedBitmap);
//        } catch (Exception e) {
//            Toast.makeText(this, "Error decoding image.", Toast.LENGTH_SHORT).show();
//        }
//    }

//    private void compareFaces(Bitmap storedBitmap) {
//        private void compareFaces(Bitmap storedBitmap){
//        boolean isMatch = FaceMatcher.compareFaces(capturedImage, storedBitmap, 85);
//        if (isMatch) {
//            facialCheckBox.setChecked(true);
//            saveAttendance();
//        } else {
//            Toast.makeText(this, "Face mismatch.", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void saveAttendance() {
        if (!locationCheckBox.isChecked() || !fingerprintCheckBox.isChecked()) {
            //if (!locationCheckBox.isChecked() || !fingerprintCheckBox.isChecked() || !facialCheckBox.isChecked()) {
            Toast.makeText(this, "Verification failed!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        String currentDate = dateTextView.getText().toString();
        DatabaseReference attendanceRef = databaseReference.child("attendance").child(currentDate).child("studentsPresent").child(userId);

        attendanceRef.setValue(true);
        Toast.makeText(this, "✅ Attendance marked!", Toast.LENGTH_LONG).show();
    }
}