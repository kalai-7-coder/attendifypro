package com.example.attendifypro2;

public class AttendanceReport {
    private String studentName;
    private String month;
    private int totalDays;
    private int presentDays;
    private double attendancePercentage;
    private int lateDays;
    private double avgWorkingHours;

    public AttendanceReport() {
        // Default constructor required for Firebase
    }

    public AttendanceReport(String studentName, String month, int totalDays,
                            int presentDays, double attendancePercentage,
                            int lateDays, double avgWorkingHours) {
        this.studentName = studentName;
        this.month = month;
        this.totalDays = totalDays;
        this.presentDays = presentDays;
        this.attendancePercentage = attendancePercentage;
        this.lateDays = lateDays;
        this.avgWorkingHours = avgWorkingHours;
    }

    // Getters and setters
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getLateDays() {
        return lateDays;
    }

    public void setLateDays(int lateDays) {
        this.lateDays = lateDays;
    }

    public double getAvgWorkingHours() {
        return avgWorkingHours;
    }

    public void setAvgWorkingHours(double avgWorkingHours) {
        this.avgWorkingHours = avgWorkingHours;
    }
}