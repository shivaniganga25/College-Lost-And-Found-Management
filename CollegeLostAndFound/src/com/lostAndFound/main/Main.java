package com.lostAndFound.main;

import java.io.IOException;
import java.util.Scanner;

import com.lostAndFound.model.Admin;
import com.lostAndFound.model.Student;
import com.lostAndFound.service.LostFoundService;
import com.lostAndFound.util.FileManager;

public class Main {

    private static final Scanner scanner =
            new Scanner(System.in);

    private static final Admin admin =
            new Admin();

    public static void main(String[] args) {

        FileManager fileManager;
        LostFoundService service;
 
         // To Start the app
         
        try {

            fileManager = new FileManager();
            service = new LostFoundService( fileManager );

        } catch (IOException | IllegalArgumentException e) {

            System.out.println();
            System.out.println("====================================");
            System.out.println("Application could not start.");
            System.out.println(e.getMessage());
            System.out.println("Correct the data file and restart.");
            System.out.println("====================================");

            scanner.close();
            return;
        }

         // Main application loop.
         
        while (true) {

            showMainMenu();
            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1: registerStudent(service);
                    break;
                case 2: showAdminMenu(service);
                    break;
                case 3: showStudentMenu(service);
                    break;
                case 0: System.out.println();
                    System.out.println( "Thank you for using "+ "College Lost and Found "+ "Management System!");

                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. " + "Please try again.");
            }
        }
    }
    // MAIN MENU
    private static void showMainMenu() {

        System.out.println();
        System.out.println("==========================================");
        System.out.println(" COLLEGE LOST AND FOUND MANAGEMENT SYSTEM" );
        System.out.println("==========================================");
        System.out.println("1. Register student");
        System.out.println("2. Admin menu");
        System.out.println("3. Student menu");
        System.out.println("0. Exit");
        System.out.println("==========================================");
    }
    // STUDENT REGISTRATION
    private static void registerStudent(
            LostFoundService service) {

        System.out.println();
        System.out.println("---------- STUDENT REGISTRATION ----------");
        String name = readRequiredText("Enter student name: ");
        String department = readRequiredText("Enter department: ");

        try {
            service.registerStudent(name,department);
        } catch (IOException |
                 IllegalArgumentException e) {
            System.out.println("Registration failed: "+ e.getMessage());
        }
    }
    // ADMIN MENU
    private static void showAdminMenu(
            LostFoundService service) {

        System.out.println();
        System.out.println("--------------- ADMIN LOGIN ---------------");

        admin.showProfile();
        while (true) {
            System.out.println();
            System.out.println("=============== ADMIN MENU ===============");
            System.out.println("1. View all item reports");
            System.out.println("2. Search item descriptions");
            System.out.println("3. Close report");
            System.out.println("4. View summary reports");
            System.out.println("0. Back");

            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1: service.showAllReports();
                    break;
                case 2: searchReports(service);
                    break;
                case 3: closeReport(service);
                    break;
                case 4: summaryMenu(service);
                    break;
                case 0:return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
    // ADMIN SEARCH
    private static void searchReports(
            LostFoundService service) {

        String keyword =
                readRequiredText(
                        "Enter search keyword: "
                );

        service.searchDescriptions(keyword);
    }
    // CLOSE REPORT
    private static void closeReport(LostFoundService service) {
        int reportId = readPositiveInt("Enter Report ID: ");
        String note = readRequiredText("Enter closure note: ");
        try {service.closeReport(reportId,note);
        } catch (IOException |IllegalArgumentException e) {

            System.out.println("Unable to close report: "+ e.getMessage());
        }
    }
    // SUMMARY MENU

    private static void summaryMenu(LostFoundService service) {
        service.showSummary();
        System.out.println();
        String answer = readRequiredText("Save summary to report.txt? (Y/N): ");

        if (answer.equalsIgnoreCase("Y")) {
            try {service.saveSummary();
            } catch (IOException e) {
                System.out.println("Unable to save summary: "+ e.getMessage());
            }
        } else {
            System.out.println("Summary was not saved.");
        }
    }
    // STUDENT MENU

    private static void showStudentMenu(LostFoundService service) {

        System.out.println();
        int studentId =readPositiveInt("Enter student ID: ");
        Student student = service.findStudent(studentId);

        if (student == null) {
            System.out.println("Student ID not found.");
            return;
        }
        System.out.println();
        System.out.println("Welcome, " + student.getName() + "!");
        while (true) {
            System.out.println();
            System.out.println("============== STUDENT MENU ==============");
            System.out.println("1. Submit item report");
            System.out.println("2. View my reports");
            System.out.println("0. Back");

            int choice =readInt("Enter your choice: ");
            switch (choice) {
                case 1:submitReport(service, studentId);
                    break;
                case 2:service.showStudentReports(studentId);
                    break;
                case 0:return;
                default:System.out.println("Invalid choice.");
            }
        }
    }
    // SUBMIT ITEM REPORT
    private static void submitReport(LostFoundService service,int studentId) {
        System.out.println();
        System.out.println("----------- ITEM REPORT ENTRY -----------");
         // Type selection.
        String[] types = service.getTypes();
        System.out.println();
        System.out.println("Report Type:");
        for (int i = 0;i < types.length;i++) {

            System.out.println((i + 1)+ ". "+ types[i]);
        }
        int typeChoice =readChoice("Choose type: ",types.length);
        String type =types[typeChoice - 1];
         //Category selection.
        String[] categories =service.getCategories();
        
        System.out.println();
        System.out.println("Category:");

        for (int i = 0;i < categories.length;i++) {
            System.out.println((i + 1)+ ". "+ categories[i]);
        }
        int categoryChoice =readChoice("Choose category: ",categories.length);
        String category =categories[categoryChoice - 1];
             //Description.
        String description;
        while (true) {
            description =readRequiredText("Enter item description: ");
            if (description.length() > 200) {

                System.out.println("Description cannot exceed "+ "200 characters.");
                continue;
            }
            if (description.contains("|")) {
                System.out.println("Description cannot contain |.");
                continue;
            }
            break;
        }
        try {service.submitReport(studentId,type,category,description);
        } 
        catch (IOException |IllegalArgumentException e) {
            System.out.println("Report submission failed: "+ e.getMessage());
        }
    }
    // INPUT METHODS
    /* Reads an integer.
     * We use nextLine() everywhere.
     * This avoids Scanner nextInt()/nextLine()
     * problems.*/
    private static int readInt(String message) {
        while (true) {
            System.out.print(message);
            String input =scanner.nextLine().trim();
            try {return Integer.parseInt(input);
            } 
            catch (NumberFormatException e) {
            	System.out.println("Please enter a valid number.");
            }
        }
    }
            // Reads a positive integer.
    private static int readPositiveInt(String message) {
        while (true) {
            int value =readInt(message);
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be positive.");
        }
    }
            // Reads a number within a range.
    private static int readChoice(String message,int max) {
        while (true) {
            int choice =readInt(message);
            if (choice >= 1 && choice <= max) {
                return choice;
            }
            System.out.println("Please choose a number "+ "between 1 and "+ max + ".");
        }
    }
    //Reads a required text field.
    private static String readRequiredText(String message) {
        while (true) {
            System.out.print(message);
            String value =scanner.nextLine().trim();
            if (value.isEmpty()) {
                System.out.println("This field cannot be empty.");
                continue;
            }
            if (value.contains("|")) {
                System.out.println("This field cannot contain |.");
                continue;
            }
            return value;
        }
    }
}