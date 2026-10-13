package com.lostAndFound.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import com.lostAndFound.model.Student;
import com.lostAndFound.model.ItemReport;
import com.lostAndFound.model.ReportStatus;
import com.lostAndFound.util.FileManager;

public class LostFoundService {

    private ArrayList<Student> students;
    private ArrayList<ItemReport> reports;

    private int nextStudentId;
    private int nextReportId;

    private final FileManager fileManager;

    // Fixed arrays required by the project.
    private final String[] types = {"Lost", "Found"};
    private final String[] categories = {"Books", "ID Cards", "Electronics", "Other"};

    // Constructor. Loads existing data when the application starts.
    public LostFoundService(FileManager fileManager) throws IOException {
        this.fileManager = fileManager;
        this.students = fileManager.loadStudents();
        this.reports = fileManager.loadReports(students);
        this.nextStudentId = findNextStudentId();
        this.nextReportId = findNextReportId();
    }
    // STUDENT MANAGEMENT
    
    // Register a new student.
    public void registerStudent(String name, String department) throws IOException {
        validateText(name, "Student name");
        validateText(department, "Department");

        Student student = new Student(nextStudentId, name.trim(), department.trim());

        // Save first; change the in-memory list only after saving succeeds.
        ArrayList<Student> updatedStudents = new ArrayList<>(students);
        updatedStudents.add(student);
        fileManager.saveStudents(updatedStudents);
        students = updatedStudents;

        System.out.println();
        System.out.println("Student registered successfully.");
        System.out.println("Student ID: " + nextStudentId);
        nextStudentId++;
    }

    // Find student using Student ID.
    public Student findStudent(int studentId) {
        for (Student student : students) {
            if (student.getId() == studentId) {
                return student;
            }
        }
        return null;
    }

    // Display all students.
    public void showAllStudents() {
        if (students.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Student student : students) {
            student.showProfile();
        }
    }
    // REPORT MANAGEMENT

    // Return the allowed report types.
    public String[] getTypes() {
        return types.clone();
    }

    // Return the allowed categories.
    public String[] getCategories() {
        return categories.clone();
    }

    // Submit a Lost or Found report.
    public void submitReport(int studentId, String type, String category, String description)
            throws IOException {

        Student student = findStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student ID not found.");
        }
        if (!isValidType(type)) {
            throw new IllegalArgumentException("Invalid report type.");
        }
        if (!isValidCategory(category)) {
            throw new IllegalArgumentException("Invalid category.");
        }
        validateDescription(description);

        ItemReport report = new ItemReport(nextReportId, studentId, type, category, description.trim());

        // Save first; update memory only after a successful save.
        ArrayList<ItemReport> updatedReports = new ArrayList<>(reports);
        updatedReports.add(report);
        fileManager.saveReports(updatedReports);
        reports = updatedReports;

        System.out.println();
        System.out.println("Report submitted successfully.");
        System.out.println("Report ID: " + nextReportId);
        System.out.println("Status: OPEN");
        nextReportId++;
    }

    // Display all reports.
    public void showAllReports() {
        if (reports.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (ItemReport report : reports) {
            displayReport(report);
        }
    }

    // Display reports belonging to one student.
    public void showStudentReports(int studentId) {
        boolean found = false;
        for (ItemReport report : reports) {
            if (report.getStudentId() == studentId) {
                displayReport(report);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No records found.");
        }
    }

    // Display one report.
    private void displayReport(ItemReport report) {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("Report ID    : " + report.getId());
        System.out.println("Student ID   : " + report.getStudentId());
        System.out.println("Type         : " + report.getType());
        System.out.println("Category     : " + report.getCategory());
        System.out.println("Description  : " + report.getDescription());
        System.out.println("Status       : " + report.getStatus());
        System.out.println("Closure Note : " + report.getClosureNote());
        System.out.println("----------------------------------------");
    }
    // SEARCH

    // Search report descriptions (case-insensitive).
    public void searchDescriptions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        String searchKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        boolean found = false;

        for (ItemReport report : reports) {
            String description = report.getDescription().toLowerCase(Locale.ROOT);
            if (description.contains(searchKeyword)) {
                displayReport(report);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching records found.");
        }
    }
    // CLOSE REPORT

    // Find a report by Report ID.
    public ItemReport findReport(int reportId) {
        for (ItemReport report : reports) {
            if (report.getId() == reportId) {
                return report;
            }
        }
        return null;
    }

    // Close an OPEN report.
    public void closeReport(int reportId, String closureNote) throws IOException {
        ItemReport existing = findReport(reportId);

        if (existing == null) {
            throw new IllegalArgumentException("Report ID not found.");
        }

        // A closed report cannot be closed again.
        if (existing.getStatus() == ReportStatus.CLOSED) {
            throw new IllegalArgumentException("Report is already closed.");
        }

        validateClosureNote(closureNote);

        // Build a new list so memory changes only after the file is saved.
        ArrayList<ItemReport> updatedReports = new ArrayList<>();
        for (ItemReport report : reports) {
            ItemReport copy = new ItemReport(report);
            if (copy.getId() == reportId) {
                copy.close(closureNote.trim());
            }
            updatedReports.add(copy);
        }

        fileManager.saveReports(updatedReports);
        reports = updatedReports;

        System.out.println();
        System.out.println("Report closed successfully.");
        System.out.println("Report ID: " + reportId);
        System.out.println("Status: CLOSED");
    }
    // SUMMARY

    // Display summary report.
    public void showSummary() {
        SummaryData summary = calculateSummary();

        System.out.println();
        System.out.println("========== SUMMARY REPORT ==========");
        System.out.println("Total Reports : " + summary.totalReports);
        System.out.println("OPEN Lost     : " + summary.openLost);
        System.out.println("OPEN Found    : " + summary.openFound);
        System.out.println("CLOSED        : " + summary.closedReports);
        System.out.println("Categories    : " + summary.distinctCategories);
        System.out.println("====================================");
    }

    // Save summary into report.txt.
    public void saveSummary() throws IOException {
        SummaryData summary = calculateSummary();

        fileManager.saveReport(
                summary.totalReports,
                summary.openLost,
                summary.openFound,
                summary.closedReports,
                summary.distinctCategories);

        System.out.println();
        System.out.println("Summary saved successfully to data/report.txt");
    }

    // Calculate all summary values.
    private SummaryData calculateSummary() {
        int totalReports = reports.size();
        int openLost = 0;
        int openFound = 0;
        int closedReports = 0;

        // HashSet stores only unique categories.
        HashSet<String> reportedCategories = new HashSet<>();

        for (ItemReport report : reports) {
            reportedCategories.add(report.getCategory());

            if (report.getStatus() == ReportStatus.CLOSED) {
                closedReports++;
            } else if (report.getType().equals("Lost")) {
                openLost++;
            } else if (report.getType().equals("Found")) {
                openFound++;
            }
        }

        return new SummaryData(totalReports, openLost, openFound, closedReports,
                reportedCategories.size());
    }
    // VALIDATION

    private void validateText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        if (containsInvalidCharacters(value)) {
            throw new IllegalArgumentException(fieldName + " cannot contain | or line breaks.");
        }
    }

    private void validateDescription(String description) {
        validateText(description, "Description");
        if (description.trim().length() > 200) {
            throw new IllegalArgumentException("Description cannot exceed 200 characters.");
        }
    }

    private void validateClosureNote(String note) {
        validateText(note, "Closure note");
        if (note.trim().length() > 200) {
            throw new IllegalArgumentException("Closure note cannot exceed 200 characters.");
        }
    }

    private boolean containsInvalidCharacters(String value) {
        return value.contains("|") || value.contains("\n") || value.contains("\r");
    }

    private boolean isValidType(String type) {
        if (type == null) {
            return false;
        }
        for (String allowedType : types) {
            if (allowedType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }
        for (String allowedCategory : categories) {
            if (allowedCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }
    // ID GENERATION

    private int findNextStudentId() {
        int maxId = 0;
        for (Student student : students) {
            if (student.getId() > maxId) {
                maxId = student.getId();
            }
        }
        return maxId + 1;
    }

    private int findNextReportId() {
        int maxId = 0;
        for (ItemReport report : reports) {
            if (report.getId() > maxId) {
                maxId = report.getId();
            }
        }
        return maxId + 1;
    }
    // INNER CLASS FOR SUMMARY

    private static class SummaryData {
        private final int totalReports;
        private final int openLost;
        private final int openFound;
        private final int closedReports;
        private final int distinctCategories;

        public SummaryData(int totalReports, int openLost, int openFound,
                int closedReports, int distinctCategories) {
            this.totalReports = totalReports;
            this.openLost = openLost;
            this.openFound = openFound;
            this.closedReports = closedReports;
            this.distinctCategories = distinctCategories;
        }
    }
}
