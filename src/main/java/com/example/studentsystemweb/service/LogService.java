package com.example.studentsystemweb.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogService {

    private final List<String> logs = new ArrayList<>();

    public void add(String msg) {
        logs.add(LocalDateTime.now() + " — " + msg);
    }

    public List<String> getAll() {
        return logs;
    }
}
