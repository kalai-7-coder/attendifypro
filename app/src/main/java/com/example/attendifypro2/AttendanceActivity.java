package com.example.attendifypro2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {

    private TextView dateTextView;
    private Button markPresentButton;
    private ListView studentListView;

    private String lobbyCode;
    private String userType;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private ArrayList<String> studentList;
    private ArrayAdapter<String> studentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the user type passed from the previous activity
        userType = getIntent().getStringExtra("USER_TYPE");

        // Choose the layout based on user type
        if ("student".equals(userType)) {
            setContentView(R.layout.activity_attendance_student); // For students
        } else if ("admin".equals(userType)) {
            setContentView(R.layout.activity_attendance_admin); // For admin
        }

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        dateTextView = findViewById(R.id.dateTextView);
        markPresentButton = findViewById(R.id.markPresentButton);
        studentListView = findViewById(R.id.studentListView);

        // Display today's date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dateTextView.setText(currentDate);

        // Get data passed from the previous activity
        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");

        // Handle actions based on user type
        if ("student".equals(userType)) {
            markPresentButton.setOnClickListener(view -> markAttendance());
        } else if ("admin".equals(userType)) {
            // Initialize the student list for admin view
            studentList = new ArrayList<>();
            studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
            studentListView.setAdapter(studentAdapter);

            loadAttendanceList();
        }
    }

    private void markAttendance() {
        // Logic for students to mark attendance
        String userId = firebaseAuth.getCurrentUser().getUid();
        String currentDate = dateTextView.getText().toString();

        // Save student's attendance in the database
        databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                .child(userId).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AttendanceActivity.this, "Attendance marked successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AttendanceActivity.this, "Failed to mark attendance.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAttendanceList() {
        // For admin: Load the list of students who marked attendance
        String currentDate = dateTextView.getText().toString();

        // Query the attendance data for the current date
        databaseReference.child("Lobbies").child(lobbyCode).child("attendance").child(currentDate)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        studentList.clear();
                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                            String studentId = studentSnapshot.getKey(); // Get student ID
                            getStudentName(studentId); // Fetch the student's name from the "students" node
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AttendanceActivity.this, "Failed to load attendance list.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getStudentName(String studentId) {
        // Query the "students" node to get the student's name under the joinedlobbies > lobbyCode
        databaseReference.child("Students").child(studentId).child("joinedLobbies").child(lobbyCode).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String studentName = snapshot.getValue(String.class); // Get the student's name
                        if (studentName != null) {
                            studentList.add(studentName); // Add student's name to the list
                            studentAdapter.notifyDataSetChanged(); // Refresh the list view
                        } else {
                            // If no name is found, show a toast
                            Toast.makeText(AttendanceActivity.this, "Student name not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AttendanceActivity.this, "Failed to load student name.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
