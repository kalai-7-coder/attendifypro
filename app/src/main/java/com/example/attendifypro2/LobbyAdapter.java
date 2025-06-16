package com.example.attendifypro2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LobbyAdapter extends RecyclerView.Adapter<LobbyAdapter.LobbyViewHolder> {

    private Context context;
    private List<Lobby> lobbyList;
    private String userType;

    public LobbyAdapter(Context context, List<Lobby> lobbyList) {
        this.context = context;
        this.lobbyList = lobbyList;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    @NonNull
    @Override
    public LobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lobby, parent, false);
        return new LobbyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LobbyViewHolder holder, int position) {
        Lobby lobby = lobbyList.get(position);
        holder.lobbyNameTextView.setText(lobby.getLobbyName());
        holder.lobbyCodeTextView.setText("Code: " + lobby.getLobbyCode());

        // Set click listener with dynamic navigation based on user type
        holder.itemView.setOnClickListener(view -> {
            Intent intent;

            if ("admin".equalsIgnoreCase(userType)) {
                intent = new Intent(context, AdminAttendanceActivity.class); // Admin goes here
            } else {
                intent = new Intent(context, AttendanceActivity.class); // Students go here
            }

            intent.putExtra("LOBBY_CODE", lobby.getLobbyCode());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return lobbyList.size();
    }

    public static class LobbyViewHolder extends RecyclerView.ViewHolder {
        TextView lobbyNameTextView;
        TextView lobbyCodeTextView;

        public LobbyViewHolder(@NonNull View itemView) {
            super(itemView);
            lobbyNameTextView = itemView.findViewById(R.id.lobbyNameTextView);
            lobbyCodeTextView = itemView.findViewById(R.id.lobbyCodeTextView);
        }
    }
}