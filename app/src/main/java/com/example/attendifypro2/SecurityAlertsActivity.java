package com.example.attendifypro2;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SecurityAlertsActivity extends AppCompatActivity {

    private TextView alertCountTextView;
    private ListView alertsListView;
    private String lobbyCode;

    private List<ProxyDetectionService.SecurityAlert> alertsList;
    private SecurityAlertAdapter alertAdapter;
    private ProxyDetectionService proxyDetectionService;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_alerts);

        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");

        proxyDetectionService = new ProxyDetectionService(this);
        databaseReference = FirebaseDatabase.getInstance(
                        "https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        alertCountTextView = findViewById(R.id.alertCountTextView);
        alertsListView = findViewById(R.id.alertsListView);

        alertsList = new ArrayList<>();
        alertAdapter = new SecurityAlertAdapter(this, alertsList,
                new SecurityAlertAdapter.OnAlertActionListener() {
                    @Override
                    public void onReview(ProxyDetectionService.SecurityAlert alert) {
                        showAlertDetails(alert);
                    }

                    @Override
                    public void onDismiss(ProxyDetectionService.SecurityAlert alert) {
                        dismissAlert(alert);
                    }
                });

        alertsListView.setAdapter(alertAdapter);

        loadSecurityAlerts();
    }

    private void loadSecurityAlerts() {
        proxyDetectionService.getUnresolvedAlerts(lobbyCode,
                new ProxyDetectionService.AlertsCallback() {
                    @Override
                    public void onAlertsLoaded(List<ProxyDetectionService.SecurityAlert> alerts) {
                        alertsList.clear();

                        // Fetch user names for each alert
                        for (ProxyDetectionService.SecurityAlert alert : alerts) {
                            databaseReference.child("Users").child(alert.userId).child("name")
                                    .get().addOnSuccessListener(snapshot -> {
                                        if (snapshot.exists()) {
                                            alert.userName = snapshot.getValue(String.class);
                                        } else {
                                            alert.userName = "Unknown User";
                                        }

                                        alertsList.add(alert);
                                        alertAdapter.notifyDataSetChanged();

                                        updateAlertCount();
                                    });
                        }

                        if (alerts.isEmpty()) {
                            alertCountTextView.setText("No security alerts");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SecurityAlertsActivity.this,
                                "Error loading alerts: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateAlertCount() {
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;

        for (ProxyDetectionService.SecurityAlert alert : alertsList) {
            if ("CRITICAL".equals(alert.severity)) criticalCount++;
            else if ("HIGH".equals(alert.severity)) highCount++;
            else if ("MEDIUM".equals(alert.severity)) mediumCount++;
        }

        String countText = String.format("Total: %d alerts (%d critical, %d high, %d medium)",
                alertsList.size(), criticalCount, highCount, mediumCount);
        alertCountTextView.setText(countText);
    }

    private void showAlertDetails(ProxyDetectionService.SecurityAlert alert) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String timeStr = alert.timestamp != null ?
                dateFormat.format(new Date(alert.timestamp)) : "Unknown";

        String message = String.format(
                "Student: %s\n\nAlert Type: %s\n\nSeverity: %s\n\nDetails: %s\n\nTime: %s",
                alert.userName, getAlertTypeName(alert.alertType),
                alert.severity, alert.details, timeStr
        );

        new AlertDialog.Builder(this)
                .setTitle("Security Alert Details")
                .setMessage(message)
                .setPositiveButton("Mark as Reviewed", (dialog, which) -> {
                    proxyDetectionService.resolveAlert(alert.alertId, "reviewed");
                    loadSecurityAlerts();
                    Toast.makeText(this, "Alert marked as reviewed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Dismiss", (dialog, which) -> {
                    dismissAlert(alert);
                })
                .setNeutralButton("Close", null)
                .show();
    }

    private void dismissAlert(ProxyDetectionService.SecurityAlert alert) {
        new AlertDialog.Builder(this)
                .setTitle("Dismiss Alert")
                .setMessage("Are you sure you want to dismiss this alert?")
                .setPositiveButton("Yes, Dismiss", (dialog, which) -> {
                    proxyDetectionService.resolveAlert(alert.alertId, "dismissed");
                    loadSecurityAlerts();
                    Toast.makeText(this, "Alert dismissed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getAlertTypeName(String alertType) {
        switch (alertType) {
            case "LOCATION_MISMATCH":
                return "Location Mismatch";
            case "SUSPICIOUS_FINGERPRINT":
                return "Suspicious Fingerprint Attempts";
            case "UNREALISTIC_TIMING":
                return "Unrealistic Timing";
            case "MULTIPLE_CHECKIN":
                return "Multiple Check-in Attempts";
            case "IMPOSSIBLE_CHECKOUT":
                return "Impossible Check-out Timing";
            default:
                return alertType;
        }
    }
}