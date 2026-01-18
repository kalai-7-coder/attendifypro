package com.example.attendifypro2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class LeaveHistoryAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> leaveList;

    public LeaveHistoryAdapter(Context context, ArrayList<String> leaveList) {
        this.context = context;
        this.leaveList = leaveList;
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
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(leaveList.get(position));
        textView.setTextColor(context.getResources().getColor(R.color.white));
        textView.setPadding(16, 16, 16, 16);

        return convertView;
    }
}