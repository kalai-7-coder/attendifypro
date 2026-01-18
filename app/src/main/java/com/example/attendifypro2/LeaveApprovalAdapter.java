package com.example.attendifypro2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class LeaveApprovalAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<LeaveApprovalActivity.LeaveRequest> leaveList;
    private OnLeaveActionListener listener;

    public interface OnLeaveActionListener {
        void onApprove(LeaveApprovalActivity.LeaveRequest request);
        void onReject(LeaveApprovalActivity.LeaveRequest request);
    }

    public LeaveApprovalAdapter(Context context,
                                ArrayList<LeaveApprovalActivity.LeaveRequest> leaveList,
                                OnLeaveActionListener listener) {
        this.context = context;
        this.leaveList = leaveList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return leaveList.size();
    }

    @Override
    public Object getItem(int position) {
        return leaveList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_leave_request, parent, false);
        }

        LeaveApprovalActivity.LeaveRequest request = leaveList.get(position);

        TextView studentNameText = convertView.findViewById(R.id.studentNameText);
        TextView leaveDatesText = convertView.findViewById(R.id.leaveDatesText);
        TextView leaveReasonText = convertView.findViewById(R.id.leaveReasonText);
        Button approveButton = convertView.findViewById(R.id.approveButton);
        Button rejectButton = convertView.findViewById(R.id.rejectButton);

        studentNameText.setText("Student: " + request.userName);
        leaveDatesText.setText("From: " + request.startDate + " To: " + request.endDate);
        leaveReasonText.setText("Reason: " + request.reason);

        approveButton.setOnClickListener(v -> listener.onApprove(request));
        rejectButton.setOnClickListener(v -> listener.onReject(request));

        return convertView;
    }
}