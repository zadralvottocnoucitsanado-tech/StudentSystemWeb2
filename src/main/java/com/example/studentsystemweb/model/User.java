package com.example.studentsystemweb.model;

public class User {

    private Integer id;
    private String login;
    private String password;
    private String role;
    private Integer linkedStudentId;

    public User() {}

    public User(Integer id, String login, String password, String role, Integer linkedStudentId) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.role = role;
        this.linkedStudentId = linkedStudentId;
    }

    public Integer getId() { return id; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public Integer getLinkedStudentId() { return linkedStudentId; }

    public void setId(Integer id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setLinkedStudentId(Integer linkedStudentId) { this.linkedStudentId = linkedStudentId; }
}
