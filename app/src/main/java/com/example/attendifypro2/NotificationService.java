package com.example.attendifypro2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class NotificationService {

    private Context context;
    private DatabaseReference databaseReference;
    private NotificationManager notificationManager;

    // Notification channels
    private static final String CHANNEL_ATTENDANCE = "attendance_channel";
    private static final String CHANNEL_ALERTS = "alerts_channel";
    private static final String CHANNEL_LEAVE = "leave_channel";

    public NotificationService(Context context) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance(
                        "https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Attendance channel
            NotificationChannel attendanceChannel = new NotificationChannel(
                    CHANNEL_ATTENDANCE,
                    "Attendance Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            attendanceChannel.setDescription("Notifications for attendance reminders and late warnings");

            // Alerts channel
            NotificationChannel alertsChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Security Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Security and proxy detection alerts");

            // Leave channel
            NotificationChannel leaveChannel = new NotificationChannel(
                    CHANNEL_LEAVE,
                    "Leave Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            leaveChannel.setDescription("Leave request status updates");

            notificationManager.createNotificationChannel(attendanceChannel);
            notificationManager.createNotificationChannel(alertsChannel);
            notificationManager.createNotificationChannel(leaveChannel);
        }
    }

    /**
     * Send attendance reminder notification
     */
    public void sendAttendanceReminder(String userId, String lobbyCode, String lobbyName) {
        String title = "⏰ Attendance Reminder";
        String message = String.format("Don't forget to mark your attendance for %s today!", lobbyName);

        Intent intent = new Intent(context, AttendanceActivity.class);
        intent.putExtra("LOBBY_CODE", lobbyCode);

        showNotification(1, title, message, CHANNEL_ATTENDANCE, intent);

        // Save notification log
        saveNotificationLog(userId, "ATTENDANCE_REMINDER", message);
    }

    /**
     * Send late warning notification
     */
    public void sendLateWarning(String userId, String lobbyCode, String lobbyName, int minutesLate) {
        String title = "⚠️ Late Entry Warning";
        String message = String.format("You checked in %d minutes late for %s", minutesLate, lobbyName);

        Intent intent = new Intent(context, AttendanceActivity.class);
        intent.putExtra("LOBBY_CODE", lobbyCode);

        showNotification(2, title, message, CHANNEL_ATTENDANCE, intent);

        saveNotificationLog(userId, "LATE_WARNING", message);
    }

    /**
     * Send absent alert notification
     */
    public void sendAbsentAlert(String userId, String userName, String lobbyName, int consecutiveDays) {
        String title = "❌ Absent Alert";
        String message = consecutiveDays > 1
                ? String.format("%s has been absent for %d consecutive days from %s",
                userName, consecutiveDays, lobbyName)
                : String.format("%s is absent today from %s", userName, lobbyName);

        Intent intent = new Intent(context, AdminDashboard.class);

        showNotification(3, title, message, CHANNEL_ATTENDANCE, intent);

        saveNotificationLog(userId, "ABSENT_ALERT", message);
    }

    /**
     * Send location mismatch alert
     */
    public void sendLocationMismatchAlert(String userId, String userName, String lobbyName, float distance) {
        String title = "🚨 Location Mismatch Alert";
        String message = String.format("%s attempted attendance from %.2f meters away (max: 50m) for %s",
                userName, distance, lobbyName);

        Intent intent = new Intent(context, SecurityAlertsActivity.class);

        showNotification(4, title, message, CHANNEL_ALERTS, intent);

        saveNotificationLog(userId, "LOCATION_MISMATCH", message);
    }

    /**
     * Send proxy detection alert to admin
     */
    public void sendProxyAlertToAdmin(String lobbyCode, String userId, String alertType, String details) {
        // Fetch user name
        databaseReference.child("Users").child(userId).child("name")
                .get().addOnSuccessListener(snapshot -> {
                    String userName = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown";

                    String title = "🚨 Proxy Detection Alert";
                    String message = String.format("%s - %s: %s", userName, alertType, details);

                    Intent intent = new Intent(context, SecurityAlertsActivity.class);
                    intent.putExtra("LOBBY_CODE", lobbyCode);

                    showNotification(5, title, message, CHANNEL_ALERTS, intent);

                    saveNotificationLog(userId, "PROXY_ALERT", message);
                });
    }

    /**
     * Send leave status update notification
     */
    public void sendLeaveStatusUpdate(String userId, String status, String startDate, String endDate) {
        String emoji = status.equals("approved") ? "✅" : "❌";
        String title = String.format("%s Leave Request %s", emoji,
                status.equals("approved") ? "Approved" : "Rejected");
        String message = String.format("Your leave request for %s to %s has been %s",
                startDate, endDate, status);

        Intent intent = new Intent(context, LeaveRequestActivity.class);

        showNotification(6, title, message, CHANNEL_LEAVE, intent);

        saveNotificationLog(userId, "LEAVE_STATUS", message);
    }

    /**
     * Check and send daily absent alerts (run this daily)
     */
    public void checkAndSendAbsentAlerts(String lobbyCode) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference enrolledRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("studentsEnrolled");
        DatabaseReference attendanceRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("attendance").child(currentDate);

        enrolledRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot enrolledSnapshot) {
                attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                        DataSnapshot presentSnapshot = attendanceSnapshot.child("studentsPresent");
                        DataSnapshot leaveSnapshot = attendanceSnapshot.child("studentsOnLeave");

                        for (DataSnapshot studentSnapshot : enrolledSnapshot.getChildren()) {
                            String studentId = studentSnapshot.getKey();

                            // Check if student is present or on leave
                            boolean isPresent = presentSnapshot.hasChild(studentId);
                            boolean isOnLeave = leaveSnapshot.hasChild(studentId);

                            if (!isPresent && !isOnLeave) {
                                // Student is absent
                                String studentName = studentSnapshot.child("name").getValue(String.class);
                                String lobbyName = "your class"; // Fetch from lobby if needed

                                sendAbsentAlert(studentId, studentName, lobbyName, 1);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Schedule attendance reminder (e.g., 15 minutes before class)
     */
    public void scheduleAttendanceReminder(String userId, String lobbyCode, String lobbyName) {
        // This would typically use WorkManager or AlarmManager
        // For simplicity, sending immediate notification
        sendAttendanceReminder(userId, lobbyCode, lobbyName);
    }

    /**
     * Show local notification
     */
    private void showNotification(int notificationId, String title, String message,
                                  String channelId, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Add this icon to drawable
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Save notification log to Firebase
     */
    private void saveNotificationLog(String userId, String type, String message) {
        String notificationId = databaseReference.child("NotificationLogs").push().getKey();

        HashMap<String, Object> logData = new HashMap<>();
        logData.put("userId", userId);
        logData.put("type", type);
        logData.put("message", message);
        logData.put("timestamp", System.currentTimeMillis());
        logData.put("read", false);

        databaseReference.child("NotificationLogs").child(notificationId).setValue(logData);

        // Also save in user's notifications
        databaseReference.child("Users").child(userId)
                .child("notifications").child(notificationId).setValue(true);
    }

    /**
     * Subscribe user to topic for FCM
     */
    public void subscribeToLobbyNotifications(String lobbyCode) {
        FirebaseMessaging.getInstance().subscribeToTopic("lobby_" + lobbyCode);
    }

    /**
     * Unsubscribe from topic
     */
    public void unsubscribeFromLobbyNotifications(String lobbyCode) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("lobby_" + lobbyCode);
    }
}