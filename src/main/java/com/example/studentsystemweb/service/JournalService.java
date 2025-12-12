package com.example.studentsystemweb.service;

import com.example.studentsystemweb.model.Attendance;
import com.example.studentsystemweb.model.Grade;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class JournalService {

    private final StudentService studentService;

    public JournalService(StudentService studentService) {
        this.studentService = studentService;
    }

    // ------------------------------------------
    // Количество уникальных предметов
    // ------------------------------------------
    public int countSubjectsForStudent(int studentId) {
        List<Grade> grades = studentService.getGrades(studentId);

        Set<String> subjects = new HashSet<>();
        for (Grade g : grades) {
            if (g.getSubject() != null && !g.getSubject().isEmpty()) {
                subjects.add(g.getSubject());
            }
        }

        return subjects.size();
    }

    // ------------------------------------------
    // Количество пропусков (status != PRESENT)
    // ------------------------------------------
    public int countAbsences(int studentId) {
        List<Attendance> att = studentService.getAttendance(studentId);

        int counter = 0;
        for (Attendance a : att) {
            if (a.getStatus() == null) continue;

            String status = a.getStatus().toUpperCase();

            // считаем пропуском всё, что не PRESENT
            if (!status.equals("PRESENT")) {
                counter++;
            }
        }
        return counter;
    }

    // ------------------------------------------
    // Средний балл (нормальная версия)
    // ------------------------------------------
    public double computeAverage(int studentId) {
        List<Grade> grades = studentService.getGrades(studentId);
        if (grades.isEmpty()) return 0;

        double sum = 0;
        int count = 0;

        for (Grade g : grades) {
            sum += g.getGrade();
            count++;
        }

        return count == 0 ? 0 : sum / count;
    }
}
