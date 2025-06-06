package com.example.attendifypro2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
//import android.widget.RecyclerView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboard extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private RecyclerView recyclerView;
    private LobbyAdapter lobbyAdapter;
    private List<Lobby> lobbyList;
    private EditText lobbyCodeInput;
    private EditText userNameInput;
    private Button joinLobbyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Lobbies");

        lobbyCodeInput = findViewById(R.id.lobbyCodeInput);
        userNameInput = findViewById(R.id.userNameInput);
        joinLobbyButton = findViewById(R.id.joinLobbyButton);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyList = new ArrayList<>();
        lobbyAdapter = new LobbyAdapter(this, lobbyList);
        lobbyAdapter.setUserType("student");
        recyclerView.setAdapter(lobbyAdapter);

        joinLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                joinLobby();
            }
        });

        loadJoinedLobbies();
    }



    private void loadJoinedLobbies() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Students").child(userId).child("joinedLobbies");

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lobbyList.clear();
                for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                    String lobbyCode = lobbySnapshot.getKey(); // Get the lobby code

                    // Now use the lobby code to fetch the lobby name from the Lobbies node
                    DatabaseReference lobbyRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Lobbies").child(lobbyCode);
                    lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot lobbyDataSnapshot) {
                            if (lobbyDataSnapshot.exists()) {
                                String lobbyName = lobbyDataSnapshot.child("lobbyName").getValue(String.class); // Get the lobby name
                                Lobby lobby = new Lobby();
                                lobby.setLobbyCode(lobbyCode);
                                lobby.setLobbyName(lobbyName); // Set the lobby name
                                lobbyList.add(lobby);
                                lobbyAdapter.notifyDataSetChanged(); // Notify the adapter after adding each lobby
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
        String userName = userNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyCode)) {
            Toast.makeText(this, "Please enter a lobby code.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference lobbyRef = databaseReference.child(lobbyCode);
        lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userId = firebaseAuth.getCurrentUser().getUid();
                    DatabaseReference studentRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Students").child(userId).child("joinedLobbies").child(lobbyCode);

                    // Create a Student object to store the name
                    Student student = new Student(userName);

                    // Save the student's name along with the lobby code
                    studentRef.setValue(student).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(StudentDashboard.this, "Successfully joined the lobby!", Toast.LENGTH_SHORT).show();
                            loadJoinedLobbies(); // Refresh the list of joined lobbies
                        } else {
                            Toast.makeText(StudentDashboard.this, "Failed to join lobby.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(StudentDashboard.this, "Lobby does not exist.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboard.this, "Failed to join lobby.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
