package com.example.studentsystemweb.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    // Мини-расписание просто для вида, без БД
    private final Map<Integer, List<String>> schedules = Map.of(
            1, List.of("Понедельник: Математика", "Вторник: Физика"),
            2, List.of("Понедельник: Программирование", "Среда: Английский")
    );

    // Если расписания нет — возвращаем пустой список
    public List<String> getScheduleForStudent(int studentId) {
        return schedules.getOrDefault(studentId, List.of());
    }
}
