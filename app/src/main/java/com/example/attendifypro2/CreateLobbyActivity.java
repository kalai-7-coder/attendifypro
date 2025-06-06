package com.example.attendifypro2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.UUID;

public class CreateLobbyActivity extends AppCompatActivity {

    private EditText lobbyNameEditText;
    private Button createLobbyButton;
    private DatabaseReference lobbiesRef;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lobby);

        lobbyNameEditText = findViewById(R.id.lobbyNameEditText);
        createLobbyButton = findViewById(R.id.createLobbyButton);

        firebaseAuth = FirebaseAuth.getInstance();
        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/");

        lobbiesRef = database.getReference("Lobbies");

        createLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createLobby();
            }
        });
    }

    private void createLobby() {
        String lobbyName = lobbyNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyName)) {
            Toast.makeText(CreateLobbyActivity.this, "Please enter a lobby name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique code for the lobby
        String lobbyCode = UUID.randomUUID().toString().substring(0, 6); // Generates a 6-character code
        String adminId = firebaseAuth.getCurrentUser().getUid();

        // Create a Lobby object
        Lobby lobby = new Lobby(lobbyName, lobbyCode, adminId);

        // Save the lobby to Firebase
        lobbiesRef.child(lobbyCode).setValue(lobby)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(CreateLobbyActivity.this, "Lobby created successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity and return to AdminDashboardActivity
                    } else {
                        Toast.makeText(CreateLobbyActivity.this, "Failed to create lobby. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
