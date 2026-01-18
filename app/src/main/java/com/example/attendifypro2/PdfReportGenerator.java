package com.example.attendifypro2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {

    private static final int PAGE_WIDTH = 595; // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 40;

    public static void generateIndividualReport(Context context, File file,
                                                AttendanceReport report, String lobbyName) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        int y = MARGIN;

        // Title
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Attendance Report", MARGIN, y, paint);
        y += 40;

        // Date
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + currentDate, MARGIN, y, paint);
        y += 30;

        // Lobby name
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Lobby: " + lobbyName, MARGIN, y, paint);
        y += 25;

        // Student name
        canvas.drawText("Student: " + report.getStudentName(), MARGIN, y, paint);
        y += 25;

        // Month
        canvas.drawText("Month: " + report.getMonth(), MARGIN, y, paint);
        y += 40;

        // Draw line
        paint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 30;

        // Summary section
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("Summary", MARGIN, y, paint);
        y += 30;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        // Total days
        canvas.drawText("Total Working Days: " + report.getTotalDays(), MARGIN + 20, y, paint);
        y += 25;

        // Present days
        canvas.drawText("Days Present: " + report.getPresentDays(), MARGIN + 20, y, paint);
        y += 25;

        // Absent days
        int absentDays = report.getTotalDays() - report.getPresentDays();
        canvas.drawText("Days Absent: " + absentDays, MARGIN + 20, y, paint);
        y += 25;

        // Attendance percentage
        paint.setFakeBoldText(true);
        String percentageText = String.format("Attendance Percentage: %.2f%%", report.getAttendancePercentage());

        // Color code based on percentage
        if (report.getAttendancePercentage() >= 75) {
            paint.setColor(Color.rgb(34, 139, 34)); // Green
        } else if (report.getAttendancePercentage() >= 50) {
            paint.setColor(Color.rgb(255, 165, 0)); // Orange
        } else {
            paint.setColor(Color.rgb(220, 20, 60)); // Red
        }
        canvas.drawText(percentageText, MARGIN + 20, y, paint);
        paint.setColor(Color.BLACK);
        y += 35;

        paint.setFakeBoldText(false);

        // Late days
        canvas.drawText("Late Entries: " + report.getLateDays(), MARGIN + 20, y, paint);
        y += 25;

        // Average working hours
        String workingHoursText = String.format("Avg Working Hours: %.2f hrs/day", report.getAvgWorkingHours());
        canvas.drawText(workingHoursText, MARGIN + 20, y, paint);
        y += 40;

        // Draw chart (simple bar chart)
        y = drawAttendanceChart(canvas, report, y);

        document.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        document.close();
    }

    public static void generateAllStudentsReport(Context context, File file,
                                                 List<AttendanceReport> reportList,
                                                 String lobbyName, String month) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        int y = MARGIN;

        // Title
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("All Students Report", MARGIN, y, paint);
        y += 40;

        // Date
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + currentDate, MARGIN, y, paint);
        y += 30;

        // Lobby name
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Lobby: " + lobbyName, MARGIN, y, paint);
        y += 25;

        // Month
        canvas.drawText("Month: " + month, MARGIN, y, paint);
        y += 40;

        // Draw line
        paint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 30;

        // Table header
        paint.setTextSize(10);
        paint.setFakeBoldText(true);
        canvas.drawText("Student", MARGIN, y, paint);
        canvas.drawText("Present", MARGIN + 150, y, paint);
        canvas.drawText("Absent", MARGIN + 220, y, paint);
        canvas.drawText("%", MARGIN + 290, y, paint);
        canvas.drawText("Late", MARGIN + 350, y, paint);
        canvas.drawText("Avg Hrs", MARGIN + 410, y, paint);
        y += 20;

        // Draw header line
        paint.setStrokeWidth(1);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 15;

        // Table rows
        paint.setFakeBoldText(false);
        int pageNumber = 1;
        int rowCount = 0;

        for (AttendanceReport report : reportList) {
            // Check if we need a new page
            if (y > PAGE_HEIGHT - 100) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = MARGIN;

                // Redraw header on new page
                paint.setFakeBoldText(true);
                canvas.drawText("Student", MARGIN, y, paint);
                canvas.drawText("Present", MARGIN + 150, y, paint);
                canvas.drawText("Absent", MARGIN + 220, y, paint);
                canvas.drawText("%", MARGIN + 290, y, paint);
                canvas.drawText("Late", MARGIN + 350, y, paint);
                canvas.drawText("Avg Hrs", MARGIN + 410, y, paint);
                y += 20;
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
                y += 15;
                paint.setFakeBoldText(false);
            }

            // Truncate long names
            String studentName = report.getStudentName();
            if (studentName.length() > 20) {
                studentName = studentName.substring(0, 17) + "...";
            }

            int absentDays = report.getTotalDays() - report.getPresentDays();

            canvas.drawText(studentName, MARGIN, y, paint);
            canvas.drawText(String.valueOf(report.getPresentDays()), MARGIN + 150, y, paint);
            canvas.drawText(String.valueOf(absentDays), MARGIN + 220, y, paint);

            // Color code percentage
            Paint percentPaint = new Paint(paint);
            if (report.getAttendancePercentage() >= 75) {
                percentPaint.setColor(Color.rgb(34, 139, 34));
            } else if (report.getAttendancePercentage() >= 50) {
                percentPaint.setColor(Color.rgb(255, 165, 0));
            } else {
                percentPaint.setColor(Color.rgb(220, 20, 60));
            }
            canvas.drawText(String.format("%.1f", report.getAttendancePercentage()), MARGIN + 290, y, percentPaint);

            canvas.drawText(String.valueOf(report.getLateDays()), MARGIN + 350, y, paint);
            canvas.drawText(String.format("%.1f", report.getAvgWorkingHours()), MARGIN + 410, y, paint);

            y += 18;
            rowCount++;
        }

        // Summary statistics
        y += 30;
        paint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 25;

        paint.setFakeBoldText(true);
        canvas.drawText("Overall Statistics", MARGIN, y, paint);
        y += 25;

        paint.setFakeBoldText(false);

        // Calculate overall stats
        double avgAttendance = reportList.stream()
                .mapToDouble(AttendanceReport::getAttendancePercentage)
                .average()
                .orElse(0.0);

        int totalLate = reportList.stream()
                .mapToInt(AttendanceReport::getLateDays)
                .sum();

        canvas.drawText("Total Students: " + reportList.size(), MARGIN + 20, y, paint);
        y += 20;
        canvas.drawText(String.format("Average Attendance: %.2f%%", avgAttendance), MARGIN + 20, y, paint);
        y += 20;
        canvas.drawText("Total Late Entries: " + totalLate, MARGIN + 20, y, paint);

        document.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        document.close();
    }

    private static int drawAttendanceChart(Canvas canvas, AttendanceReport report, int startY) {
        Paint paint = new Paint();
        int chartY = startY;

        // Chart title
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Attendance Chart", MARGIN, chartY, paint);
        chartY += 30;

        // Chart dimensions
        int chartWidth = PAGE_WIDTH - (2 * MARGIN);
        int chartHeight = 150;
        int barHeight = 30;

        // Draw present bar
        paint.setColor(Color.rgb(34, 139, 34));
        int presentWidth = (int) ((report.getPresentDays() * 1.0 / report.getTotalDays()) * chartWidth);
        canvas.drawRect(MARGIN, chartY, MARGIN + presentWidth, chartY + barHeight, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText("Present: " + report.getPresentDays(), MARGIN + 10, chartY + 20, paint);

        chartY += barHeight + 10;

        // Draw absent bar
        int absentDays = report.getTotalDays() - report.getPresentDays();
        paint.setColor(Color.rgb(220, 20, 60));
        int absentWidth = (int) ((absentDays * 1.0 / report.getTotalDays()) * chartWidth);
        canvas.drawRect(MARGIN, chartY, MARGIN + absentWidth, chartY + barHeight, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText("Absent: " + absentDays, MARGIN + 10, chartY + 20, paint);

        chartY += barHeight + 10;

        // Draw late bar
        paint.setColor(Color.rgb(255, 165, 0));
        int lateWidth = (int) ((report.getLateDays() * 1.0 / report.getTotalDays()) * chartWidth);
        canvas.drawRect(MARGIN, chartY, MARGIN + lateWidth, chartY + barHeight, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText("Late: " + report.getLateDays(), MARGIN + 10, chartY + 20, paint);

        return chartY + barHeight + 40;
    }
}