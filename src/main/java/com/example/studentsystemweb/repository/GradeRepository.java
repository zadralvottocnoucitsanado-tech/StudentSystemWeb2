package com.example.studentsystemweb.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.studentsystemweb.model.Grade;

@Repository
public class GradeRepository {

    private final JdbcTemplate jdbcTemplate;

    public GradeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------- Маппер ----------
    private final RowMapper<Grade> mapper = (rs, rowNum) -> {
        Grade g = new Grade();
        g.setId(rs.getInt("id"));
        g.setStudentId(rs.getInt("student_id"));

        String subject = rs.getString("subject");
        String wt = rs.getString("work_type");

        g.setSubject(subject == null ? null : subject.trim());
        g.setWorkType(wt == null ? null : wt.trim());
        g.setGrade(rs.getDouble("grade"));

        return g;
    };

    // ---------- Нормализация строк ----------
    private String norm(String s) {
        if (s == null) return null;
        return s.trim().replace("\u00A0", ""); // убираем невидимые пробелы
    }

    // ===========================================================
    // 1. Полный список оценок конкретного студента
    // ===========================================================
    public List<Grade> findByStudent(int studentId) {
        return jdbcTemplate.query(
                "SELECT * FROM grades WHERE student_id = ? ORDER BY subject, work_type",
                mapper, studentId
        );
    }

    // ===========================================================
    // 2. Предметы студента
    // ===========================================================
    public List<String> findSubjectsForStudent(int studentId) {
        return jdbcTemplate.query(
                "SELECT DISTINCT subject FROM grades WHERE student_id = ? ORDER BY subject",
                (rs, i) -> {
                    String s = rs.getString(1);
                    return s == null ? null : s.trim();
                },
                studentId
        );
    }

    // ===========================================================
    // 3. Все компоненты по предмету студента
    // ===========================================================
    public List<Grade> findComponentsForSubject(int studentId, String subject) {
        subject = norm(subject);

        return jdbcTemplate.query(
                "SELECT * FROM grades WHERE student_id = ? AND subject = ? ORDER BY id",
                mapper, studentId, subject
        );
    }

    // ===========================================================
    // 4. Map<work_type, grade> по предмету
    // ===========================================================
    public Map<String, Double> getComponentMap(int studentId, String subject) {

        Map<String, Double> map = new HashMap<>();
        List<Grade> list = findComponentsForSubject(studentId, subject);

        for (Grade g : list) {
            String wt = norm(g.getWorkType());
            if (wt != null && !wt.isEmpty()) {
                map.put(wt, g.getGrade());
            }
        }
        return map;
    }

    // ===========================================================
    // 5. INSERT новой компоненты
    // ===========================================================
    public void insertComponent(int studentId, String subject, String workType, double grade) {

        subject = norm(subject);
        workType = norm(workType);

        jdbcTemplate.update(
                "INSERT INTO grades(student_id, subject, work_type, grade) VALUES (?, ?, ?, ?)",
                studentId, subject, workType, grade
        );
    }

    // ===========================================================
    // 6. UPDATE по ID
    // ===========================================================
    public void updateById(int id, double grade) {
        jdbcTemplate.update(
                "UPDATE grades SET grade = ? WHERE id = ?",
                grade, id
        );
    }

    // ===========================================================
    // 7. Найти одну компоненту
    // ===========================================================
    public Grade findOneComponent(int studentId, String subject, String workType) {

        subject = norm(subject);
        workType = norm(workType);

        List<Grade> list = jdbcTemplate.query(
                "SELECT * FROM grades WHERE student_id = ? AND subject = ? AND work_type = ? LIMIT 1",
                mapper, studentId, subject, workType
        );

        return list.isEmpty() ? null : list.get(0);
    }

    // ===========================================================
    // 8. UPSERT (добавить или обновить)
    // ===========================================================
    public void upsertComponent(int studentId, String subject, String workType, Double grade) {

        if (grade == null) return;

        subject = norm(subject);
        workType = norm(workType);

        Grade exists = findOneComponent(studentId, subject, workType);

        if (exists == null) {
            insertComponent(studentId, subject, workType, grade);
        } else {
            updateById(exists.getId(), grade);
        }
    }

    // ===========================================================
    // 9. Глобальный средний балл
    // ===========================================================
    public Double globalAverage() {
        Double v = jdbcTemplate.queryForObject(
                "SELECT AVG(grade) FROM grades",
                Double.class
        );
        return v != null ? v : 0.0;
    }

    // 
    // 10. SUPER IMPORTANT 
    // 
    public List<Grade> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM grades ORDER BY student_id, subject, work_type",
                mapper
        );
    }
}
