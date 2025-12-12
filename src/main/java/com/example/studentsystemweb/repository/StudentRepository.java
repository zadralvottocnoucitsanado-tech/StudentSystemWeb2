package com.example.studentsystemweb.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.studentsystemweb.model.Student;

@Repository
public class StudentRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Student> mapper = (rs, rowNum) -> {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setFullname(rs.getString("fullname"));
        s.setGroupName(rs.getString("group_name"));
        return s;
    };

    public Student findById(int id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM students WHERE id = ?",
                mapper, id
        );
    }

    public List<Student> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM students ORDER BY id ASC",
                mapper
        );
    }

    public void insert(String fullname, String group) {
        jdbcTemplate.update(
                "INSERT INTO students(fullname, group_name) VALUES (?, ?)",
                fullname, group
        );
    }

    public void delete(int id) {
        jdbcTemplate.update(
                "DELETE FROM students WHERE id = ?",
                id
        );
    }

   
    public List<String> findAllGroups() {
        return jdbcTemplate.query(
                "SELECT DISTINCT group_name FROM students ORDER BY group_name",
                (rs, i) -> rs.getString(1)
        );
    }

    
    public List<Student> findByGroup(String group) {
        return jdbcTemplate.query(
                "SELECT * FROM students WHERE group_name = ? ORDER BY id ASC",
                mapper,
                group
        );
    }
}
