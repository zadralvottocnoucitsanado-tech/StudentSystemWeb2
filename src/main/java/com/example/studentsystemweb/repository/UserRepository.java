package com.example.studentsystemweb.repository;

import com.example.studentsystemweb.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> mapper = (rs, rowNum) ->
            new User(
                    rs.getInt("id"),
                    rs.getString("login"),
                    rs.getString("password"),
                    rs.getString("role"),
                    (Integer) rs.getObject("linked_student_id")
            );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findByLogin(String login) {
        List<User> list = jdbcTemplate.query(
                "SELECT * FROM users WHERE login = ?",
                mapper,
                login
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY id", mapper);
    }

    public void createUser(String login, String password, String role, Integer linkedStudentId) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO users(login, password, role, linked_student_id) VALUES (?, ?, ?, ?)",
                    login,
                    password,
                    role,
                    linkedStudentId
            );
        } catch (DataAccessException ex) {
            // Например, дублирующийся login (UNIQUE constraint)
            throw new RuntimeException("Пользователь с таким логином уже существует.");
        }
    }

    public void deleteUser(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }
}
