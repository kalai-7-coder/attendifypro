package com.example.attendifypro2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.google.android.gms.location.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

public class CreateLobbyActivity extends AppCompatActivity {

    private EditText lobbyNameEditText, latitudeEditText, longitudeEditText;
    private EditText fromDateEditText, toDateEditText, studentLimitEditText;
    private Button createLobbyButton;

    private DatabaseReference lobbiesRef, usersRef;
    private FirebaseAuth firebaseAuth;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lobby);

        lobbyNameEditText = findViewById(R.id.lobbyNameEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        fromDateEditText = findViewById(R.id.fromDateEditText);
        toDateEditText = findViewById(R.id.toDateEditText);
        studentLimitEditText = findViewById(R.id.studentLimitEditText);
        createLobbyButton = findViewById(R.id.createLobbyButton);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/");
        lobbiesRef = database.getReference("Lobbies");
        usersRef = database.getReference("Users");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationUpdates();

        // ✅ Restored Calendar functionality
        fromDateEditText.setOnClickListener(v -> showDatePicker(fromDateEditText));
        toDateEditText.setOnClickListener(v -> showDatePicker(toDateEditText));

        createLobbyButton.setOnClickListener(v -> createLobby());
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        latitudeEditText.setText(String.valueOf(location.getLatitude()));
                        longitudeEditText.setText(String.valueOf(location.getLongitude()));
                        System.out.println("Latitude: " + location.getLatitude());
                        System.out.println("Longitude: " + location.getLongitude());
                    } else {
                        System.out.println("Location is NULL");
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void showDatePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    targetEditText.setText(date);
                    System.out.println("Date Selected: " + date);  // ✅ Debugging log
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void createLobby() {
        String lobbyName = lobbyNameEditText.getText().toString().trim();
        String fromDate = fromDateEditText.getText().toString().trim();
        String toDate = toDateEditText.getText().toString().trim();
        String studentLimit = studentLimitEditText.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyName) || TextUtils.isEmpty(fromDate) || TextUtils.isEmpty(toDate) || TextUtils.isEmpty(studentLimit)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIX: Check if latitude/longitude fields are empty BEFORE parsing
        String latStr = latitudeEditText.getText().toString().trim();
        String lngStr = longitudeEditText.getText().toString().trim();

        // Check if fields are empty or still showing placeholder text
        if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lngStr) ||
                "Detecting...".equals(latStr) || "Detecting...".equals(lngStr)) {
            Toast.makeText(this, "⏳ Please wait for location to be detected...", Toast.LENGTH_LONG).show();
            return;
        }

        // ✅ FIX: Use try-catch to handle parsing errors gracefully
        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Invalid location data. Please wait for GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Additional validation: Check if coordinates are valid
        if (latitude == 0.0 || longitude == 0.0) {
            Toast.makeText(this, "Location not detected! Wait or retry.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Show progress to user
        createLobbyButton.setEnabled(false);
        createLobbyButton.setText("Creating Lobby...");

        String lobbyCode = UUID.randomUUID().toString().substring(0, 6);
        String adminId = firebaseAuth.getCurrentUser().getUid();

        HashMap<String, Object> lobbyData = new HashMap<>();
        lobbyData.put("lobbyName", lobbyName);
        lobbyData.put("lobbyCode", lobbyCode);
        lobbyData.put("latitude", latitude);
        lobbyData.put("longitude", longitude);
        lobbyData.put("fromDate", fromDate);
        lobbyData.put("toDate", toDate);
        lobbyData.put("studentLimit", Integer.parseInt(studentLimit));
        lobbyData.put("studentsEnrolled", new HashMap<>());
        lobbyData.put("attendance", new HashMap<>());
        lobbyData.put("studentsAbsent", new HashMap<>());
        lobbyData.put("studentsPresent", new HashMap<>());

        lobbiesRef.child(lobbyCode).setValue(lobbyData).addOnCompleteListener(task -> {
            // ✅ Re-enable button
            createLobbyButton.setEnabled(true);
            createLobbyButton.setText("Create Lobby");

            if (task.isSuccessful()) {
                usersRef.child(adminId).child("createdLobbies").child(lobbyCode).setValue(true);
                Toast.makeText(this, "✅ Lobby created successfully! Code: " + lobbyCode, Toast.LENGTH_LONG).show();
                finish();
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "❌ Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                System.out.println("Firebase Error: " + errorMessage);
            }
        });
    }
}