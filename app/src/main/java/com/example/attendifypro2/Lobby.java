package com.example.attendifypro2;

public class Lobby {
    private String lobbyName;
    private String lobbyCode;
    private double latitude;
    private double longitude;
    private String fromDate;
    private String toDate;
    private int studentLimit;

    // ✅ Required empty constructor for Firebase
    public Lobby() {}

    // ✅ Constructor for basic admin dashboard display
    public Lobby(String lobbyCode, String lobbyName) {
        this.lobbyCode = lobbyCode;
        this.lobbyName = lobbyName;
    }

    // ✅ Full Constructor for complete lobby details
    public Lobby(String lobbyCode, String lobbyName, double latitude, double longitude,
                 String fromDate, String toDate, int studentLimit) {
        this.lobbyCode = lobbyCode;
        this.lobbyName = lobbyName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.studentLimit = studentLimit;
    }

    // Getters and setters
    public String getLobbyName() { return lobbyName; }
    public void setLobbyName(String lobbyName) { this.lobbyName = lobbyName; }

    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }

    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }

    public int getStudentLimit() { return studentLimit; }
    public void setStudentLimit(int studentLimit) { this.studentLimit = studentLimit; }
}