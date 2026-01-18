package com.example.attendifypro2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AttendanceActivity extends AppCompatActivity {

    private TextView dateTextView, latitudeTextView, longitudeTextView, statusTextView;
    private Button markCheckInButton, markCheckOutButton, applyLeaveButton;
    private CheckBox locationCheckBox, fingerprintCheckBox, faceCheckBox;
    private RadioGroup authModeGroup;
    private RadioButton dualAuthRadio, faceOnlyRadio, fingerprintOnlyRadio;

    private String lobbyCode;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private Executor executor;

    private boolean isCheckingIn = true;
    private boolean locationVerified = false;
    private boolean fingerprintVerified = false;
    private boolean faceVerified = false;

    private String selectedAuthMode = "dual"; // dual, face, fingerprint

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_student);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Lobbies").child(lobbyCode);

        // Initialize views
        dateTextView = findViewById(R.id.dateTextView);
        statusTextView = findViewById(R.id.statusTextView);
        markCheckInButton = findViewById(R.id.markCheckInButton);
        markCheckOutButton = findViewById(R.id.markCheckOutButton);
        applyLeaveButton = findViewById(R.id.applyLeaveButton);
        locationCheckBox = findViewById(R.id.locationCheckBox);
        fingerprintCheckBox = findViewById(R.id.fingerprintCheckBox);
        faceCheckBox = findViewById(R.id.faceCheckBox);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);

        authModeGroup = findViewById(R.id.authModeGroup);
        dualAuthRadio = findViewById(R.id.dualAuthRadio);
        faceOnlyRadio = findViewById(R.id.faceOnlyRadio);
        fingerprintOnlyRadio = findViewById(R.id.fingerprintOnlyRadio);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executor = ContextCompat.getMainExecutor(this);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateTextView.setText("Date: " + currentDate);

        // Auth mode selection
        authModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.dualAuthRadio) {
                selectedAuthMode = "dual";
                faceCheckBox.setVisibility(android.view.View.VISIBLE);
                fingerprintCheckBox.setVisibility(android.view.View.VISIBLE);
            } else if (checkedId == R.id.faceOnlyRadio) {
                selectedAuthMode = "face";
                faceCheckBox.setVisibility(android.view.View.VISIBLE);
                fingerprintCheckBox.setVisibility(android.view.View.GONE);
            } else if (checkedId == R.id.fingerprintOnlyRadio) {
                selectedAuthMode = "fingerprint";
                faceCheckBox.setVisibility(android.view.View.GONE);
                fingerprintCheckBox.setVisibility(android.view.View.VISIBLE);
            }
            resetCheckboxes();
        });

        checkAttendanceStatus();

        markCheckInButton.setOnClickListener(view -> {
            isCheckingIn = true;
            resetCheckboxes();
            markAttendance();
        });

        markCheckOutButton.setOnClickListener(view -> {
            isCheckingIn = false;
            resetCheckboxes();
            markAttendance();
        });

        applyLeaveButton.setOnClickListener(view -> {
            Intent intent = new Intent(AttendanceActivity.this, LeaveRequestActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            startActivity(intent);
        });
    }

    private void resetCheckboxes() {
        locationCheckBox.setChecked(false);
        fingerprintCheckBox.setChecked(false);
        faceCheckBox.setChecked(false);
        locationVerified = false;
        fingerprintVerified = false;
        faceVerified = false;
    }

    private void checkAttendanceStatus() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DatabaseReference attendanceRef = databaseReference.child("attendance")
                .child(currentDate).child("studentsPresent").child(userId);

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long checkInTime = snapshot.child("checkInTime").getValue(Long.class);
                    Long checkOutTime = snapshot.child("checkOutTime").getValue(Long.class);

                    if (checkInTime != null && checkOutTime == null) {
                        statusTextView.setText("✅ Checked In - Ready for Check Out");
                        markCheckInButton.setEnabled(false);
                        markCheckOutButton.setEnabled(true);
                    } else if (checkInTime != null && checkOutTime != null) {
                        statusTextView.setText("✅ Attendance Complete for Today");
                        markCheckInButton.setEnabled(false);
                        markCheckOutButton.setEnabled(false);

                        long workingMillis = checkOutTime - checkInTime;
                        double workingHours = workingMillis / (1000.0 * 60 * 60);
                        Toast.makeText(AttendanceActivity.this,
                                String.format("Total working hours: %.2f hrs", workingHours),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    statusTextView.setText("Not Checked In Yet");
                    markCheckInButton.setEnabled(true);
                    markCheckOutButton.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AttendanceActivity.this, "Error checking status", Toast.LENGTH_SHORT).show();
            }
        });
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
                    Double adminLatObj = snapshot.child("latitude").getValue(Double.class);
                    Double adminLngObj = snapshot.child("longitude").getValue(Double.class);

                    if (adminLatObj == null || adminLngObj == null) {
                        Toast.makeText(AttendanceActivity.this, "Lobby location not set.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double adminLat = adminLatObj;
                    double adminLng = adminLngObj;
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(adminLat, adminLng, studentLat, studentLng, results);

                    if (results[0] <= 50) {
                        locationCheckBox.setChecked(true);
                        locationVerified = true;
                        proceedToAuthentication();
                    } else {
                        Toast.makeText(AttendanceActivity.this,
                                String.format("Too far from attendance location. Distance: %.2f meters", results[0]),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AttendanceActivity.this, "Error retrieving location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToAuthentication() {
        if (selectedAuthMode.equals("dual")) {
            // Dual auth: Face + Fingerprint
            captureFaceForVerification();
        } else if (selectedAuthMode.equals("face")) {
            // Face only
            captureFaceForVerification();
        } else if (selectedAuthMode.equals("fingerprint")) {
            // Fingerprint only
            authenticateBiometric();
        }
    }

    private void captureFaceForVerification() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bitmap capturedBitmap = (Bitmap) data.getExtras().get("data");
            verifyFace(capturedBitmap);
        }
    }

    private void verifyFace(Bitmap capturedBitmap) {
        String userId = firebaseAuth.getCurrentUser().getUid();

        // Fetch stored face from Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users").child(userId);

        userRef.child("faceImage").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String storedFaceBase64 = snapshot.getValue(String.class);
                Bitmap storedFace = FaceRecognitionHelper.base64ToBitmap(storedFaceBase64);

                if (storedFace != null) {
                    // Compare faces (threshold: 75% similarity)
                    FaceRecognitionHelper.compareFaces(capturedBitmap, storedFace, 75,
                            new FaceRecognitionHelper.FaceMatchCallback() {
                                @Override
                                public void onMatch(boolean isMatch, float similarity) {
                                    if (isMatch) {
                                        faceCheckBox.setChecked(true);
                                        faceVerified = true;
                                        Toast.makeText(AttendanceActivity.this,
                                                String.format("✅ Face verified! (%.1f%% match)", similarity),
                                                Toast.LENGTH_SHORT).show();

                                        // If dual auth, proceed to fingerprint
                                        if (selectedAuthMode.equals("dual")) {
                                            authenticateBiometric();
                                        } else {
                                            // Face only mode - save attendance
                                            saveAttendance();
                                        }
                                    } else {
                                        Toast.makeText(AttendanceActivity.this,
                                                String.format("❌ Face mismatch (%.1f%% match)", similarity),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(AttendanceActivity.this,
                                            "Face verification error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(this, "Error loading stored face", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No face registered. Please use fingerprint only.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void authenticateBiometric() {
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                fingerprintCheckBox.setChecked(true);
                fingerprintVerified = true;
                saveAttendance();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(AttendanceActivity.this, "Fingerprint authentication failed.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Toast.makeText(AttendanceActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify Your Identity")
                .setSubtitle(isCheckingIn ? "Authenticate to Check-In" : "Authenticate to Check-Out")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void saveAttendance() {
        // Verify based on selected auth mode
        boolean verified = false;

        if (selectedAuthMode.equals("dual")) {
            verified = locationVerified && faceVerified && fingerprintVerified;
        } else if (selectedAuthMode.equals("face")) {
            verified = locationVerified && faceVerified;
        } else if (selectedAuthMode.equals("fingerprint")) {
            verified = locationVerified && fingerprintVerified;
        }

        if (!verified) {
            Toast.makeText(this, "Verification incomplete!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DatabaseReference attendanceRef = databaseReference.child("attendance")
                .child(currentDate).child("studentsPresent").child(userId);

        long currentTimeMillis = System.currentTimeMillis();

        if (isCheckingIn) {
            HashMap<String, Object> checkInData = new HashMap<>();
            checkInData.put("checkInTime", currentTimeMillis);
            checkInData.put("present", true);
            checkInData.put("authMode", selectedAuthMode);

            attendanceRef.updateChildren(checkInData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    String time = timeFormat.format(new Date(currentTimeMillis));
                    Toast.makeText(this, "✅ Check-In Successful at " + time, Toast.LENGTH_LONG).show();
                    checkAttendanceStatus();
                } else {
                    Toast.makeText(this, "Failed to mark check-in", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            attendanceRef.child("checkOutTime").setValue(currentTimeMillis)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            attendanceRef.child("checkInTime").get().addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    Long checkInTime = snapshot.getValue(Long.class);
                                    if (checkInTime != null) {
                                        long workingMillis = currentTimeMillis - checkInTime;
                                        double workingHours = workingMillis / (1000.0 * 60 * 60);
                                        attendanceRef.child("workingHours").setValue(workingHours);

                                        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                        String time = timeFormat.format(new Date(currentTimeMillis));
                                        Toast.makeText(this,
                                                String.format("✅ Check-Out Successful at %s\nWorking hours: %.2f hrs", time, workingHours),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                                checkAttendanceStatus();
                            });
                        } else {
                            Toast.makeText(this, "Failed to mark check-out", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}