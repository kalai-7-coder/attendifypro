package com.example.attendifypro2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ProxyDetectionService {

    private static final String TAG = "ProxyDetection";
    private DatabaseReference databaseReference;
    private Context context;

    // Thresholds
    private static final float MAX_DISTANCE_METERS = 50f; // Max allowed distance from lobby
    private static final int MAX_FINGERPRINT_ATTEMPTS = 5; // Max attempts in 5 minutes
    private static final long ATTEMPT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
    private static final int UNREALISTIC_HOUR_START = 0; // 12 AM
    private static final int UNREALISTIC_HOUR_END = 5; // 5 AM

    public ProxyDetectionService(Context context) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance(
                        "https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();
    }

    /**
     * Check for location mismatch
     */
    public void checkLocationMismatch(String lobbyCode, String userId, double studentLat,
                                      double studentLng, LocationCheckCallback callback) {

        DatabaseReference lobbyRef = databaseReference.child("Lobbies").child(lobbyCode);

        lobbyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double adminLat = snapshot.child("latitude").getValue(Double.class);
                    Double adminLng = snapshot.child("longitude").getValue(Double.class);

                    if (adminLat != null && adminLng != null) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(
                                adminLat, adminLng, studentLat, studentLng, results
                        );

                        float distance = results[0];

                        if (distance > MAX_DISTANCE_METERS) {
                            // ALERT: Location mismatch detected
                            raiseAlert(lobbyCode, userId, "LOCATION_MISMATCH",
                                    String.format("Distance: %.2f meters (allowed: %.2f)",
                                            distance, MAX_DISTANCE_METERS));
                            callback.onMismatchDetected(distance);
                        } else {
                            callback.onLocationValid(distance);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Location check error: " + error.getMessage());
            }
        });
    }

    /**
     * Track and detect suspicious fingerprint attempts
     */
    public void trackFingerprintAttempt(String lobbyCode, String userId, boolean success) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long currentTime = System.currentTimeMillis();

        DatabaseReference attemptsRef = databaseReference.child("SecurityLogs")
                .child(lobbyCode).child(currentDate).child(userId).child("fingerprintAttempts");

        // Log this attempt
        String attemptId = attemptsRef.push().getKey();
        HashMap<String, Object> attemptData = new HashMap<>();
        attemptData.put("timestamp", currentTime);
        attemptData.put("success", success);

        attemptsRef.child(attemptId).setValue(attemptData);

        // Check for suspicious pattern
        checkSuspiciousFingerprintPattern(lobbyCode, userId, currentDate, currentTime);
    }

    private void checkSuspiciousFingerprintPattern(String lobbyCode, String userId,
                                                   String date, long currentTime) {
        DatabaseReference attemptsRef = databaseReference.child("SecurityLogs")
                .child(lobbyCode).child(date).child(userId).child("fingerprintAttempts");

        attemptsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int recentAttempts = 0;
                long windowStart = currentTime - ATTEMPT_WINDOW_MS;

                for (DataSnapshot attemptSnapshot : snapshot.getChildren()) {
                    Long timestamp = attemptSnapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp >= windowStart) {
                        recentAttempts++;
                    }
                }

                if (recentAttempts >= MAX_FINGERPRINT_ATTEMPTS) {
                    // ALERT: Too many fingerprint attempts
                    raiseAlert(lobbyCode, userId, "SUSPICIOUS_FINGERPRINT",
                            String.format("%d attempts in 5 minutes", recentAttempts));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fingerprint check error: " + error.getMessage());
            }
        });
    }

    /**
     * Check for unrealistic timing patterns
     */
    public void checkUnrealisticTiming(String lobbyCode, String userId,
                                       UnrealisticTimingCallback callback) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Check 1: Unrealistic hours (midnight to 5 AM)
        if (hour >= UNREALISTIC_HOUR_START && hour < UNREALISTIC_HOUR_END) {
            raiseAlert(lobbyCode, userId, "UNREALISTIC_TIMING",
                    String.format("Attendance marked at %02d:%02d", hour, minute));
            callback.onUnrealisticTime(hour, minute, "Night time attendance (suspicious)");
            return;
        }

        // Check 2: Multiple check-ins on same day
        checkMultipleCheckIns(lobbyCode, userId, callback);

        // Check 3: Impossible check-out timing (before check-in)
        checkImpossibleCheckout(lobbyCode, userId, callback);
    }

    private void checkMultipleCheckIns(String lobbyCode, String userId,
                                       UnrealisticTimingCallback callback) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference attendanceRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("attendance").child(currentDate)
                .child("studentsPresent").child(userId);

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long existingCheckIn = snapshot.child("checkInTime").getValue(Long.class);
                    if (existingCheckIn != null) {
                        // Already checked in today - possible proxy attempt
                        raiseAlert(lobbyCode, userId, "MULTIPLE_CHECKIN",
                                "Attempting to check-in multiple times on same day");
                        callback.onUnrealisticTime(0, 0, "Multiple check-in attempts detected");
                    } else {
                        callback.onTimingValid();
                    }
                } else {
                    callback.onTimingValid();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Multiple check-in error: " + error.getMessage());
            }
        });
    }

    private void checkImpossibleCheckout(String lobbyCode, String userId,
                                         UnrealisticTimingCallback callback) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long currentTime = System.currentTimeMillis();

        DatabaseReference attendanceRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("attendance").child(currentDate)
                .child("studentsPresent").child(userId);

        attendanceRef.child("checkInTime").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Long checkInTime = snapshot.getValue(Long.class);
                if (checkInTime != null && currentTime < checkInTime) {
                    // Checkout time before check-in (impossible!)
                    raiseAlert(lobbyCode, userId, "IMPOSSIBLE_CHECKOUT",
                            "Check-out time is before check-in time");
                    callback.onUnrealisticTime(0, 0, "Impossible timing detected");
                }
            }
        });
    }

    /**
     * Raise security alert
     */
    private void raiseAlert(String lobbyCode, String userId, String alertType, String details) {
        String alertId = databaseReference.child("SecurityAlerts").push().getKey();
        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> alertData = new HashMap<>();
        alertData.put("alertId", alertId);
        alertData.put("lobbyCode", lobbyCode);
        alertData.put("userId", userId);
        alertData.put("alertType", alertType);
        alertData.put("details", details);
        alertData.put("timestamp", timestamp);
        alertData.put("status", "unresolved"); // unresolved, reviewed, dismissed
        alertData.put("severity", getSeverity(alertType));

        // Save alert
        databaseReference.child("SecurityAlerts").child(alertId).setValue(alertData);

        // Also save reference in lobby
        databaseReference.child("Lobbies").child(lobbyCode)
                .child("securityAlerts").child(alertId).setValue(true);

        // Send notification to admin
        NotificationService notificationService = new NotificationService(context);
        notificationService.sendProxyAlertToAdmin(lobbyCode, userId, alertType, details);

        Log.w(TAG, String.format("ALERT RAISED: %s for user %s - %s",
                alertType, userId, details));
    }

    private String getSeverity(String alertType) {
        switch (alertType) {
            case "LOCATION_MISMATCH":
                return "HIGH";
            case "SUSPICIOUS_FINGERPRINT":
                return "MEDIUM";
            case "UNREALISTIC_TIMING":
                return "HIGH";
            case "MULTIPLE_CHECKIN":
                return "CRITICAL";
            case "IMPOSSIBLE_CHECKOUT":
                return "CRITICAL";
            default:
                return "LOW";
        }
    }

    /**
     * Get all unresolved alerts for a lobby
     */
    public void getUnresolvedAlerts(String lobbyCode, AlertsCallback callback) {
        DatabaseReference lobbyAlertsRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("securityAlerts");

        lobbyAlertsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SecurityAlert> alerts = new ArrayList<>();

                for (DataSnapshot alertRefSnapshot : snapshot.getChildren()) {
                    String alertId = alertRefSnapshot.getKey();

                    // Fetch full alert details
                    databaseReference.child("SecurityAlerts").child(alertId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot alertSnapshot) {
                                    if (alertSnapshot.exists()) {
                                        String status = alertSnapshot.child("status").getValue(String.class);

                                        if ("unresolved".equals(status)) {
                                            SecurityAlert alert = new SecurityAlert();
                                            alert.alertId = alertSnapshot.child("alertId").getValue(String.class);
                                            alert.userId = alertSnapshot.child("userId").getValue(String.class);
                                            alert.alertType = alertSnapshot.child("alertType").getValue(String.class);
                                            alert.details = alertSnapshot.child("details").getValue(String.class);
                                            alert.timestamp = alertSnapshot.child("timestamp").getValue(Long.class);
                                            alert.severity = alertSnapshot.child("severity").getValue(String.class);

                                            alerts.add(alert);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Alert fetch error: " + error.getMessage());
                                }
                            });
                }

                callback.onAlertsLoaded(alerts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Mark alert as reviewed/dismissed
     */
    public void resolveAlert(String alertId, String action) {
        databaseReference.child("SecurityAlerts").child(alertId)
                .child("status").setValue("reviewed");
        databaseReference.child("SecurityAlerts").child(alertId)
                .child("action").setValue(action);
    }

    // Callback interfaces
    public interface LocationCheckCallback {
        void onLocationValid(float distance);
        void onMismatchDetected(float distance);
    }

    public interface UnrealisticTimingCallback {
        void onTimingValid();
        void onUnrealisticTime(int hour, int minute, String reason);
    }

    public interface AlertsCallback {
        void onAlertsLoaded(List<SecurityAlert> alerts);
        void onError(String error);
    }

    // Data class for security alerts
    public static class SecurityAlert {
        public String alertId;
        public String userId;
        public String userName;
        public String alertType;
        public String details;
        public Long timestamp;
        public String severity;
        public String status;
    }
}