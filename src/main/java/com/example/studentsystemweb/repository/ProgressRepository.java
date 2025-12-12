package com.example.studentsystemweb.repository;

import com.example.studentsystemweb.model.ProgressRecord;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProgressRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProgressRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ProgressRecord> mapper = (rs, rowNum) -> {
        ProgressRecord p = new ProgressRecord();
        p.setId(rs.getInt("id"));
        p.setStudentId(rs.getInt("student_id"));
        p.setWorkType(rs.getString("work_type"));
        p.setScore(rs.getDouble("score"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    };

    public List<ProgressRecord> findByStudent(int studentId) {
        return jdbcTemplate.query(
                "SELECT * FROM progress WHERE student_id = ? ORDER BY id DESC",
                mapper, studentId
        );
    }
}
