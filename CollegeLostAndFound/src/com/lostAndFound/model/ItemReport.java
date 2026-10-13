package com.lostAndFound.model;

public class ItemReport {

    private int id;
    private int studentId;
    private String type;
    private String category;
    private String description;
    private ReportStatus status;
    private String closureNote;

    public ItemReport(
            int id,
            int studentId,
            String type,
            String category,
            String description) {

        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.category = category;
        this.description = description;

        this.status = ReportStatus.OPEN;
        this.closureNote = "";
    }

    /*
     * Copy constructor.
     * Used when we need to safely update a report
     * before saving it to the file.
     */
    public ItemReport(ItemReport other) {

        this.id = other.id;
        this.studentId = other.studentId;
        this.type = other.type;
        this.category = other.category;
        this.description = other.description;
        this.status = other.status;
        this.closureNote = other.closureNote;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public void close(String note) {
    	
        if (status == ReportStatus.CLOSED) {
            return;
        }
        status = ReportStatus.CLOSED;
        closureNote = note;
    }
}