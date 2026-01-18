package com.example.attendifypro2;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class LeaveRequestActivity extends AppCompatActivity {

    private EditText startDateEditText, endDateEditText, reasonEditText;
    private Button submitLeaveButton;
    private ListView leaveHistoryListView;

    private String lobbyCode;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private ArrayList<String> leaveHistoryList;
    private LeaveHistoryAdapter leaveAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_request);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        // Initialize views
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        reasonEditText = findViewById(R.id.reasonEditText);
        submitLeaveButton = findViewById(R.id.submitLeaveButton);
        leaveHistoryListView = findViewById(R.id.leaveHistoryListView);

        leaveHistoryList = new ArrayList<>();
        leaveAdapter = new LeaveHistoryAdapter(this, leaveHistoryList);
        leaveHistoryListView.setAdapter(leaveAdapter);

        // Date pickers
        startDateEditText.setOnClickListener(v -> showDatePicker(startDateEditText));
        endDateEditText.setOnClickListener(v -> showDatePicker(endDateEditText));

        // Submit button
        submitLeaveButton.setOnClickListener(v -> submitLeaveRequest());

        // Load leave history
        loadLeaveHistory();
    }

    private void showDatePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    targetEditText.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void submitLeaveRequest() {
        String startDate = startDateEditText.getText().toString().trim();
        String endDate = endDateEditText.getText().toString().trim();
        String reason = reasonEditText.getText().toString().trim();

        if (startDate.isEmpty() || endDate.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        String leaveId = databaseReference.child("LeaveRequests").push().getKey();

        // Get user name
        databaseReference.child("Users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userName = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown";

                        HashMap<String, Object> leaveData = new HashMap<>();
                        leaveData.put("leaveId", leaveId);
                        leaveData.put("userId", userId);
                        leaveData.put("userName", userName);
                        leaveData.put("lobbyCode", lobbyCode);
                        leaveData.put("startDate", startDate);
                        leaveData.put("endDate", endDate);
                        leaveData.put("reason", reason);
                        leaveData.put("status", "pending"); // pending, approved, rejected
                        leaveData.put("requestTime", System.currentTimeMillis());

                        // Save to Firebase
                        databaseReference.child("LeaveRequests").child(leaveId)
                                .setValue(leaveData)
                                .addOnSuccessListener(aVoid -> {
                                    // Also save reference in lobby
                                    databaseReference.child("Lobbies").child(lobbyCode)
                                            .child("leaveRequests").child(leaveId).setValue(true);

                                    Toast.makeText(LeaveRequestActivity.this,
                                            "Leave request submitted successfully!", Toast.LENGTH_LONG).show();

                                    // Clear fields
                                    startDateEditText.setText("");
                                    endDateEditText.setText("");
                                    reasonEditText.setText("");

                                    loadLeaveHistory();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LeaveRequestActivity.this,
                                            "Failed to submit request", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LeaveRequestActivity.this,
                                "Error fetching user name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLeaveHistory() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        databaseReference.child("LeaveRequests")
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        leaveHistoryList.clear();

                        for (DataSnapshot leaveSnapshot : snapshot.getChildren()) {
                            String startDate = leaveSnapshot.child("startDate").getValue(String.class);
                            String endDate = leaveSnapshot.child("endDate").getValue(String.class);
                            String status = leaveSnapshot.child("status").getValue(String.class);
                            String reason = leaveSnapshot.child("reason").getValue(String.class);

                            String emoji = "⏳"; // pending
                            if ("approved".equals(status)) emoji = "✅";
                            if ("rejected".equals(status)) emoji = "❌";

                            String historyText = String.format("%s %s to %s - %s\n%s",
                                    emoji, startDate, endDate, status.toUpperCase(), reason);

                            leaveHistoryList.add(historyText);
                        }

                        leaveAdapter.notifyDataSetChanged();

                        if (leaveHistoryList.isEmpty()) {
                            leaveHistoryList.add("No leave requests yet");
                            leaveAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LeaveRequestActivity.this,
                                "Failed to load leave history", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}