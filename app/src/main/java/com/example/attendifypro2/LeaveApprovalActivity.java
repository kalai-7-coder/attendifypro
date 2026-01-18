package com.example.attendifypro2;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LeaveApprovalActivity extends AppCompatActivity {

    private ListView leaveRequestsListView;
    private String lobbyCode;
    private DatabaseReference databaseReference;
    private NotificationService notificationService;

    private ArrayList<LeaveRequest> leaveRequestsList;
    private LeaveApprovalAdapter leaveAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_approval);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");

        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        notificationService = new NotificationService(this);

        leaveRequestsListView = findViewById(R.id.leaveRequestsListView);

        leaveRequestsList = new ArrayList<>();
        leaveAdapter = new LeaveApprovalAdapter(this, leaveRequestsList,
                new LeaveApprovalAdapter.OnLeaveActionListener() {
                    @Override
                    public void onApprove(LeaveRequest request) {
                        approveLeave(request);
                    }

                    @Override
                    public void onReject(LeaveRequest request) {
                        rejectLeave(request);
                    }
                });

        leaveRequestsListView.setAdapter(leaveAdapter);

        loadPendingLeaveRequests();
    }

    private void loadPendingLeaveRequests() {
        databaseReference.child("Lobbies").child(lobbyCode).child("leaveRequests")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot lobbySnapshot) {
                        leaveRequestsList.clear();

                        for (DataSnapshot leaveRefSnapshot : lobbySnapshot.getChildren()) {
                            String leaveId = leaveRefSnapshot.getKey();

                            // Fetch full leave details
                            databaseReference.child("LeaveRequests").child(leaveId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                String status = snapshot.child("status").getValue(String.class);

                                                // Only show pending requests
                                                if ("pending".equals(status)) {
                                                    LeaveRequest request = new LeaveRequest();
                                                    request.leaveId = snapshot.child("leaveId").getValue(String.class);
                                                    request.userId = snapshot.child("userId").getValue(String.class);
                                                    request.userName = snapshot.child("userName").getValue(String.class);
                                                    request.startDate = snapshot.child("startDate").getValue(String.class);
                                                    request.endDate = snapshot.child("endDate").getValue(String.class);
                                                    request.reason = snapshot.child("reason").getValue(String.class);
                                                    request.status = status;

                                                    leaveRequestsList.add(request);
                                                    leaveAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(LeaveApprovalActivity.this,
                                                    "Error loading leave details", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LeaveApprovalActivity.this,
                                "Failed to load leave requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void approveLeave(LeaveRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Leave")
                .setMessage("Approve leave for " + request.userName + "?")
                .setPositiveButton("Approve", (dialog, which) -> {
                    // Update status to approved
                    databaseReference.child("LeaveRequests").child(request.leaveId)
                            .child("status").setValue("approved")
                            .addOnSuccessListener(aVoid -> {
                                // Mark attendance as "on leave" for date range
                                markAttendanceForLeave(request);

                                // Send notification to student
                                notificationService.sendLeaveStatusUpdate(
                                        request.userId, "approved",
                                        request.startDate, request.endDate);

                                Toast.makeText(this, "Leave approved!", Toast.LENGTH_SHORT).show();
                                loadPendingLeaveRequests(); // Refresh list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to approve", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rejectLeave(LeaveRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Leave")
                .setMessage("Reject leave for " + request.userName + "?")
                .setPositiveButton("Reject", (dialog, which) -> {
                    databaseReference.child("LeaveRequests").child(request.leaveId)
                            .child("status").setValue("rejected")
                            .addOnSuccessListener(aVoid -> {
                                // Send notification to student
                                notificationService.sendLeaveStatusUpdate(
                                        request.userId, "rejected",
                                        request.startDate, request.endDate);

                                Toast.makeText(this, "Leave rejected", Toast.LENGTH_SHORT).show();
                                loadPendingLeaveRequests(); // Refresh list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to reject", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markAttendanceForLeave(LeaveRequest request) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(request.startDate);
            Date endDate = sdf.parse(request.endDate);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            // Mark each day as "on leave"
            while (!calendar.getTime().after(endDate)) {
                String dateStr = sdf.format(calendar.getTime());

                HashMap<String, Object> leaveData = new HashMap<>();
                leaveData.put("onLeave", true);
                leaveData.put("leaveId", request.leaveId);
                leaveData.put("reason", request.reason);

                databaseReference.child("Lobbies").child(lobbyCode)
                        .child("attendance").child(dateStr)
                        .child("studentsOnLeave").child(request.userId)
                        .setValue(leaveData);

                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error marking attendance for leave", Toast.LENGTH_SHORT).show();
        }
    }

    // Inner class for leave request data
    public static class LeaveRequest {
        public String leaveId;
        public String userId;
        public String userName;
        public String startDate;
        public String endDate;
        public String reason;
        public String status;
    }
}