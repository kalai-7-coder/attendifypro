package com.example.attendifypro2;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.Intent;

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
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Lobbies");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lobbyList = new ArrayList<>();
        lobbyAdapter = new LobbyAdapter(this,lobbyList);
        lobbyAdapter.setUserType("admin");
        recyclerView.setAdapter(lobbyAdapter);

        loadLobbies();
    }

    private void loadLobbies() {
        String adminId = firebaseAuth.getCurrentUser().getUid();
        Button buttonCreateLobby = findViewById(R.id.createLobbyButton); // Ensure you have this button in your XML layout
        buttonCreateLobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminDashboard.this, CreateLobbyActivity.class);
                startActivity(intent);
            }
        });

        databaseReference.orderByChild("adminId").equalTo(adminId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        lobbyList.clear();
                        for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                            Lobby lobby = lobbySnapshot.getValue(Lobby.class);
                            lobbyList.add(lobby);
                        }
                        lobbyAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminDashboard.this, "Failed to load lobbies", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
