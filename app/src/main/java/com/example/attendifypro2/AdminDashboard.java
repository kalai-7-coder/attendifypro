package com.example.attendifypro2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private RecyclerView recyclerView;
    private LobbyAdapter lobbyAdapter;
    private List<Lobby> lobbyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        String adminId = firebaseAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users").child(adminId).child("createdLobbies");  // ✅ Updated reference

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyList = new ArrayList<>();
        lobbyAdapter = new LobbyAdapter(this, lobbyList);
        lobbyAdapter.setUserType("admin");
        recyclerView.setAdapter(lobbyAdapter);

        Button buttonCreateLobby = findViewById(R.id.createLobbyButton);
        buttonCreateLobby.setOnClickListener(view -> {
            Intent intent = new Intent(AdminDashboard.this, CreateLobbyActivity.class);
            startActivity(intent);
        });

        loadLobbies();
    }

    private void loadLobbies() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lobbyList.clear();
                for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                    String lobbyCode = lobbySnapshot.getKey();  // ✅ Gets lobbyCode

                    DatabaseReference lobbyRef = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("Lobbies").child(lobbyCode);  // ✅ Retrieve actual lobby details

                    lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot lobbyDataSnapshot) {
                            if (lobbyDataSnapshot.exists()) {
                                String lobbyName = lobbyDataSnapshot.child("lobbyName").getValue(String.class);  // ✅ Gets lobbyName
                                Lobby lobby = new Lobby(lobbyCode, lobbyName);  // ✅ Ensure Lobby class supports both fields
                                lobbyList.add(lobby);
                                lobbyAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(AdminDashboard.this, "Failed to load lobby details.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminDashboard.this, "Failed to load created lobbies.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}