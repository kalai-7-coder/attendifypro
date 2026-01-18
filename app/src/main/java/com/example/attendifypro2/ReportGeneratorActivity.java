package com.example.attendifypro2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportGeneratorActivity extends AppCompatActivity {

    private TextView lobbyNameTextView, selectedMonthTextView;
    private RadioGroup reportTypeGroup;
    private RadioButton individualReportRadio, allStudentsReportRadio;
    private Spinner studentSpinner;
    private Button selectMonthButton, generateReportButton;

    private String lobbyCode, lobbyName, selectedMonth;
    private DatabaseReference databaseReference;
    private HashMap<String, String> studentMap; // studentId -> studentName
    private List<String> studentNames;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_generator);

        // Get lobby details from intent
        lobbyCode = getIntent().getStringExtra("LOBBY_CODE");
        lobbyName = getIntent().getStringExtra("LOBBY_NAME");

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance("https://attendifypro-25edb-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        // Initialize views
        lobbyNameTextView = findViewById(R.id.lobbyNameTextView);
        selectedMonthTextView = findViewById(R.id.selectedMonthTextView);
        reportTypeGroup = findViewById(R.id.reportTypeGroup);
        individualReportRadio = findViewById(R.id.individualReportRadio);
        allStudentsReportRadio = findViewById(R.id.allStudentsReportRadio);
        studentSpinner = findViewById(R.id.studentSpinner);
        selectMonthButton = findViewById(R.id.selectMonthButton);
        generateReportButton = findViewById(R.id.generateReportButton);

        lobbyNameTextView.setText("Lobby: " + lobbyName);

        studentMap = new HashMap<>();
        studentNames = new ArrayList<>();

        // Request storage permission
        checkPermission();

        // Load enrolled students
        loadEnrolledStudents();

        // Month selection
        selectMonthButton.setOnClickListener(v -> showMonthYearPicker());

        // Toggle student spinner visibility
        reportTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.individualReportRadio) {
                studentSpinner.setVisibility(android.view.View.VISIBLE);
            } else {
                studentSpinner.setVisibility(android.view.View.GONE);
            }
        });

        // Generate report
        generateReportButton.setOnClickListener(v -> generateReport());
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void loadEnrolledStudents() {
        DatabaseReference enrolledRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("studentsEnrolled");

        enrolledRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentMap.clear();
                studentNames.clear();

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentId = studentSnapshot.getKey();
                    String studentName = studentSnapshot.child("name").getValue(String.class);

                    if (studentName != null) {
                        studentMap.put(studentId, studentName);
                        studentNames.add(studentName);
                    }
                }

                // Populate spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ReportGeneratorActivity.this,
                        android.R.layout.simple_spinner_item,
                        studentNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                studentSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportGeneratorActivity.this,
                        "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, monthOfYear, dayOfMonth) -> {
                    ReportGeneratorActivity.this.selectedMonth = String.format("%04d-%02d", selectedYear, monthOfYear + 1);
                    selectedMonthTextView.setText("Selected: " + ReportGeneratorActivity.this.selectedMonth);
                },
                year, month, 1
        );
        datePickerDialog.show();
    }

    private void generateReport() {
        if (selectedMonth == null || selectedMonth.isEmpty()) {
            Toast.makeText(this, "Please select a month", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedReportType = reportTypeGroup.getCheckedRadioButtonId();

        if (selectedReportType == R.id.individualReportRadio) {
            generateIndividualReport();
        } else if (selectedReportType == R.id.allStudentsReportRadio) {
            generateAllStudentsReport();
        } else {
            Toast.makeText(this, "Please select a report type", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateIndividualReport() {
        String selectedStudentName = (String) studentSpinner.getSelectedItem();
        if (selectedStudentName == null) {
            Toast.makeText(this, "No student selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find student ID
        String studentId = null;
        for (Map.Entry<String, String> entry : studentMap.entrySet()) {
            if (entry.getValue().equals(selectedStudentName)) {
                studentId = entry.getKey();
                break;
            }
        }

        if (studentId == null) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalStudentId = studentId;
        fetchAttendanceData(finalStudentId, selectedStudentName, false);
    }

    private void generateAllStudentsReport() {
        fetchAttendanceData(null, null, true);
    }

    private void fetchAttendanceData(String studentId, String studentName, boolean isAllStudents) {
        DatabaseReference attendanceRef = databaseReference.child("Lobbies")
                .child(lobbyCode).child("attendance");

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAllStudents) {
                    processAllStudentsData(snapshot);
                } else {
                    processIndividualStudentData(snapshot, studentId, studentName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportGeneratorActivity.this,
                        "Failed to fetch attendance data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processIndividualStudentData(DataSnapshot snapshot, String studentId, String studentName) {
        int totalDays = 0;
        int presentDays = 0;
        int lateDays = 0;
        long totalWorkingMinutes = 0;

        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
            String date = dateSnapshot.getKey();

            // Check if date belongs to selected month
            if (date != null && date.startsWith(selectedMonth)) {
                totalDays++;

                // Check if student was present
                DataSnapshot presentSnapshot = dateSnapshot.child("studentsPresent").child(studentId);
                if (presentSnapshot.exists()) {
                    presentDays++;

                    // Check for late entry
                    Long checkInTime = presentSnapshot.child("checkInTime").getValue(Long.class);
                    Long checkOutTime = presentSnapshot.child("checkOutTime").getValue(Long.class);

                    if (checkInTime != null) {
                        // Assuming standard start time is 9:00 AM
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(checkInTime);
                        int hour = cal.get(Calendar.HOUR_OF_DAY);
                        int minute = cal.get(Calendar.MINUTE);

                        if (hour > 9 || (hour == 9 && minute > 0)) {
                            lateDays++;
                        }
                    }

                    // Calculate working hours
                    if (checkInTime != null && checkOutTime != null) {
                        long workingMillis = checkOutTime - checkInTime;
                        totalWorkingMinutes += workingMillis / (1000 * 60);
                    }
                }
            }
        }

        double attendancePercentage = totalDays > 0 ? (presentDays * 100.0 / totalDays) : 0;
        double avgWorkingHours = presentDays > 0 ? (totalWorkingMinutes / 60.0 / presentDays) : 0;

        // Create report data
        AttendanceReport report = new AttendanceReport(
                studentName,
                selectedMonth,
                totalDays,
                presentDays,
                attendancePercentage,
                lateDays,
                avgWorkingHours
        );

        // Generate PDF
        generatePdfReport(report, false);
    }

    private void processAllStudentsData(DataSnapshot snapshot) {
        List<AttendanceReport> reportList = new ArrayList<>();

        for (Map.Entry<String, String> entry : studentMap.entrySet()) {
            String studentId = entry.getKey();
            String studentName = entry.getValue();

            int totalDays = 0;
            int presentDays = 0;
            int lateDays = 0;
            long totalWorkingMinutes = 0;

            for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                String date = dateSnapshot.getKey();

                if (date != null && date.startsWith(selectedMonth)) {
                    totalDays++;

                    DataSnapshot presentSnapshot = dateSnapshot.child("studentsPresent").child(studentId);
                    if (presentSnapshot.exists()) {
                        presentDays++;

                        Long checkInTime = presentSnapshot.child("checkInTime").getValue(Long.class);
                        Long checkOutTime = presentSnapshot.child("checkOutTime").getValue(Long.class);

                        if (checkInTime != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(checkInTime);
                            int hour = cal.get(Calendar.HOUR_OF_DAY);
                            int minute = cal.get(Calendar.MINUTE);

                            if (hour > 9 || (hour == 9 && minute > 0)) {
                                lateDays++;
                            }
                        }

                        if (checkInTime != null && checkOutTime != null) {
                            long workingMillis = checkOutTime - checkInTime;
                            totalWorkingMinutes += workingMillis / (1000 * 60);
                        }
                    }
                }
            }

            double attendancePercentage = totalDays > 0 ? (presentDays * 100.0 / totalDays) : 0;
            double avgWorkingHours = presentDays > 0 ? (totalWorkingMinutes / 60.0 / presentDays) : 0;

            AttendanceReport report = new AttendanceReport(
                    studentName,
                    selectedMonth,
                    totalDays,
                    presentDays,
                    attendancePercentage,
                    lateDays,
                    avgWorkingHours
            );

            reportList.add(report);
        }

        // Generate PDF for all students
        generatePdfReport(reportList, true);
    }

    private void generatePdfReport(AttendanceReport report, boolean isSingleStudent) {
        try {
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "AttendifyPro");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "Attendance_" + report.getStudentName() + "_" + selectedMonth + ".pdf";
            File pdfFile = new File(directory, fileName);

            PdfReportGenerator.generateIndividualReport(this, pdfFile, report, lobbyName);

            Toast.makeText(this, "Report saved to Downloads/AttendifyPro", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error generating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void generatePdfReport(List<AttendanceReport> reportList, boolean isAllStudents) {
        try {
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "AttendifyPro");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "Attendance_AllStudents_" + selectedMonth + ".pdf";
            File pdfFile = new File(directory, fileName);

            PdfReportGenerator.generateAllStudentsReport(this, pdfFile, reportList, lobbyName, selectedMonth);

            Toast.makeText(this, "Report saved to Downloads/AttendifyPro", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error generating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
