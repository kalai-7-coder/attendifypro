package com.example.attendifypro2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AttendanceActivity extends AppCompatActivity {

    private TextView dateTextView, latitudeTextView, longitudeTextView;
    private Button markPresentButton;
    private CheckBox locationCheckBox, fingerprintCheckBox;
    private ListView studentListView;
    private ArrayList<String> studentList;
    private ArrayAdapter<String> studentAdapter;

    private String lobbyCode;
    private String userType;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userType = getIntent().getStringExtra("USER_TYPE");

        if ("student".equals(userType)) {
            setContentView(R.layout.activity_attendance_student);
        } else if ("admin".equals(userType)) {
            setContentView(R.layout.activity_attendance_admin);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance(
                "https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference();

        dateTextView = findViewById(R.id.dateTextView);
        markPresentButton = findViewById(R.id.markPresentButton);
        locationCheckBox = findViewById(R.id.locationCheckBox);
        fingerprintCheckBox = findViewById(R.id.fingerprintCheckBox);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        studentListView = findViewById(R.id.studentListView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executor = ContextCompat.getMainExecutor(this);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateTextView.setText(currentDate);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");

        if ("student".equals(userType)) {
            markPresentButton.setOnClickListener(view -> markAttendance());
        } else if ("admin".equals(userType)) {
            studentList = new ArrayList<>();
            studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
            studentListView.setAdapter(studentAdapter);
            loadAttendanceList();
        }
    }

    private void markAttendance() {
        // Step 1: Detect Location First
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                // Set non-editable fields with detected location
                latitudeTextView.setText(String.valueOf(lat));
                longitudeTextView.setText(String.valueOf(lng));

                // Store detected location in Firebase for future reference
                String userId = firebaseAuth.getCurrentUser().getUid();
                String currentDate = dateTextView.getText().toString();

                databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                        .child(userId).child("latitude").setValue(lat);
                databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                        .child(userId).child("longitude").setValue(lng);

                verifyLocation(lat, lng);
            } else {
                Toast.makeText(AttendanceActivity.this, "Unable to detect location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyLocation(double studentLat, double studentLng) {
        databaseReference.child("Lobbies").child(lobbyCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double adminLat = snapshot.child("latitude").getValue(Double.class);
                    double adminLng = snapshot.child("longitude").getValue(Double.class);

                    float[] results = new float[1];
                    Location.distanceBetween(adminLat, adminLng, studentLat, studentLng, results);
                    float distance = results[0];

                    if (distance <= 50) {
                        locationCheckBox.setChecked(true); // Update location checkbox
                        authenticateBiometric(); // Proceed to biometric authentication
                    } else {
                        Toast.makeText(AttendanceActivity.this, "You are too far from the attendance location.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AttendanceActivity.this, "Error retrieving lobby location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateBiometric() {
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        fingerprintCheckBox.setChecked(true); // Update fingerprint checkbox
                        saveAttendance();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(AttendanceActivity.this, "Biometric authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify Your Identity")
                .setSubtitle("Use fingerprint or face recognition")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void saveAttendance() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String currentDate = dateTextView.getText().toString();

        databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                .child(userId).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AttendanceActivity.this, "✅ Attendance marked successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AttendanceActivity.this, "❌ Failed to mark attendance.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAttendanceList() {
        String currentDate = dateTextView.getText().toString();

        databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        studentList.clear();
                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                            String studentId = studentSnapshot.getKey();
                            getStudentName(studentId);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AttendanceActivity.this, "Failed to load attendance list.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getStudentName(String studentId) {
        databaseReference.child("Students").child(studentId).child("joinedLobbies").child(lobbyCode).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String studentName = snapshot.getValue(String.class);
                        if (studentName != null) {
                            studentList.add(studentName);
                            studentAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AttendanceActivity.this, "Failed to load student name.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}