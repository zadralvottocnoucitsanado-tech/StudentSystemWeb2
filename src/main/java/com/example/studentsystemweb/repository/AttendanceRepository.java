package com.example.studentsystemweb.repository;

import com.example.studentsystemweb.model.Attendance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AttendanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Attendance> mapper = (rs, rowNum) -> {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setDate(rs.getString("date"));
        a.setSubject(rs.getString("subject"));
        a.setStatus(rs.getString("status"));
        return a;
    };

    public List<Attendance> findByStudentId(int studentId) {
        return jdbcTemplate.query(
                "SELECT * FROM attendance WHERE student_id = ? ORDER BY id DESC",
                mapper, studentId
        );
    }

    public int countPresent(int studentId) {
        Integer val = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND LOWER(status) = 'present'",
                Integer.class, studentId
        );
        return val != null ? val : 0;
    }

    public int countAbsent(int studentId) {
        Integer val = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND LOWER(status) = 'absent'",
                Integer.class, studentId
        );
        return val != null ? val : 0;
    }

    public List<Map<String, Object>> absentLeaders(int minAbs) {
        return jdbcTemplate.queryForList(
                """
                SELECT s.fullname, s.group_name, COUNT(a.id) AS abs
                FROM attendance a
                JOIN students s ON s.id = a.student_id
                WHERE LOWER(a.status) = 'absent'
                GROUP BY a.student_id
                HAVING abs >= ?
                """,
                minAbs
        );
    }
}
