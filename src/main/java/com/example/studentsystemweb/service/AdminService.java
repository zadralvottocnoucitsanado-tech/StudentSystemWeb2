package com.example.studentsystemweb.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.studentsystemweb.model.Grade;
import com.example.studentsystemweb.model.Student;
import com.example.studentsystemweb.repository.GradeRepository;
import com.example.studentsystemweb.repository.StudentRepository;
import com.example.studentsystemweb.repository.UserRepository;

@Service
public class AdminService {

    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public AdminService(StudentRepository studentRepository,
                        GradeRepository gradeRepository,
                        UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------
    // Управление студентами
    // ------------------------------------------------------------

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public void addStudent(String fullname, String group) {
        studentRepository.insert(fullname, group);
    }

    public void deleteStudent(int id) {
        studentRepository.delete(id);
    }

    // ------------------------------------------------------------
    // Аналитика
    // ------------------------------------------------------------

    /**
     * Реальный средний балл конкретного студента.
     *
     * Реализация: берём все записи оценок студента, фильтруем только FINAL и
     * считаем среднее по этим значениям. Если FINAL нет — возвращаем 0.0.
     */
    private Double averageForStudent(int studentId) {
        List<Grade> grades = gradeRepository.findByStudent(studentId);
        if (grades == null || grades.isEmpty()) return 0.0;

        return grades.stream()
                .filter(Objects::nonNull)
                .filter(g -> "FINAL".equalsIgnoreCase(g.getWorkType()))
                .map(Grade::getGrade)
                .filter(Objects::nonNull)
                .filter(v -> v > 0) // игнорируем -1 / -2 и нулевые
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Средний балл всех студентов (общий).
     */
    public Double getGlobalAverage() {
        Double avg = gradeRepository.globalAverage();
        return avg != null ? avg : 0.0;
    }

    /**
     * Средний балл каждого студента (карта ID → AVG)
     */
    public Map<Integer, Double> averageByStudent() {
        Map<Integer, Double> result = new LinkedHashMap<>();

        for (Student s : studentRepository.findAll()) {
            result.put(s.getId(), averageForStudent(s.getId()));
        }

        return result;
    }

    /**
     * Топ студентов по среднему баллу
     */
    public List<Map<String, Object>> getTopStudents(int count) {

        List<Map<String, Object>> list = new ArrayList<>();

        for (Student s : studentRepository.findAll()) {
            Map<String, Object> row = new HashMap<>();
            row.put("student", s);
            row.put("avg", averageForStudent(s.getId()));
            list.add(row);
        }

        list.sort((a, b) ->
                Double.compare((Double) b.get("avg"), (Double) a.get("avg"))
        );

        return list.subList(0, Math.min(count, list.size()));
    }

    /**
     * Худшие студенты (низкий средний балл)
     */
    public List<Map<String, Object>> getWorstStudents(int count) {

        List<Map<String, Object>> list = new ArrayList<>();

        for (Student s : studentRepository.findAll()) {
            Map<String, Object> row = new HashMap<>();
            row.put("student", s);
            row.put("avg", averageForStudent(s.getId()));
            list.add(row);
        }

        list.sort(Comparator.comparing(a -> (Double) a.get("avg")));

        return list.subList(0, Math.min(count, list.size()));
    }

    /**
     * Средний балл групп
     */
    public Map<String, Double> groupAverages() {
        Map<String, List<Double>> groups = new LinkedHashMap<>();

        for (Student s : studentRepository.findAll()) {
            double avg = averageForStudent(s.getId());
            groups.putIfAbsent(s.getGroupName(), new ArrayList<>());
            groups.get(s.getGroupName()).add(avg);
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (var entry : groups.entrySet()) {
            double avg = entry.getValue()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            result.put(entry.getKey(), avg);
        }

        return result;
    }

    // ------------------------------------------------------------
    // Создание пользователя
    // ------------------------------------------------------------

    public void createUser(String login,
                           String password,
                           String role,
                           Integer linkedStudentId) {

        if (login == null || login.isBlank())
            throw new RuntimeException("Логин не может быть пустым");

        if (password == null || password.isBlank())
            throw new RuntimeException("Пароль не может быть пустым");

        if (role == null || role.isBlank())
            throw new RuntimeException("Роль не указана");

        if (role.equals("STUDENT") && linkedStudentId == null)
            throw new RuntimeException("Для STUDENT необходимо указать ID студента");

        userRepository.createUser(login, password, role, linkedStudentId);
    }
}
