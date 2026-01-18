package com.example.attendifypro2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AttendanceDetailsActivity extends AppCompatActivity {
    private TextView dateTextView, totalEnrolledTextView, presentTextView, absentTextView;
    private ListView studentListView;
    private Button manageStudentsButton, leaveApprovalButton, securityAlertsButton, generateReportButton;
    private ArrayList<String> studentList;
    private StudentListAdapter studentAdapter;
    private DatabaseReference attendanceRef, enrolledStudentsRef, usersRef, lobbyRef;
    private String lobbyCode, selectedDate, lobbyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_details);

        // Retrieve lobby code and selected date from intent
        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        selectedDate = getIntent().getStringExtra("SELECTED_DATE");

        // Initialize UI elements
        dateTextView = findViewById(R.id.dateTextView);
        totalEnrolledTextView = findViewById(R.id.totalEnrolledTextView);
        presentTextView = findViewById(R.id.presentTextView);
        absentTextView = findViewById(R.id.absentTextView);
        studentListView = findViewById(R.id.studentListView);
        manageStudentsButton = findViewById(R.id.manageStudentsButton);
        leaveApprovalButton = findViewById(R.id.leaveApprovalButton);
        securityAlertsButton = findViewById(R.id.securityAlertsButton);
        generateReportButton = findViewById(R.id.generateReportButton);

        dateTextView.setText("Attendance for: " + selectedDate);

        // Set up Firebase references
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/");
        attendanceRef = firebaseDatabase.getReference("Lobbies").child(lobbyCode).child("attendance").child(selectedDate);
        enrolledStudentsRef = firebaseDatabase.getReference("Lobbies").child(lobbyCode).child("studentsEnrolled");
        usersRef = firebaseDatabase.getReference("Users");
        lobbyRef = firebaseDatabase.getReference("Lobbies").child(lobbyCode);

        // Fetch lobby name
        lobbyRef.child("lobbyName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    lobbyName = snapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                lobbyName = "Unknown Lobby";
            }
        });

        studentList = new ArrayList<>();
        studentAdapter = new StudentListAdapter(this, studentList);
        studentListView.setAdapter(studentAdapter);

        loadAttendanceDetails();

        // Navigate to ManageStudentsActivity
        manageStudentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceDetailsActivity.this, ManageStudentsActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            startActivity(intent);
        });

        // Navigate to LeaveApprovalActivity
        leaveApprovalButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceDetailsActivity.this, LeaveApprovalActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            startActivity(intent);
        });

        // Navigate to SecurityAlertsActivity
        securityAlertsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceDetailsActivity.this, SecurityAlertsActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            startActivity(intent);
        });

        // Navigate to ReportGeneratorActivity
        generateReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceDetailsActivity.this, ReportGeneratorActivity.class);
            intent.putExtra("LOBBY_CODE", lobbyCode);
            intent.putExtra("LOBBY_NAME", lobbyName != null ? lobbyName : "Unknown Lobby");
            startActivity(intent);
        });
    }

    private void loadAttendanceDetails() {
        AtomicInteger presentCount = new AtomicInteger(0);
        AtomicInteger absentCount = new AtomicInteger(0);
        studentList.clear();

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot presentSnapshot = snapshot.child("studentsPresent");
                    DataSnapshot absentSnapshot = snapshot.child("studentsAbsent");

                    if (presentSnapshot.exists()) {
                        for (DataSnapshot student : presentSnapshot.getChildren()) {
                            String studentId = student.getKey();

                            usersRef.child(studentId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                    if (nameSnapshot.exists()) {
                                        String studentName = nameSnapshot.getValue(String.class);
                                        studentList.add("✅ " + studentName);
                                        presentTextView.setText("Present: " + presentCount.incrementAndGet());
                                        studentAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(AttendanceDetailsActivity.this, "Failed to fetch student name.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    if (absentSnapshot.exists()) {
                        for (DataSnapshot student : absentSnapshot.getChildren()) {
                            String studentId = student.getKey();

                            usersRef.child(studentId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                    if (nameSnapshot.exists()) {
                                        String studentName = nameSnapshot.getValue(String.class);
                                        studentList.add("❌ " + studentName);
                                        absentTextView.setText("Absent: " + absentCount.incrementAndGet());
                                        studentAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(AttendanceDetailsActivity.this, "Failed to fetch student name.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                } else {
                    Toast.makeText(AttendanceDetailsActivity.this, "No attendance records found for this date.", Toast.LENGTH_SHORT).show();
                }

                enrolledStudentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long enrolledCount = snapshot.exists() ? snapshot.getChildrenCount() : 0;
                        totalEnrolledTextView.setText("Total Enrolled: " + enrolledCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AttendanceDetailsActivity.this, "Error loading student count.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AttendanceDetailsActivity.this, "Failed to load attendance data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}