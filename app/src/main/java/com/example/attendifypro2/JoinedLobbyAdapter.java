package com.example.attendifypro2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class JoinedLobbyAdapter extends RecyclerView.Adapter<JoinedLobbyAdapter.ViewHolder> {

    private Context context;
    private List<Lobby> lobbyList;

    public JoinedLobbyAdapter(Context context, List<Lobby> lobbyList) {
        this.context = context;
        this.lobbyList = lobbyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_joined_lobby, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lobby lobby = lobbyList.get(position);
        // Set the lobby code and the user name
        holder.lobbyCodeTextView.setText("Lobby Code: " + lobby.getLobbyCode());
        holder.userNameTextView.setText("User Name: " + lobby.getLobbyName()); // User's name stored as lobby name
    }

    @Override
    public int getItemCount() {
        return lobbyList.size(); // Return the size of the lobby list
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView lobbyCodeTextView;
        TextView userNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            lobbyCodeTextView = itemView.findViewById(R.id.lobby_code_text);
            userNameTextView = itemView.findViewById(R.id.user_name_text);
        }
    }
}
