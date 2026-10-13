package com.lostAndFound.model;

public class Student extends User {

    private String department;

    public Student(
            int id,
            String name,
            String department) {

        super(id, name);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    @Override
    public void showProfile() {

        System.out.println("-------------------------");
        System.out.println("Student ID   : " + getId());
        System.out.println("Student Name : " + getName());
        System.out.println("Department   : " + department);
        System.out.println("-------------------------");
    }
}
