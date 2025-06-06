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
    private String userType; // Add userType to pass as an extra

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

        // Set an OnClickListener on each lobby item
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, AttendanceActivity.class);
            intent.putExtra("LOBBY_CODE", lobby.getLobbyCode());
            intent.putExtra("USER_TYPE", userType); // Pass the userType dynamically
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
