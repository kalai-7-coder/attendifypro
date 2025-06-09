package com.example.attendifypro2;

public class Lobby {
    private String lobbyName;
    private String lobbyCode;
    private String adminId;
    private double latitude;
    private double longitude;

    // Empty constructor required for Firebase
    public Lobby() {}

    public Lobby(String lobbyName, String lobbyCode, String adminId, double latitude, double longitude) {
        this.lobbyName = lobbyName;
        this.lobbyCode = lobbyCode;
        this.adminId = adminId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getLobbyName() {
        return lobbyName;
    }

    public void setLobbyName(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public String getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}