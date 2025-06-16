package com.example.attendifypro2;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboard extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private RecyclerView recyclerView;
    private LobbyAdapter lobbyAdapter;
    private List<Lobby> lobbyList;
    private EditText lobbyCodeInput, userNameInput; // ✅ Added field for student name input
    private Button joinLobbyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        lobbyCodeInput = findViewById(R.id.lobbyCodeInput);
        // ✅ New input field for student name
        joinLobbyButton = findViewById(R.id.joinLobbyButton);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        lobbyList = new ArrayList<>();
        lobbyAdapter = new LobbyAdapter(this, lobbyList);
        lobbyAdapter.setUserType("student");
        recyclerView.setAdapter(lobbyAdapter);

        joinLobbyButton.setOnClickListener(view -> joinLobby());
        loadJoinedLobbies();
    }

    private void loadJoinedLobbies() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference userRef = databaseReference.child(userId).child("joinedLobbies");

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lobbyList.clear();
                for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                    String lobbyCode = lobbySnapshot.getKey();

                    DatabaseReference lobbyRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("Lobbies").child(lobbyCode);

                    lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot lobbyDataSnapshot) {
                            if (lobbyDataSnapshot.exists()) {
                                String lobbyName = lobbyDataSnapshot.child("lobbyName").getValue(String.class);
                                Lobby lobby = new Lobby();
                                lobby.setLobbyCode(lobbyCode);
                                lobby.setLobbyName(lobbyName);
                                lobbyList.add(lobby);
                                lobbyAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(StudentDashboard.this, "Failed to load lobby details", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboard.this, "Failed to load joined lobbies", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinLobby() {
        String lobbyCode = lobbyCodeInput.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyCode)) {
            Toast.makeText(this, "Please enter a lobby code.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference userRef = databaseReference.child(userId);  // Reference to Users node

        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String studentName = snapshot.getValue(String.class); // Fetch name from Users node

                    DatabaseReference lobbyRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("Lobbies").child(lobbyCode);
                    DatabaseReference joinedLobbiesRef = userRef.child("joinedLobbies").child(lobbyCode);
                    DatabaseReference enrolledStudentsRef = lobbyRef.child("studentsEnrolled").child(userId);

                    joinedLobbiesRef.setValue(true).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            enrolledStudentsRef.child("name").setValue(studentName).addOnCompleteListener(enrollTask -> {
                                if (enrollTask.isSuccessful()) {
                                    Toast.makeText(StudentDashboard.this, "Successfully joined the lobby!", Toast.LENGTH_SHORT).show();
                                    loadJoinedLobbies();
                                } else {
                                    Toast.makeText(StudentDashboard.this, "Failed to save student name in lobby.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(StudentDashboard.this, "Failed to join lobby.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(StudentDashboard.this, "Name not found in Firebase.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboard.this, "Failed to fetch student name.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}