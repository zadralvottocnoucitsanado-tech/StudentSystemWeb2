package com.example.studentsystemweb.model;

public class Student {
    private Integer id;
    private String fullname;
    private String groupName;

    public Student() {}

    public Student(Integer id, String fullname, String groupName) {
        this.id = id;
        this.fullname = fullname;
        this.groupName = groupName;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
