package com.example.attendifypro2;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SecurityAlertAdapter extends BaseAdapter {
    private Context context;
    private List<ProxyDetectionService.SecurityAlert> alertList;
    private OnAlertActionListener listener;

    public interface OnAlertActionListener {
        void onReview(ProxyDetectionService.SecurityAlert alert);
        void onDismiss(ProxyDetectionService.SecurityAlert alert);
    }

    public SecurityAlertAdapter(Context context,
                                List<ProxyDetectionService.SecurityAlert> alertList,
                                OnAlertActionListener listener) {
        this.context = context;
        this.alertList = alertList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return alertList.size();
    }

    @Override
    public Object getItem(int position) {
        return alertList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.item_security_alert, parent, false);
        }

        ProxyDetectionService.SecurityAlert alert = alertList.get(position);

        TextView alertTypeText = convertView.findViewById(R.id.alertTypeText);
        TextView studentNameText = convertView.findViewById(R.id.studentNameText);
        TextView alertDetailsText = convertView.findViewById(R.id.alertDetailsText);
        TextView alertTimeText = convertView.findViewById(R.id.alertTimeText);
        TextView severityBadge = convertView.findViewById(R.id.severityBadge);
        Button reviewButton = convertView.findViewById(R.id.reviewButton);
        Button dismissButton = convertView.findViewById(R.id.dismissButton);

        // Set alert type
        alertTypeText.setText(getAlertTypeName(alert.alertType));

        // Set student name
        studentNameText.setText("Student: " + (alert.userName != null ? alert.userName : "Unknown"));

        // Set details
        alertDetailsText.setText(alert.details);

        // Set time
        if (alert.timestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            alertTimeText.setText(dateFormat.format(new Date(alert.timestamp)));
        }

        // Set severity badge
        severityBadge.setText(alert.severity);
        switch (alert.severity) {
            case "CRITICAL":
                severityBadge.setBackgroundColor(Color.parseColor("#D32F2F")); // Red
                break;
            case "HIGH":
                severityBadge.setBackgroundColor(Color.parseColor("#F57C00")); // Orange
                break;
            case "MEDIUM":
                severityBadge.setBackgroundColor(Color.parseColor("#FBC02D")); // Yellow
                break;
            default:
                severityBadge.setBackgroundColor(Color.parseColor("#388E3C")); // Green
                break;
        }

        reviewButton.setOnClickListener(v -> listener.onReview(alert));
        dismissButton.setOnClickListener(v -> listener.onDismiss(alert));

        return convertView;
    }

    private String getAlertTypeName(String alertType) {
        switch (alertType) {
            case "LOCATION_MISMATCH":
                return "📍 Location Mismatch";
            case "SUSPICIOUS_FINGERPRINT":
                return "👆 Suspicious Fingerprint";
            case "UNREALISTIC_TIMING":
                return "⏰ Unrealistic Timing";
            case "MULTIPLE_CHECKIN":
                return "🔁 Multiple Check-in";
            case "IMPOSSIBLE_CHECKOUT":
                return "❌ Impossible Checkout";
            default:
                return alertType;
        }
    }
}