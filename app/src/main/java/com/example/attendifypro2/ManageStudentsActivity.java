package com.example.attendifypro2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ManageStudentsActivity extends AppCompatActivity {
    private LinearLayout studentContainer; // ✅ Use LinearLayout instead of ListView
    private Button deleteStudentButton;
    private HashMap<String, CheckBox> studentCheckBoxes; // ✅ Track selected students
    private DatabaseReference enrolledStudentsRef, usersRef;
    private String lobbyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        studentContainer = findViewById(R.id.studentContainer); // ✅ Updated reference
        deleteStudentButton = findViewById(R.id.deleteStudentButton);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/");
        enrolledStudentsRef = firebaseDatabase.getReference("Lobbies").child(lobbyCode).child("studentsEnrolled");
        usersRef = firebaseDatabase.getReference("Users"); // ✅ Fetch names directly

        studentCheckBoxes = new HashMap<>();

        loadEnrolledStudents();

        deleteStudentButton.setOnClickListener(v -> removeSelectedStudents());
    }

    private void loadEnrolledStudents() {
        enrolledStudentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                studentContainer.removeAllViews(); // ✅ Clear previous entries
                studentCheckBoxes.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                        String studentId = studentSnapshot.getKey();

                        // ✅ Fetch name from Users node
                        usersRef.child(studentId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                if (nameSnapshot.exists()) {
                                    String studentName = nameSnapshot.getValue(String.class);
                                    String displayText = studentName + " (" + studentId + ")";

                                    // ✅ Create dynamic checkbox
                                    CheckBox checkBox = new CheckBox(ManageStudentsActivity.this);
                                    checkBox.setText(displayText);
                                    checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                                        if (isChecked) {
                                            studentCheckBoxes.put(studentId, checkBox);
                                        } else {
                                            studentCheckBoxes.remove(studentId);
                                        }
                                    });

                                    // ✅ Add checkbox to layout
                                    studentContainer.addView(checkBox);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(ManageStudentsActivity.this, "Failed to fetch student name.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ManageStudentsActivity.this, "Error loading students.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeSelectedStudents() {
        if (studentCheckBoxes.isEmpty()) {
            Toast.makeText(this, "No students selected for removal.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String studentId : studentCheckBoxes.keySet()) {
            DatabaseReference lobbyRef = enrolledStudentsRef.child(studentId);
            DatabaseReference userRef = usersRef.child(studentId).child("joinedLobbies").child(lobbyCode);

            lobbyRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userRef.removeValue().addOnCompleteListener(userTask -> {
                        if (userTask.isSuccessful()) {
                            Toast.makeText(ManageStudentsActivity.this, "Selected students removed successfully!", Toast.LENGTH_SHORT).show();
                            loadEnrolledStudents(); // ✅ Refresh list after deletion
                        } else {
                            Toast.makeText(ManageStudentsActivity.this, "Failed to remove student from joined lobbies.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(ManageStudentsActivity.this, "Failed to remove student from lobby.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}