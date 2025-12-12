package com.example.studentsystemweb.model;

public class ProgressRecord {
    private Integer id;
    private Integer studentId;
    private String workType;
    private Double score;
    private String createdAt;

    public ProgressRecord() {}

    public ProgressRecord(Integer id, Integer studentId, String workType, Double score, String createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.workType = workType;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
