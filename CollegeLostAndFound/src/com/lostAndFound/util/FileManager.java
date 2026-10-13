package com.lostAndFound.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.lostAndFound.model.ItemReport;
import com.lostAndFound.model.ReportStatus;
import com.lostAndFound.model.Student;

public class FileManager {

private final Path dataFolder = Path.of("data");
private final Path studentsFile = dataFolder.resolve("students.txt");
private final Path reportsFile = dataFolder.resolve("reports.txt");
private final Path reportFile = dataFolder.resolve("report.txt");

// Creates data folder if it does not exist.
public FileManager() throws IOException {
Files.createDirectories(dataFolder);
}

// LOAD STUDENTS
public ArrayList<Student> loadStudents() throws IOException {
ArrayList<Student> students = new ArrayList<>();

if (!Files.exists(studentsFile)) 
{
return students;
}

Set<Integer> usedIds = new HashSet<>();

try (BufferedReader reader = Files.newBufferedReader(studentsFile, StandardCharsets.UTF_8)) {
String line;
int lineNumber = 0;

while ((line = reader.readLine()) != null) {
lineNumber++;

// Empty lines are not allowed.
if (line.trim().isEmpty()) {
throw invalidFile(studentsFile, lineNumber, "Empty line.");
}

// -1 preserves empty final fields.
String[] fields = line.split("\\|", -1);

if (fields.length != 3) {
throw invalidFile(studentsFile, lineNumber, "Expected 3 fields.");
}

int id = parsePositiveId(fields[0], studentsFile, lineNumber);

if (!usedIds.add(id)) {
throw invalidFile(studentsFile, lineNumber, "Duplicate student ID.");
}

String name = fields[1].trim();
String department = fields[2].trim();

validateTextField(name, "Student name", studentsFile, lineNumber);
validateTextField(department, "Department", studentsFile, lineNumber);

students.add(new Student(id, name, department));
}
}

return students;
}

// LOAD REPORTS
public ArrayList<ItemReport> loadReports(List<Student> students) throws IOException {
ArrayList<ItemReport> reports = new ArrayList<>();

if (!Files.exists(reportsFile)) {
return reports;
}

Set<Integer> usedIds = new HashSet<>();
Set<Integer> studentIds = new HashSet<>();

for (Student student : students) {
studentIds.add(student.getId());
}

try (BufferedReader reader = Files.newBufferedReader(reportsFile, StandardCharsets.UTF_8)) {
String line;
int lineNumber = 0;

while ((line = reader.readLine()) != null) {
lineNumber++;

if (line.trim().isEmpty()) {
throw invalidFile(reportsFile, lineNumber, "Empty line.");
}

// -1 preserves an empty final closureNote.
String[] fields = line.split("\\|", -1);

if (fields.length != 7) {
throw invalidFile(reportsFile, lineNumber, "Expected 7 fields.");
}

int id = parsePositiveId(fields[0], reportsFile, lineNumber);

if (!usedIds.add(id)) {
throw invalidFile(reportsFile, lineNumber, "Duplicate report ID.");
}

int studentId = parsePositiveId(fields[1], reportsFile, lineNumber);

// Every report must belong to an existing student.
if (!studentIds.contains(studentId)) {
throw invalidFile(reportsFile, lineNumber, "Student ID does not exist.");
}

String type = fields[2].trim();
String category = fields[3].trim();
String description = fields[4].trim();
String statusText = fields[5].trim();
String closureNote = fields[6].trim();

validateType(type, reportsFile, lineNumber);
validateCategory(category, reportsFile, lineNumber);
validateDescription(description, reportsFile, lineNumber);

ReportStatus status;
try {
status = ReportStatus.valueOf(statusText);
} catch (IllegalArgumentException e) {
throw invalidFile(reportsFile, lineNumber, "Invalid status. Use OPEN or CLOSED.");
}

// Validate OPEN/CLOSED and closure note.
if (status == ReportStatus.OPEN) {
if (!closureNote.isEmpty()) {
throw invalidFile(reportsFile, lineNumber, "OPEN report must have an empty closure note.");
}
} else {
if (closureNote.isEmpty()) {
throw invalidFile(reportsFile, lineNumber, "CLOSED report must have a closure note.");
}
if (closureNote.length() > 200) {
throw invalidFile(reportsFile, lineNumber, "Closure note exceeds 200 characters.");
}
}

ItemReport report = new ItemReport(id, studentId, type, category, description);

if (status == ReportStatus.CLOSED) {
report.close(closureNote);
}

reports.add(report);
}
}

return reports;
}

// SAVE STUDENTS

public void saveStudents(List<Student> students) throws IOException {
writeAtomically(studentsFile, writer -> {
for (Student student : students) {
writer.write(student.getId() + "|" + student.getName() + "|" + student.getDepartment());
writer.newLine();
}
});
}

// SAVE REPORTS

public void saveReports(List<ItemReport> reports) throws IOException {
writeAtomically(reportsFile, writer -> {
for (ItemReport report : reports) {
writer.write(report.getId() + "|" + report.getStudentId() + "|" + report.getType() + "|" + report.getCategory() + "|" + report.getDescription() + "|" + report.getStatus() + "|" + report.getClosureNote());
writer.newLine();
}
});
}

// SAVE SUMMARY REPORT

public void saveReport(int totalReports, int openLost, int openFound, int closedReports, int distinctCategories) throws IOException {
writeAtomically(reportFile, writer -> {
writer.write("College Lost and Found Management System");
writer.newLine();
writer.write("Total Reports: " + totalReports);
writer.newLine();
writer.write("OPEN Lost Reports: " + openLost);
writer.newLine();
writer.write("OPEN Found Reports: " + openFound);
writer.newLine();
writer.write("CLOSED Reports: " + closedReports);
writer.newLine();
writer.write("Distinct Categories: " + distinctCategories);
writer.newLine();
});
}

// ATOMIC FILE WRITING

// Writes to a temporary file first. Only after writing succeeds,
// the temporary file replaces the original file.
private void writeAtomically(Path target, WriterAction action) throws IOException {
Files.createDirectories(dataFolder);

Path tempFile = Files.createTempFile(dataFolder, target.getFileName().toString(), ".tmp");

boolean success = false;

try {
try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
action.write(writer);
}

try {
Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
} catch (AtomicMoveNotSupportedException e) {
Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
}

success = true;

} finally {
if (!success) {
Files.deleteIfExists(tempFile);
}
}
}

// VALIDATION HELPERS
private int parsePositiveId(String value, Path file, int lineNumber) throws IOException {
int id;

try {
id = Integer.parseInt(value.trim());
} catch (NumberFormatException e) {
throw invalidFile(file, lineNumber, "ID must be a number.");
}

if (id <= 0) {
throw invalidFile(file, lineNumber, "ID must be positive.");
}

return id;
}

private void validateTextField(String value, String fieldName, Path file, int lineNumber) throws IOException {
if (value.isEmpty()) {
throw invalidFile(file, lineNumber, fieldName + " cannot be empty.");
}

if (containsInvalidCharacters(value)) {
throw invalidFile(file, lineNumber, fieldName + " contains invalid characters.");
}
}

private void validateDescription(String description, Path file, int lineNumber) throws IOException {
validateTextField(description, "Description", file, lineNumber);

if (description.length() > 200) {
throw invalidFile(file, lineNumber, "Description exceeds 200 characters.");
}
}

private void validateType(String type, Path file, int lineNumber) throws IOException {
if (!type.equals("Lost") && !type.equals("Found")) {
throw invalidFile(file, lineNumber, "Invalid type.");
}
}

private void validateCategory(String category, Path file, int lineNumber) throws IOException {
if (!category.equals("Books") && !category.equals("ID Cards") && !category.equals("Electronics") && !category.equals("Other")) {
throw invalidFile(file, lineNumber, "Invalid category.");
}
}

private boolean containsInvalidCharacters(String value) {
return value.contains("|") || value.contains("\n") || value.contains("\r");
}

private IOException invalidFile(Path file, int lineNumber, String message) {
return new IOException("Invalid data in " + file + " at line " + lineNumber + ": " + message);
}

// FUNCTIONAL INTERFACE

@FunctionalInterface
private interface WriterAction {
void write(BufferedWriter writer) throws IOException;
}
}