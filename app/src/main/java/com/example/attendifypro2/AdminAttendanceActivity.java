package com.example.attendifypro2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;

public class AdminAttendanceActivity extends AppCompatActivity {

    private TextView lobbyCodeTextView;
    private RecyclerView dateRecyclerView;
    private DateAdapter dateAdapter;
    private ArrayList<String> dateList;
    private DatabaseReference databaseReference;
    private String lobbyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_dates);

        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Lobbies");

        lobbyCodeTextView = findViewById(R.id.lobbyCodeTextView);
        dateRecyclerView = findViewById(R.id.dateRecyclerView);
        dateRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        lobbyCodeTextView.setText("Lobby Code: " + lobbyCode);

        dateList = new ArrayList<>();
        dateAdapter = new DateAdapter(dateList, selectedDate -> {
            Intent intent = new Intent(AdminAttendanceActivity.this, AttendanceDetailsActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            intent.putExtra("SELECTED_DATE", selectedDate);
            startActivity(intent);
        });

        dateRecyclerView.setAdapter(dateAdapter);
        loadAttendanceDates();
    }

    private void loadAttendanceDates() {
        databaseReference.child(lobbyCode).child("attendance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        dateList.clear();
                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                            String attendanceDate = dateSnapshot.getKey();
                            if (!TextUtils.isEmpty(attendanceDate)) {
                                dateList.add(attendanceDate);  // ✅ Ensures proper date retrieval
                            }
                        }
                        Collections.sort(dateList, Collections.reverseOrder());
                        dateAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AdminAttendanceActivity.this, "Failed to load attendance dates.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}