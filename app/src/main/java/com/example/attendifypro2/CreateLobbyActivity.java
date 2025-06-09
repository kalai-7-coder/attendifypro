package com.example.attendifypro2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.UUID;

public class CreateLobbyActivity extends AppCompatActivity {

    private EditText lobbyNameEditText;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private Button createLobbyButton;
    private DatabaseReference lobbiesRef;
    private FirebaseAuth firebaseAuth;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lobby);

        lobbyNameEditText = findViewById(R.id.lobbyNameEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        createLobbyButton = findViewById(R.id.createLobbyButton);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        lobbiesRef = database.getReference("Lobbies");

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Step 1: Auto-detect location on startup
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    latitudeEditText.setText(String.valueOf(location.getLatitude()));
                    longitudeEditText.setText(String.valueOf(location.getLongitude()));
                }
            });
        }

        createLobbyButton.setOnClickListener(v -> createLobby());
    }
    private void createLobby() {
        String lobbyName = lobbyNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyName)) {
            Toast.makeText(this, "Please enter a lobby name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Get the user's current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Generate lobby code
                            String lobbyCode = UUID.randomUUID().toString().substring(0, 6);
                            String adminId = firebaseAuth.getCurrentUser().getUid();

                            // Create lobby object with location
                            Lobby lobby = new Lobby(lobbyName, lobbyCode, adminId, latitude, longitude);

                            lobbiesRef.child(lobbyCode).setValue(lobby)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(CreateLobbyActivity.this, "Lobby created successfully!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(CreateLobbyActivity.this, "Failed to create lobby. Try again.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(CreateLobbyActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}